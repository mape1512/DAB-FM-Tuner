package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.RadioBand
import com.example.data.model.RadioStation

@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val streamUrl: String,
    val faviconUrl: String,
    val band: String,
    val frequencyOrChannel: String,
    val countryCode: String,
    val genreTags: String,
    val codec: String,
    val bitrate: Int,
    val votes: Int,
    val isFavorite: Boolean = false,
    val presetSlot: Int? = null,
    val isCustom: Boolean = false,
    val lastPlayedAt: Long = 0L
) {
    fun toDomain(): RadioStation = RadioStation(
        id = id,
        name = name,
        streamUrl = streamUrl,
        faviconUrl = faviconUrl,
        band = RadioBand.fromStored(band),
        frequencyOrChannel = frequencyOrChannel,
        countryCode = countryCode,
        genreTags = genreTags,
        codec = codec,
        bitrate = bitrate,
        votes = votes,
        isFavorite = isFavorite,
        presetSlot = presetSlot,
        isCustom = isCustom,
        lastPlayedAt = lastPlayedAt
    )

    companion object {
        fun fromDomain(station: RadioStation): StationEntity = StationEntity(
            id = station.id,
            name = station.name,
            streamUrl = station.streamUrl,
            faviconUrl = station.faviconUrl,
            band = station.band.name,
            frequencyOrChannel = station.frequencyOrChannel,
            countryCode = station.countryCode,
            genreTags = station.genreTags,
            codec = station.codec,
            bitrate = station.bitrate,
            votes = station.votes,
            isFavorite = station.isFavorite,
            presetSlot = station.presetSlot,
            isCustom = station.isCustom,
            lastPlayedAt = station.lastPlayedAt
        )
    }
}
