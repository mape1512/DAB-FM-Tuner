package com.example.data.model

data class RadioStation(
    val id: String,
    val name: String,
    val streamUrl: String,
    val faviconUrl: String,
    val band: RadioBand,
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
    val displayTechSpec: String
        get() = buildString {
            if (codec.isNotBlank()) append(codec.uppercase())
            if (bitrate > 0) {
                if (isNotEmpty()) append(" · ")
                append("$bitrate kbps")
            }
            if (isEmpty()) append("LIVE STREAM")
        }
}
