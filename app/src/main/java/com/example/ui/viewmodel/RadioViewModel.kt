package com.example.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.R
import com.example.data.ai.ImageRecognitionState
import com.example.data.ai.StationImageRecognizer
import com.example.data.local.RadioDatabase
import com.example.data.local.RadioPreferences
import com.example.data.model.CountryCatalog
import com.example.data.model.CountryOption
import com.example.data.model.NowPlayingState
import com.example.data.model.RadioBand
import com.example.data.model.RadioStation
import com.example.data.model.SignalReceptionInfo
import com.example.data.model.StreamStatus
import com.example.data.network.NetworkSignalMonitor
import com.example.data.network.NetworkSignalSnapshot
import com.example.data.network.RadioNetworkClient
import com.example.data.radio.CarRadioHardwareMonitor
import com.example.data.radio.CarRadioHardwareState
import com.example.data.radio.HardwareTunerAvailability
import com.example.data.repository.RadioRepository
import com.example.service.RadioPlaybackService
import androidx.media3.common.C
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "RadioViewModel"

class RadioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RadioRepository(
        stationDao = RadioDatabase.getInstance(application).stationDao(),
        preferences = RadioPreferences(application)
    )
    private val networkSignalMonitor = NetworkSignalMonitor(application)
    private val carRadioHardwareMonitor = CarRadioHardwareMonitor(application)
    private val imageRecognizer by lazy { StationImageRecognizer() }

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var recognitionJob: Job? = null
    private var liveProbeJob: Job? = null
    private var logoCatalogSyncJob: Job? = null
    private var staleMetadataSignature: String? = null
    private var lastSyncedLanWifiTransport: String? = null

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _selectedGenre.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _availableCountries = MutableStateFlow(CountryCatalog.defaultCountries)
    val availableCountries: StateFlow<List<CountryOption>> = _availableCountries.asStateFlow()

    private val _nowPlaying = MutableStateFlow(NowPlayingState())
    val nowPlaying: StateFlow<NowPlayingState> = _nowPlaying.asStateFlow()

    private val _imageRecognitionState = MutableStateFlow<ImageRecognitionState>(ImageRecognitionState.Idle)
    val imageRecognitionState: StateFlow<ImageRecognitionState> = _imageRecognitionState.asStateFlow()

    val carHardwareState: StateFlow<CarRadioHardwareState> = carRadioHardwareMonitor.state

    val isDarkCockpit: StateFlow<Boolean> = repository.isDarkCockpit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val selectedCountry: StateFlow<String> = repository.selectedCountry
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DE")

    val activeCountry: StateFlow<CountryOption> = combine(
        selectedCountry,
        availableCountries
    ) { code, catalog ->
        CountryCatalog.findOption(code, catalog)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CountryCatalog.findOption("DE"))

    val selectedBand: StateFlow<RadioBand> = repository.selectedBand
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RadioBand.ALL)

    val isDrivingMode: StateFlow<Boolean> = repository.isDrivingMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val allStations: StateFlow<List<RadioStation>> = repository.allStations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteStations: StateFlow<List<RadioStation>> = repository.favoriteStations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val presetStations: StateFlow<List<RadioStation>> = repository.presetStations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<RadioStation>> = repository.recentlyPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredStations: StateFlow<List<RadioStation>> = combine(
        allStations,
        selectedBand,
        selectedCountry,
        _searchQuery,
        _selectedGenre
    ) { stations, band, country, query, genre ->
        stations.filter { station ->
            val matchesBand = band == RadioBand.ALL || station.band == band
            val matchesCountry = station.countryCode.equals(country, ignoreCase = true) ||
                station.isCustom
            val trimmedQuery = query.trim().lowercase()
            val matchesQuery = trimmedQuery.isBlank() ||
                station.name.lowercase().contains(trimmedQuery) ||
                station.frequencyOrChannel.lowercase().contains(trimmedQuery) ||
                station.genreTags.lowercase().contains(trimmedQuery)
            val matchesGenre = genre.isNullOrBlank() ||
                station.genreTags.lowercase().contains(genre.lowercase())
            matchesBand && matchesCountry && matchesQuery && matchesGenre
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dabStations: StateFlow<List<RadioStation>> = combine(
        allStations,
        selectedCountry
    ) { stations, country ->
        stations.filter { station ->
            station.band == RadioBand.DAB_PLUS &&
                (station.countryCode.equals(country, ignoreCase = true) || station.isCustom)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fmStations: StateFlow<List<RadioStation>> = combine(
        allStations,
        selectedCountry
    ) { stations, country ->
        stations.filter { station ->
            station.band == RadioBand.FM &&
                (station.countryCode.equals(country, ignoreCase = true) || station.isCustom)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            syncPlayerState()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            syncPlayerState()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.mediaId?.let { id ->
                val current = _nowPlaying.value
                if (current.station?.id != id && current.rdsTitle.isNotBlank()) {
                    staleMetadataSignature = "${current.rdsArtist}|${current.rdsTitle}"
                }
                val immediateMatch = allStations.value.firstOrNull { it.id == id }
                if (immediateMatch != null) {
                    _nowPlaying.update {
                        it.copy(
                            station = immediateMatch,
                            rdsTitle = "",
                            rdsArtist = "",
                            antennaArtwork = null
                        )
                    }
                    probeLiveMetadataAndArtwork(immediateMatch)
                }
                viewModelScope.launch {
                    val matched = immediateMatch ?: repository.getStationById(id)
                    if (matched != null) {
                        if (immediateMatch == null) {
                            _nowPlaying.update {
                                it.copy(
                                    station = matched,
                                    rdsTitle = "",
                                    rdsArtist = "",
                                    antennaArtwork = null
                                )
                            }
                            probeLiveMetadataAndArtwork(matched)
                        }
                        repository.markStationPlayed(matched)
                    }
                }
            }
            syncPlayerState()
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            staleMetadataSignature = null
            applyStreamMetadata(mediaMetadata)
        }

        override fun onPlayerError(error: PlaybackException) {
            val errorMsg = getApplication<Application>().getString(R.string.status_error)
            _nowPlaying.update {
                it.copy(status = StreamStatus.Error(errorMsg))
            }
        }
    }

    init {
        viewModelScope.launch {
            repository.ensureStarterCatalog()
            val lastId = repository.lastStationId.first()
            val initialStations = repository.allStations.first()
            val initialStation = initialStations.firstOrNull { it.id == lastId }
                ?: initialStations.firstOrNull()
            if (initialStation != null && _nowPlaying.value.station == null) {
                _nowPlaying.update { it.copy(station = initialStation) }
            }
            val country = repository.selectedCountry.first()
            syncStationsFromApi(country, showSuccessToast = false)
            repository.fetchAvailableCountries().onSuccess { countries ->
                if (countries.isNotEmpty()) {
                    _availableCountries.value = countries
                }
            }
        }
        viewModelScope.launch {
            combine(
                networkSignalMonitor.snapshot,
                carRadioHardwareMonitor.state
            ) { snapshot, hardwareState ->
                snapshot to hardwareState
            }.collect { (snapshot, hardwareState) ->
                updateReceptionState(snapshot, hardwareState)
                if (snapshot.isLanOrWifi && lastSyncedLanWifiTransport != snapshot.transportLabel) {
                    lastSyncedLanWifiTransport = snapshot.transportLabel
                    triggerCatalogLogoRefreshOverLanOrWlan(showToast = false)
                } else if (!snapshot.isLanOrWifi) {
                    lastSyncedLanWifiTransport = null
                }
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                updateReceptionState(
                    networkSignalMonitor.snapshot.value,
                    carRadioHardwareMonitor.state.value
                )
            }
        }
        connectMediaController()
    }

    private fun connectMediaController() {
        val app = getApplication<Application>()
        val token = SessionToken(app, ComponentName(app, RadioPlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching {
                    val controller = future.get()
                    mediaController = controller
                    controller.addListener(playerListener)
                    syncPlayerState()
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    private fun syncPlayerState() {
        val controller = mediaController ?: return
        val currentId = controller.currentMediaItem?.mediaId
        val matchedStation = currentId?.let { id ->
            allStations.value.firstOrNull { it.id == id }
        } ?: _nowPlaying.value.station

        val newStatus = when {
            controller.playerError != null -> {
                StreamStatus.Error(getApplication<Application>().getString(R.string.status_error))
            }
            controller.playbackState == Player.STATE_BUFFERING -> StreamStatus.Buffering
            controller.isPlaying -> StreamStatus.Playing
            controller.playbackState == Player.STATE_READY && !controller.playWhenReady -> StreamStatus.Paused
            controller.playbackState == Player.STATE_IDLE && matchedStation != null -> StreamStatus.Idle
            else -> StreamStatus.Idle
        }

        _nowPlaying.update {
            it.copy(
                station = matchedStation,
                status = newStatus
            )
        }
        applyStreamMetadata(controller.mediaMetadata)
        updateReceptionState(networkSignalMonitor.snapshot.value, carRadioHardwareMonitor.state.value)
    }

    private fun updateReceptionState(
        snapshot: NetworkSignalSnapshot,
        hardwareState: CarRadioHardwareState = carRadioHardwareMonitor.state.value
    ) {
        val controller = mediaController
        val currentState = _nowPlaying.value
        val station = currentState.station

        val isUsbDabDetected = hardwareState.availability == HardwareTunerAvailability.USB_DAB_READY ||
            hardwareState.availability == HardwareTunerAvailability.USB_DAB_CONNECTED_NEEDS_PERMISSION
        val usbNeedsPermission = hardwareState.availability == HardwareTunerAvailability.USB_DAB_CONNECTED_NEEDS_PERMISSION

        if (hardwareState.isHardwarePlaying) {
            val hardwareBars = hardwareState.antennaBars.coerceAtLeast(1)
            val app = getApplication<Application>()
            val qualityLabel = when (hardwareBars) {
                1 -> app.getString(R.string.signal_weak)
                2 -> app.getString(R.string.signal_fair)
                3 -> app.getString(R.string.signal_good)
                4 -> app.getString(R.string.signal_very_good)
                else -> app.getString(R.string.signal_optimal)
            }
            val hardwareTransport = if (hardwareState.activeHardwareBand == RadioBand.DAB_PLUS) {
                "DAB+-Antenne"
            } else {
                "UKW-Antenne"
            }
            val hwTitle = hardwareState.hardwareSongTitle
            val hwArtist = hardwareState.hardwareSongArtist
            val (parsedHwArtist, parsedHwTitle) = if (hwTitle.isNotBlank()) {
                parseIcyMetadata(hwArtist, hwTitle)
            } else {
                "" to ""
            }
            _nowPlaying.update {
                it.copy(
                    rdsTitle = parsedHwTitle.ifBlank { it.rdsTitle },
                    rdsArtist = if (parsedHwTitle.isNotBlank()) parsedHwArtist else it.rdsArtist,
                    antennaArtwork = hardwareState.hardwareStationArtwork ?: it.antennaArtwork,
                    reception = SignalReceptionInfo(
                        bars = hardwareBars,
                        transportLabel = hardwareTransport,
                        qualityLabel = qualityLabel,
                        bufferSeconds = 0f,
                        liveBitrateKbps = 0,
                        signalDbm = null,
                        isConnected = true,
                        isAntennaActive = true,
                        isUsbDabDetected = isUsbDabDetected,
                        usbNeedsPermission = usbNeedsPermission,
                        antennaStatusText = hardwareState.statusDetailDe,
                        imageSourceLabel = "Antenne (${hardwareTransport})"
                    )
                )
            }
            return
        }

        val bufferAheadMs = if (controller != null &&
            (currentState.status == StreamStatus.Playing || currentState.status == StreamStatus.Buffering)
        ) {
            (controller.bufferedPosition - controller.currentPosition).coerceAtLeast(0L)
        } else {
            0L
        }
        val bufferSeconds = bufferAheadMs / 1000f

        val trackBitrateKbps = controller?.currentTracks?.groups
            ?.asSequence()
            ?.filter { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }
            ?.mapNotNull { group ->
                if (group.length > 0) group.getTrackFormat(0).bitrate else null
            }
            ?.firstOrNull { it > 0 }
            ?.div(1000)
            ?: station?.bitrate
            ?: 0

        val effectiveBars = when {
            !snapshot.isConnected -> 0
            currentState.status is StreamStatus.Error -> 1
            currentState.status == StreamStatus.Buffering -> minOf(snapshot.networkBars, 2)
            currentState.status == StreamStatus.Playing && bufferSeconds < 0.8f ->
                minOf(snapshot.networkBars, 3)
            else -> snapshot.networkBars
        }

        val app = getApplication<Application>()
        val qualityLabel = when (effectiveBars) {
            0 -> app.getString(R.string.signal_offline)
            1 -> app.getString(R.string.signal_weak)
            2 -> app.getString(R.string.signal_fair)
            3 -> app.getString(R.string.signal_good)
            4 -> app.getString(R.string.signal_very_good)
            else -> app.getString(R.string.signal_optimal)
        }

        val imageSourceText = when {
            snapshot.isLanOrWifi -> "${snapshot.transportLabel} (Senderbilder aktiv)"
            snapshot.isConnected -> "Mobilfunk (Bilder über Antenne oder LAN/WLAN)"
            else -> "Offline"
        }

        _nowPlaying.update {
            it.copy(
                reception = SignalReceptionInfo(
                    bars = effectiveBars,
                    transportLabel = snapshot.transportLabel,
                    qualityLabel = qualityLabel,
                    bufferSeconds = bufferSeconds,
                    liveBitrateKbps = trackBitrateKbps,
                    signalDbm = snapshot.signalDbm,
                    isConnected = snapshot.isConnected,
                    isAntennaActive = false,
                    isUsbDabDetected = isUsbDabDetected,
                    usbNeedsPermission = usbNeedsPermission,
                    antennaStatusText = hardwareState.statusDetailDe,
                    imageSourceLabel = imageSourceText
                )
            )
        }
    }

    private fun applyStreamMetadata(mediaMetadata: MediaMetadata) {
        val station = _nowPlaying.value.station ?: return
        val controllerMediaId = mediaController?.currentMediaItem?.mediaId
        if (controllerMediaId != null && controllerMediaId != station.id) {
            return
        }

        val stationName = station.name
        val stationSubtitle = "${station.band.badgeText} · ${station.frequencyOrChannel}"

        val rawTitle = (mediaMetadata.title ?: mediaMetadata.displayTitle)
            ?.toString()
            ?.trim()
            .orEmpty()
        val candidateArtist = (mediaMetadata.artist ?: mediaMetadata.albumArtist)
            ?.toString()
            ?.trim()
            .orEmpty()
        val rawArtist = if (
            candidateArtist.equals(stationSubtitle, ignoreCase = true) ||
            candidateArtist.equals(stationName, ignoreCase = true)
        ) {
            ""
        } else {
            candidateArtist
        }

        if (rawTitle.isNotBlank() &&
            !rawTitle.equals(stationName, ignoreCase = true) &&
            !rawTitle.startsWith("http://", ignoreCase = true) &&
            !rawTitle.startsWith("https://", ignoreCase = true)
        ) {
            val (parsedArtist, parsedTitle) = parseIcyMetadata(rawArtist, rawTitle)
            val sig = "$parsedArtist|$parsedTitle"
            if (staleMetadataSignature != null && staleMetadataSignature == sig) {
                return
            }
            staleMetadataSignature = null
            _nowPlaying.update {
                it.copy(rdsArtist = parsedArtist, rdsTitle = parsedTitle)
            }
        }
    }

    private fun parseIcyMetadata(artist: String, title: String): Pair<String, String> {
        val cleanedTitle = title
            .replace(Regex("""^(Jetzt läuft|Now Playing|On Air)\s*:\s*""", RegexOption.IGNORE_CASE), "")
            .trim()
        if (artist.isNotBlank()) return artist.trim() to cleanedTitle

        val separators = listOf(" - ", " – ", " — ", " / ")
        for (sep in separators) {
            val idx = cleanedTitle.indexOf(sep)
            if (idx in 1 until (cleanedTitle.length - sep.length)) {
                val parsedArtist = cleanedTitle.substring(0, idx).trim()
                val parsedTitle = cleanedTitle.substring(idx + sep.length).trim()
                if (parsedArtist.isNotBlank() && parsedTitle.isNotBlank()) {
                    return parsedArtist to parsedTitle
                }
            }
        }
        return "" to cleanedTitle
    }

    private fun probeLiveMetadataAndArtwork(station: RadioStation) {
        liveProbeJob?.cancel()
        liveProbeJob = viewModelScope.launch {
            coroutineScope {
                launch {
                    val icyResult = RadioNetworkClient.fetchImmediateIcyMetadata(station.streamUrl)
                    val current = _nowPlaying.value
                    if (current.station?.id == station.id && icyResult != null) {
                        val rawTitle = icyResult.streamTitle.trim()
                        if (rawTitle.isNotBlank() &&
                            !rawTitle.equals(station.name, ignoreCase = true) &&
                            !rawTitle.startsWith("http://", ignoreCase = true) &&
                            !rawTitle.startsWith("https://", ignoreCase = true)
                        ) {
                            val (parsedArtist, parsedTitle) = parseIcyMetadata("", rawTitle)
                            staleMetadataSignature = null
                            _nowPlaying.update { state ->
                                if (state.station?.id == station.id) {
                                    state.copy(rdsArtist = parsedArtist, rdsTitle = parsedTitle)
                                } else {
                                    state
                                }
                            }
                        }
                    }
                }
                launch {
                    val hwPlaying = carRadioHardwareMonitor.state.value.isHardwarePlaying
                    val isLanOrWifi = networkSignalMonitor.snapshot.value.isLanOrWifi
                    if (!hwPlaying && isLanOrWifi) {
                        val freshLogo = repository.refreshStationLogoOverNetwork(station)
                        if (!freshLogo.isNullOrBlank() && freshLogo != station.faviconUrl) {
                            _nowPlaying.update { state ->
                                if (state.station?.id == station.id) {
                                    state.copy(station = state.station.copy(faviconUrl = freshLogo))
                                } else {
                                    state
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun tuneStation(
        station: RadioStation,
        queueSource: List<RadioStation> = filteredStations.value
    ) {
        val previous = _nowPlaying.value
        if (previous.station?.id != station.id && previous.rdsTitle.isNotBlank()) {
            staleMetadataSignature = "${previous.rdsArtist}|${previous.rdsTitle}"
        }

        if (carRadioHardwareMonitor.tryTuneViaAntenna(station)) {
            liveProbeJob?.cancel()
            mediaController?.pause()
            val hwState = carRadioHardwareMonitor.state.value
            val (parsedArtist, parsedTitle) = if (hwState.hardwareSongTitle.isNotBlank()) {
                parseIcyMetadata(hwState.hardwareSongArtist, hwState.hardwareSongTitle)
            } else {
                "" to ""
            }
            _nowPlaying.update {
                it.copy(
                    station = station,
                    status = StreamStatus.Playing,
                    rdsTitle = parsedTitle,
                    rdsArtist = parsedArtist,
                    antennaArtwork = hwState.hardwareStationArtwork
                )
            }
            viewModelScope.launch {
                repository.markStationPlayed(station)
            }
            updateReceptionState(networkSignalMonitor.snapshot.value, hwState)
            return
        }

        carRadioHardwareMonitor.stopHardwarePlayback()

        _nowPlaying.update {
            it.copy(
                station = station,
                status = StreamStatus.Buffering,
                rdsTitle = "",
                rdsArtist = "",
                antennaArtwork = null
            )
        }
        probeLiveMetadataAndArtwork(station)

        val queue = (queueSource.ifEmpty { allStations.value })
            .distinctBy { it.id }
            .let { list -> if (list.any { it.id == station.id }) list else listOf(station) + list }

        val startIndex = queue.indexOfFirst { it.id == station.id }.coerceAtLeast(0)
        val mediaItems = queue.map { item ->
            val metadata = MediaMetadata.Builder()
                .setTitle(item.name)
                .setSubtitle("${item.band.badgeText} · ${item.frequencyOrChannel}")
                .setStation(item.name)
                .apply {
                    if (item.faviconUrl.isNotBlank()) {
                        setArtworkUri(Uri.parse(item.faviconUrl))
                    }
                }
                .build()

            MediaItem.Builder()
                .setMediaId(item.id)
                .setUri(item.streamUrl)
                .setMediaMetadata(metadata)
                .build()
        }

        val controller = mediaController
        if (controller != null) {
            controller.setMediaItems(mediaItems, startIndex, 0L)
            controller.prepare()
            controller.play()
        } else {
            connectMediaController()
        }

        viewModelScope.launch {
            repository.markStationPlayed(station)
        }
    }

    fun togglePlayPause() {
        if (carRadioHardwareMonitor.state.value.isHardwarePlaying) {
            carRadioHardwareMonitor.stopHardwarePlayback()
            _nowPlaying.update { it.copy(status = StreamStatus.Paused) }
            updateReceptionState(networkSignalMonitor.snapshot.value, carRadioHardwareMonitor.state.value)
            return
        }
        val controller = mediaController
        val currentStation = _nowPlaying.value.station

        if (controller == null) {
            if (currentStation != null) tuneStation(currentStation)
            return
        }

        if (controller.mediaItemCount == 0 && currentStation != null) {
            tuneStation(currentStation)
            return
        }

        if (controller.isPlaying || _nowPlaying.value.status == StreamStatus.Buffering) {
            controller.pause()
        } else {
            if (controller.playerError != null && currentStation != null) {
                tuneStation(currentStation)
            } else {
                controller.prepare()
                controller.play()
            }
        }
    }

    fun skipNext() {
        val controller = mediaController
        if (controller != null && controller.mediaItemCount > 1) {
            controller.seekToNextMediaItem()
            controller.prepare()
            controller.play()
        } else {
            val list = filteredStations.value.ifEmpty { allStations.value }
            if (list.isNotEmpty()) {
                val currentIdx = list.indexOfFirst { it.id == _nowPlaying.value.station?.id }
                val nextStation = list[(currentIdx + 1) % list.size]
                tuneStation(nextStation, list)
            }
        }
    }

    fun skipPrevious() {
        val controller = mediaController
        if (controller != null && controller.mediaItemCount > 1) {
            controller.seekToPreviousMediaItem()
            controller.prepare()
            controller.play()
        } else {
            val list = filteredStations.value.ifEmpty { allStations.value }
            if (list.isNotEmpty()) {
                val currentIdx = list.indexOfFirst { it.id == _nowPlaying.value.station?.id }
                val prevIdx = if (currentIdx <= 0) list.lastIndex else currentIdx - 1
                tuneStation(list[prevIdx], list)
            }
        }
    }

    fun retryCurrentStream() {
        _nowPlaying.value.station?.let { tuneStation(it) }
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        mediaController?.volume = clamped
        _nowPlaying.update { it.copy(volume = clamped, isMuted = clamped == 0f) }
    }

    fun toggleMute() {
        val current = _nowPlaying.value
        val newMuted = !current.isMuted
        val targetVolume = if (newMuted) 0f else if (current.volume > 0f) current.volume else 0.8f
        mediaController?.volume = targetVolume
        _nowPlaying.update {
            it.copy(
                isMuted = newMuted,
                volume = if (!newMuted && it.volume == 0f) 0.8f else it.volume
            )
        }
    }

    fun toggleFavorite(station: RadioStation) {
        viewModelScope.launch {
            repository.toggleFavorite(station)
            if (_nowPlaying.value.station?.id == station.id) {
                _nowPlaying.update {
                    it.copy(station = station.copy(isFavorite = !station.isFavorite))
                }
            }
        }
    }

    fun assignCurrentToPreset(slot: Int) {
        val station = _nowPlaying.value.station ?: return
        assignStationToPreset(station, slot)
    }

    fun assignStationToPreset(station: RadioStation, slot: Int) {
        viewModelScope.launch {
            repository.assignPreset(station.id, slot)
            _snackbarMessage.value = getApplication<Application>()
                .getString(R.string.preset_assigned, slot)
        }
    }

    fun clearPreset(slot: Int) {
        viewModelScope.launch {
            repository.clearPreset(slot)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectGenre(genre: String?) {
        _selectedGenre.value = if (_selectedGenre.value == genre) null else genre
    }

    fun selectBand(band: RadioBand) {
        viewModelScope.launch {
            repository.setSelectedBand(band)
        }
    }

    fun selectCountry(countryCode: String) {
        viewModelScope.launch {
            repository.setSelectedCountry(countryCode)
            syncStationsFromApi(countryCode, showSuccessToast = true)
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            repository.setDarkCockpit(!isDarkCockpit.value)
        }
    }

    fun toggleDrivingMode() {
        viewModelScope.launch {
            repository.setDrivingMode(!isDrivingMode.value)
        }
    }

    fun syncStationsFromApi(
        countryCode: String = selectedCountry.value,
        showSuccessToast: Boolean = true
    ) {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            val query = _searchQuery.value.takeIf { it.isNotBlank() }
            val result = repository.refreshFromDirectory(countryCode, query)
            _isSyncing.value = false
            result.onSuccess { count ->
                if (showSuccessToast && count > 0) {
                    _snackbarMessage.value = getApplication<Application>()
                        .getString(R.string.network_sync_success, count)
                }
            }.onFailure {
                if (showSuccessToast) {
                    _snackbarMessage.value = getApplication<Application>()
                        .getString(R.string.network_sync_offline)
                }
            }
        }
    }

    fun addCustomStation(
        name: String,
        streamUrl: String,
        band: RadioBand,
        frequencyOrChannel: String
    ) {
        viewModelScope.launch {
            val station = repository.addCustomStation(name, streamUrl, band, frequencyOrChannel)
            tuneStation(station)
        }
    }

    fun deleteCustomStation(stationId: String) {
        viewModelScope.launch {
            repository.deleteCustomStation(stationId)
        }
    }

    fun recognizeStationFromUri(uri: Uri) {
        recognitionJob?.cancel()
        recognitionJob = viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    StationImageRecognizer.decodeAndScaleBitmap(getApplication(), uri)
                }.getOrNull()
            }
            if (bitmap == null) {
                _imageRecognitionState.value = ImageRecognitionState.Error(
                    previewBitmap = null,
                    message = "Das ausgewählte Bild konnte nicht geladen werden."
                )
                return@launch
            }
            runRecognitionPipeline(bitmap)
        }
    }

    fun recognizeStationFromBitmap(bitmap: Bitmap) {
        recognitionJob?.cancel()
        recognitionJob = viewModelScope.launch {
            val scaled = withContext(Dispatchers.IO) {
                StationImageRecognizer.scaleBitmapForAi(bitmap)
            }
            runRecognitionPipeline(scaled)
        }
    }

    private suspend fun runRecognitionPipeline(bitmap: Bitmap) {
        _imageRecognitionState.value = ImageRecognitionState.Analyzing(bitmap)
        try {
            val recognized = imageRecognizer.recognizeStation(bitmap)
            val matches = repository.findStationsForRecognition(
                recognized = recognized,
                activeCountryCode = selectedCountry.value
            )
            _imageRecognitionState.value = ImageRecognitionState.Result(
                previewBitmap = bitmap,
                recognized = recognized,
                matchedStations = matches
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Station image recognition timed out", e)
            _imageRecognitionState.value = ImageRecognitionState.Error(
                previewBitmap = bitmap,
                message = "Zeitüberschreitung bei der Bilderkennung (60 Sek.). Bitte erneut versuchen."
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Station image recognition failed", e)
            _imageRecognitionState.value = ImageRecognitionState.Error(
                previewBitmap = bitmap,
                message = e.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Die Bilderkennung konnte nicht abgeschlossen werden."
            )
        }
    }

    fun resetImageRecognition() {
        recognitionJob?.cancel()
        _imageRecognitionState.value = ImageRecognitionState.Idle
    }

    fun requestUsbDabPermission() {
        carRadioHardwareMonitor.requestUsbDabPermission()
    }

    fun refreshHardwareStatus() {
        carRadioHardwareMonitor.refreshHardwareStatus()
        refreshStationImages(showFeedback = false)
    }

    fun refreshStationImages(showFeedback: Boolean = true) {
        val hardwareState = carRadioHardwareMonitor.state.value
        val snapshot = networkSignalMonitor.snapshot.value
        if (hardwareState.isHardwarePlaying) {
            carRadioHardwareMonitor.refreshHardwareStatus()
            if (showFeedback) {
                _snackbarMessage.value = "Senderbilder werden über die DAB+/FM-Antenne aktualisiert."
            }
            return
        }
        if (snapshot.isLanOrWifi) {
            triggerCatalogLogoRefreshOverLanOrWlan(showToast = showFeedback)
        } else if (showFeedback) {
            _snackbarMessage.value = "Für neue Senderbilder bitte Antenne oder LAN/WLAN verbinden."
        }
    }

    private fun triggerCatalogLogoRefreshOverLanOrWlan(showToast: Boolean) {
        if (!networkSignalMonitor.snapshot.value.isLanOrWifi) return
        logoCatalogSyncJob?.cancel()
        logoCatalogSyncJob = viewModelScope.launch {
            val currentStation = _nowPlaying.value.station
            if (currentStation != null) {
                val freshUrl = repository.refreshStationLogoOverNetwork(currentStation)
                if (!freshUrl.isNullOrBlank() && freshUrl != currentStation.faviconUrl) {
                    _nowPlaying.update { state ->
                        if (state.station?.id == currentStation.id) {
                            state.copy(station = state.station.copy(faviconUrl = freshUrl))
                        } else {
                            state
                        }
                    }
                }
            }
            val updatedCount = repository.refreshCatalogLogosOverNetwork(selectedCountry.value)
            if (showToast) {
                val transport = networkSignalMonitor.snapshot.value.transportLabel
                _snackbarMessage.value = if (updatedCount > 0) {
                    "$updatedCount Senderbilder über $transport aktualisiert."
                } else {
                    "Senderbilder über $transport geprüft – aktuell."
                }
            }
        }
    }

    fun shutdownRadioPlayback() {
        liveProbeJob?.cancel()
        logoCatalogSyncJob?.cancel()
        carRadioHardwareMonitor.stopHardwarePlayback()
        mediaController?.run {
            playWhenReady = false
            pause()
            stop()
            clearMediaItems()
        }
        val app = getApplication<Application>()
        runCatching {
            app.stopService(Intent(app, RadioPlaybackService::class.java))
        }
        _nowPlaying.update {
            it.copy(
                status = StreamStatus.Idle,
                rdsTitle = "",
                rdsArtist = ""
            )
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    override fun onCleared() {
        recognitionJob?.cancel()
        shutdownRadioPlayback()
        networkSignalMonitor.release()
        carRadioHardwareMonitor.release()
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
