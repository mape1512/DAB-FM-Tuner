package com.example.data.repository

import com.example.data.ai.RecognizedStationInfo
import com.example.data.local.CuratedDachStations
import com.example.data.local.RadioPreferences
import com.example.data.local.StationDao
import com.example.data.local.StationEntity
import com.example.data.model.CountryCatalog
import com.example.data.model.CountryOption
import com.example.data.model.RadioBand
import com.example.data.model.RadioStation
import com.example.data.network.RadioBrowserApi
import com.example.data.network.RadioBrowserDto
import com.example.data.network.RadioNetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class RadioRepository(
    private val stationDao: StationDao,
    private val preferences: RadioPreferences,
    private val primaryApi: RadioBrowserApi = RadioNetworkClient.primaryApi,
    private val fallbackApi: RadioBrowserApi = RadioNetworkClient.fallbackApi
) {

    val allStations: Flow<List<RadioStation>> =
        stationDao.observeAllStations().map { list -> list.map { it.toDomain() } }

    val favoriteStations: Flow<List<RadioStation>> =
        stationDao.observeFavorites().map { list -> list.map { it.toDomain() } }

    val presetStations: Flow<List<RadioStation>> =
        stationDao.observePresets().map { list -> list.map { it.toDomain() } }

    val recentlyPlayed: Flow<List<RadioStation>> =
        stationDao.observeRecentlyPlayed().map { list -> list.map { it.toDomain() } }

    val isDarkCockpit: Flow<Boolean> = preferences.isDarkCockpit
    val selectedCountry: Flow<String> = preferences.selectedCountry.map { CountryCatalog.normalizeCode(it) }
    val selectedBand: Flow<RadioBand> = preferences.selectedBand
    val lastStationId: Flow<String?> = preferences.lastStationId
    val isDrivingMode: Flow<Boolean> = preferences.isDrivingMode

    suspend fun ensureStarterCatalog() = withContext(Dispatchers.IO) {
        if (stationDao.getAllStationsOnce().isEmpty()) {
            stationDao.upsertStations(CuratedDachStations.starterStations)
        }
    }

    suspend fun fetchAvailableCountries(): Result<List<CountryOption>> = withContext(Dispatchers.IO) {
        runCatching {
            val remote = fetchWithFallback { api -> api.getCountries() }
            val countsByCode = remote
                .filter { it.isoCode.trim().length == 2 && it.stationCount > 0 }
                .associate { dto ->
                    val code = dto.isoCode.trim().uppercase()
                    code to CountryOption(
                        code = code,
                        nameDe = CountryCatalog.displayNameFor(code, dto.name.trim()),
                        stationCount = dto.stationCount
                    )
                }

            val pinnedOrder = CountryCatalog.defaultCountries.mapIndexed { idx, opt -> opt.code to idx }.toMap()
            val merged = (CountryCatalog.defaultCountries.map { base ->
                countsByCode[base.code] ?: base
            } + countsByCode.values.filter { it.code !in pinnedOrder })
                .distinctBy { it.code }
                .sortedWith(
                    compareBy<CountryOption> { pinnedOrder[it.code] ?: Int.MAX_VALUE }
                        .thenByDescending { it.stationCount }
                        .thenBy { it.nameDe }
                )
            merged
        }
    }

    suspend fun refreshFromDirectory(countryCode: String, query: String? = null): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cleanCountry = CountryCatalog.normalizeCode(countryCode)
                val cleanQuery = query?.trim()?.takeIf { it.isNotBlank() }

                val dtos = fetchWithFallback { api ->
                    coroutineScope {
                        val generalDeferred = async {
                            api.searchStations(
                                countryCode = cleanCountry,
                                name = cleanQuery,
                                limit = 80
                            )
                        }
                        val dabDeferred = async {
                            api.searchStations(
                                countryCode = cleanCountry,
                                tag = "dab+",
                                name = cleanQuery,
                                limit = 50
                            )
                        }
                        val dabPlainDeferred = async {
                            api.searchStations(
                                countryCode = cleanCountry,
                                tag = "dab",
                                name = cleanQuery,
                                limit = 40
                            )
                        }
                        val fmDeferred = async {
                            api.searchStations(
                                countryCode = cleanCountry,
                                tag = "fm",
                                name = cleanQuery,
                                limit = 50
                            )
                        }
                        (dabDeferred.await() + dabPlainDeferred.await() + fmDeferred.await() + generalDeferred.await())
                            .distinctBy { it.stationUuid }
                    }
                }

                val existingById = stationDao.getAllStationsOnce().associateBy { it.id }
                val mapped = dtos.mapNotNull { dto ->
                    val stream = dto.urlResolved.ifBlank { dto.url }.trim()
                    val cleanName = dto.name.trim()
                    if (stream.isBlank() || cleanName.isBlank()) return@mapNotNull null

                    val existing = existingById[dto.stationUuid]
                    val classification = classifyStation(dto)

                    StationEntity(
                        id = dto.stationUuid,
                        name = cleanName,
                        streamUrl = stream,
                        faviconUrl = dto.favicon.trim(),
                        band = classification.band.name,
                        frequencyOrChannel = classification.frequencyOrChannel,
                        countryCode = dto.countryCode.ifBlank { countryCode }.uppercase(),
                        genreTags = dto.tags.split(",")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .take(4)
                            .joinToString(", "),
                        codec = dto.codec.ifBlank { "STREAM" }.uppercase(),
                        bitrate = dto.bitrate,
                        votes = dto.votes,
                        isFavorite = existing?.isFavorite ?: false,
                        presetSlot = existing?.presetSlot,
                        isCustom = existing?.isCustom ?: false,
                        lastPlayedAt = existing?.lastPlayedAt ?: 0L
                    )
                }

                val dtoFaviconsByNormalizedName = dtos
                    .filter { it.favicon.isNotBlank() }
                    .associate { normalizeLogoLookupName(it.name) to it.favicon.trim() }

                val updatedCurated = existingById.values
                    .filter { it.id.startsWith("curated_") && it.countryCode.equals(cleanCountry, ignoreCase = true) }
                    .mapNotNull { curated ->
                        val normCurated = normalizeLogoLookupName(curated.name)
                        val matchedFavicon = dtoFaviconsByNormalizedName[normCurated]
                            ?: dtoFaviconsByNormalizedName.entries.firstOrNull { (key, _) ->
                                key.contains(normCurated) || normCurated.contains(key)
                            }?.value
                        if (!matchedFavicon.isNullOrBlank() && matchedFavicon != curated.faviconUrl) {
                            curated.copy(faviconUrl = matchedFavicon)
                        } else {
                            null
                        }
                    }

                val combinedUpsert = (mapped + updatedCurated).distinctBy { it.id }
                if (combinedUpsert.isNotEmpty()) {
                    stationDao.upsertStations(combinedUpsert)
                }
                mapped.size
            }
        }

    private suspend fun <T> fetchWithFallback(block: suspend (RadioBrowserApi) -> T): T {
        return try {
            block(primaryApi)
        } catch (_: Exception) {
            block(fallbackApi)
        }
    }

    private fun classifyStation(dto: RadioBrowserDto): CuratedDachStations.ChannelHint {
        val hint = CuratedDachStations.findChannelHint(dto.name)
        if (hint != null) return hint

        val lowerTags = dto.tags.lowercase()
        val lowerName = dto.name.lowercase()

        val freqRegex = Regex("""(\d{2,3}[.,]\d)\s*(mhz|fm|ukw)?""", RegexOption.IGNORE_CASE)
        val freqMatch = freqRegex.find(dto.name) ?: freqRegex.find(dto.tags)

        val dabChannelRegex = Regex("""\b(5[ABCD]|6[ABCD]|7[ABCD]|8[ABCD]|9[ABCD]|10[ABCD]|11[ABCD]|12[ABCD]|13[A-F])\b""")
        val dabMatch = dabChannelRegex.find(dto.name)

        return when {
            lowerTags.contains("dab+") || lowerTags.contains("dab") ||
                lowerName.contains("dab+") || lowerName.contains("dab") || dabMatch != null -> {
                val channel = dabMatch?.value?.let { "DAB+ $it" } ?: "DAB+ Digital"
                CuratedDachStations.ChannelHint(RadioBand.DAB_PLUS, channel)
            }
            lowerTags.contains("fm") || lowerTags.contains("ukw") || freqMatch != null -> {
                val freq = freqMatch?.groupValues?.getOrNull(1)?.replace(',', '.')
                    ?.let { "$it MHz" } ?: "FM / UKW"
                CuratedDachStations.ChannelHint(RadioBand.FM, freq)
            }
            else -> {
                val estimatedBand = if (dto.codec.equals("AAC", ignoreCase = true) ||
                    dto.codec.equals("AAC+", ignoreCase = true)
                ) {
                    RadioBand.DAB_PLUS
                } else {
                    RadioBand.FM
                }
                val label = if (estimatedBand == RadioBand.DAB_PLUS) "DAB+ Stream" else "FM Stream"
                CuratedDachStations.ChannelHint(estimatedBand, label)
            }
        }
    }

    suspend fun toggleFavorite(station: RadioStation) = withContext(Dispatchers.IO) {
        stationDao.setFavorite(station.id, !station.isFavorite)
    }

    suspend fun assignPreset(stationId: String, slot: Int) = withContext(Dispatchers.IO) {
        if (slot in 1..12) {
            stationDao.assignPreset(stationId, slot)
        }
    }

    suspend fun clearPreset(slot: Int) = withContext(Dispatchers.IO) {
        stationDao.clearPresetSlot(slot)
    }

    suspend fun addCustomStation(
        name: String,
        streamUrl: String,
        band: RadioBand,
        frequencyOrChannel: String
    ): RadioStation = withContext(Dispatchers.IO) {
        val cleanFreq = frequencyOrChannel.trim().ifBlank {
            if (band == RadioBand.DAB_PLUS) "DAB+ Custom" else "FM Custom"
        }
        val entity = StationEntity(
            id = "custom_${UUID.randomUUID()}",
            name = name.trim(),
            streamUrl = streamUrl.trim(),
            faviconUrl = "",
            band = band.name,
            frequencyOrChannel = cleanFreq,
            countryCode = "DE",
            genreTags = if (band == RadioBand.DAB_PLUS) "dab+, custom" else "fm, custom",
            codec = "STREAM",
            bitrate = 0,
            votes = 9999,
            isFavorite = true,
            isCustom = true
        )
        stationDao.upsertStation(entity)
        entity.toDomain()
    }

    suspend fun deleteCustomStation(stationId: String) = withContext(Dispatchers.IO) {
        stationDao.deleteCustomStation(stationId)
    }

    suspend fun markStationPlayed(station: RadioStation) = withContext(Dispatchers.IO) {
        stationDao.updateLastPlayed(station.id, System.currentTimeMillis())
        preferences.setLastStationId(station.id)
        if (!station.isCustom && !station.id.startsWith("curated_")) {
            runCatching { primaryApi.registerStationClick(station.id) }
        }
    }

    suspend fun findStationsForRecognition(
        recognized: RecognizedStationInfo,
        activeCountryCode: String
    ): List<RadioStation> = withContext(Dispatchers.IO) {
        val searchTerms = (listOf(recognized.stationName) + recognized.alternateNames)
            .map { it.trim() }
            .filter { it.length >= 2 }
            .distinct()

        val freqNumber = Regex("""(\d{2,3}[.,]\d)""").find(recognized.frequencyOrChannel)
            ?.groupValues?.getOrNull(1)?.replace(',', '.')

        val targetCountry = recognized.countryCode.takeIf { it.length == 2 }
            ?: activeCountryCode.takeIf { it.length == 2 }

        // 1. Live online lookup if we have station terms (best effort, never fails local lookup)
        if (searchTerms.isNotEmpty()) {
            val primaryTerm = searchTerms.first()
            val remoteDtos = runCatching {
                fetchWithFallback { api ->
                    val countryHits = if (targetCountry != null) {
                        api.searchStations(
                            countryCode = targetCountry,
                            name = primaryTerm,
                            limit = 15
                        )
                    } else emptyList()

                    if (countryHits.isNotEmpty()) {
                        countryHits
                    } else {
                        api.searchStations(
                            countryCode = null,
                            name = primaryTerm,
                            limit = 15
                        )
                    }
                }
            }.getOrDefault(emptyList())

            if (remoteDtos.isNotEmpty()) {
                val existingById = stationDao.getAllStationsOnce().associateBy { it.id }
                val mapped = remoteDtos.mapNotNull { dto ->
                    val stream = dto.urlResolved.ifBlank { dto.url }.trim()
                    val cleanName = dto.name.trim()
                    if (stream.isBlank() || cleanName.isBlank()) return@mapNotNull null
                    val existing = existingById[dto.stationUuid]
                    val classification = classifyStation(dto)
                    StationEntity(
                        id = dto.stationUuid,
                        name = cleanName,
                        streamUrl = stream,
                        faviconUrl = dto.favicon.trim(),
                        band = classification.band.name,
                        frequencyOrChannel = classification.frequencyOrChannel,
                        countryCode = dto.countryCode.ifBlank { targetCountry ?: "DE" }.uppercase(),
                        genreTags = dto.tags.split(",")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .take(4)
                            .joinToString(", "),
                        codec = dto.codec.ifBlank { "STREAM" }.uppercase(),
                        bitrate = dto.bitrate,
                        votes = dto.votes,
                        isFavorite = existing?.isFavorite ?: false,
                        presetSlot = existing?.presetSlot,
                        isCustom = existing?.isCustom ?: false,
                        lastPlayedAt = existing?.lastPlayedAt ?: 0L
                    )
                }
                if (mapped.isNotEmpty()) {
                    stationDao.upsertStations(mapped)
                }
            }
        }

        // 2. Score all cached/upserted stations against recognition result
        val allCached = stationDao.getAllStationsOnce().map { it.toDomain() }
        val normalizedTerms = searchTerms.map { it.lowercase().replace(Regex("[^a-z0-9äöüß]"), "") }
            .filter { it.isNotBlank() }

        allCached.mapNotNull { station ->
            val cleanStationName = station.name.lowercase().replace(Regex("[^a-z0-9äöüß]"), "")
            val lowerStationName = station.name.lowercase()
            val lowerFreq = station.frequencyOrChannel.lowercase().replace(',', '.')

            var score = 0
            searchTerms.forEachIndexed { idx, term ->
                val lowerTerm = term.lowercase()
                val normTerm = normalizedTerms.getOrNull(idx).orEmpty()
                when {
                    lowerStationName == lowerTerm || (normTerm.isNotBlank() && cleanStationName == normTerm) ->
                        score += if (idx == 0) 120 else 95
                    lowerStationName.startsWith(lowerTerm) || (normTerm.length >= 3 && cleanStationName.startsWith(normTerm)) ->
                        score += if (idx == 0) 85 else 65
                    lowerStationName.contains(lowerTerm) || (normTerm.length >= 3 && cleanStationName.contains(normTerm)) ->
                        score += if (idx == 0) 60 else 45
                    lowerTerm.length >= 4 && lowerTerm.contains(lowerStationName) && lowerStationName.length >= 3 ->
                        score += 40
                }
            }

            if (!freqNumber.isNullOrBlank() && lowerFreq.contains(freqNumber)) {
                score += 50
            }
            if (recognized.frequencyOrChannel.isNotBlank() &&
                lowerFreq.contains(recognized.frequencyOrChannel.lowercase())
            ) {
                score += 35
            }

            if (score <= 0) return@mapNotNull null

            if (targetCountry != null && station.countryCode.equals(targetCountry, ignoreCase = true)) {
                score += 20
            }
            if (recognized.bandHint != RadioBand.ALL && station.band == recognized.bandHint) {
                score += 15
            }
            if (station.isFavorite || station.presetSlot != null) {
                score += 8
            }

            station to score
        }
            .sortedWith(compareByDescending<Pair<RadioStation, Int>> { it.second }.thenByDescending { it.first.votes })
            .map { it.first }
            .distinctBy { it.id }
            .take(8)
    }

    suspend fun refreshStationLogoOverNetwork(station: RadioStation): String? =
        withContext(Dispatchers.IO) {
            val currentUrl = station.faviconUrl.trim()
            if (currentUrl.isNotBlank() && RadioNetworkClient.verifyImageUrlReachable(currentUrl)) {
                return@withContext currentUrl
            }

            val searchName = station.name
                .replace(Regex("""\(DAB\+\)|\(FM\)|\(UKW\)""", RegexOption.IGNORE_CASE), "")
                .trim()
            if (searchName.length < 2) return@withContext null

            val candidates = runCatching {
                fetchWithFallback { api ->
                    val countryHits = if (station.countryCode.length == 2) {
                        api.searchStations(
                            countryCode = station.countryCode,
                            name = searchName,
                            limit = 12
                        )
                    } else emptyList()
                    countryHits.ifEmpty {
                        api.searchStations(
                            countryCode = null,
                            name = searchName,
                            limit = 12
                        )
                    }
                }
            }.getOrDefault(emptyList())

            val normTarget = normalizeLogoLookupName(searchName)
            val orderedFavicons = candidates
                .mapNotNull { dto ->
                    val fav = dto.favicon.trim()
                    if (fav.isBlank()) null
                    else {
                        val normCandidate = normalizeLogoLookupName(dto.name)
                        val exactScore = when {
                            normCandidate == normTarget -> 3
                            normCandidate.contains(normTarget) || normTarget.contains(normCandidate) -> 2
                            else -> 1
                        }
                        fav to exactScore
                    }
                }
                .sortedByDescending { it.second }
                .map { it.first }
                .distinct()

            for (candidateUrl in orderedFavicons.take(5)) {
                if (RadioNetworkClient.verifyImageUrlReachable(candidateUrl)) {
                    stationDao.updateFaviconUrl(station.id, candidateUrl)
                    return@withContext candidateUrl
                }
            }
            null
        }

    suspend fun refreshCatalogLogosOverNetwork(countryCode: String): Int =
        withContext(Dispatchers.IO) {
            val cleanCountry = CountryCatalog.normalizeCode(countryCode)
            val stations = stationDao.getAllStationsOnce()
                .filter { it.countryCode.equals(cleanCountry, ignoreCase = true) }
            var updatedCount = 0
            for (entity in stations.take(20)) {
                val currentFav = entity.faviconUrl.trim()
                val needsRefresh = currentFav.isBlank() ||
                    entity.id.startsWith("curated_") && !RadioNetworkClient.verifyImageUrlReachable(currentFav)
                if (needsRefresh) {
                    val fresh = refreshStationLogoOverNetwork(entity.toDomain())
                    if (!fresh.isNullOrBlank() && fresh != currentFav) {
                        updatedCount++
                    }
                }
            }
            updatedCount
        }

    private fun normalizeLogoLookupName(name: String): String {
        return name
            .replace(Regex("""\(DAB\+\)|\(FM\)|\(UKW\)""", RegexOption.IGNORE_CASE), "")
            .lowercase()
            .replace(Regex("[^a-z0-9äöüß]"), "")
    }

    suspend fun getStationById(id: String): RadioStation? = withContext(Dispatchers.IO) {
        stationDao.getStationById(id)?.toDomain()
    }

    suspend fun setDarkCockpit(enabled: Boolean) = preferences.setDarkCockpit(enabled)
    suspend fun setSelectedCountry(countryCode: String) =
        preferences.setSelectedCountry(CountryCatalog.normalizeCode(countryCode))
    suspend fun setSelectedBand(band: RadioBand) = preferences.setSelectedBand(band)
    suspend fun setDrivingMode(enabled: Boolean) = preferences.setDrivingMode(enabled)
}
