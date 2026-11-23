package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RadioBrowserDto(
    @Json(name = "stationuuid") val stationUuid: String,
    @Json(name = "name") val name: String,
    @Json(name = "url") val url: String = "",
    @Json(name = "url_resolved") val urlResolved: String = "",
    @Json(name = "homepage") val homepage: String = "",
    @Json(name = "favicon") val favicon: String = "",
    @Json(name = "tags") val tags: String = "",
    @Json(name = "countrycode") val countryCode: String = "",
    @Json(name = "codec") val codec: String = "",
    @Json(name = "bitrate") val bitrate: Int = 0,
    @Json(name = "votes") val votes: Int = 0,
    @Json(name = "lastcheckok") val lastCheckOk: Int = 1
)

@JsonClass(generateAdapter = true)
data class RadioCountryDto(
    @Json(name = "name") val name: String = "",
    @Json(name = "iso_3166_1") val isoCode: String = "",
    @Json(name = "stationcount") val stationCount: Int = 0
)
