package com.example.data.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RadioBrowserApi {

    @GET("json/stations/search")
    suspend fun searchStations(
        @Query("countrycode") countryCode: String? = null,
        @Query("tag") tag: String? = null,
        @Query("name") name: String? = null,
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("order") order: String = "votes",
        @Query("reverse") reverse: Boolean = true,
        @Query("limit") limit: Int = 100
    ): List<RadioBrowserDto>

    @GET("json/stations/bytagexact/{tag}")
    suspend fun getStationsByExactTag(
        @Path("tag") tag: String,
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("order") order: String = "votes",
        @Query("reverse") reverse: Boolean = true,
        @Query("limit") limit: Int = 80
    ): List<RadioBrowserDto>

    @GET("json/countries")
    suspend fun getCountries(): List<RadioCountryDto>

    @GET("json/url/{stationuuid}")
    suspend fun registerStationClick(
        @Path("stationuuid") stationUuid: String
    )
}
