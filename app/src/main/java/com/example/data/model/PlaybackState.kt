package com.example.data.model

import android.graphics.Bitmap

sealed interface StreamStatus {
    data object Idle : StreamStatus
    data object Buffering : StreamStatus
    data object Playing : StreamStatus
    data object Paused : StreamStatus
    data class Error(val message: String) : StreamStatus
}

data class SignalReceptionInfo(
    val bars: Int = 0,
    val transportLabel: String = "Offline",
    val qualityLabel: String = "Kein Netz",
    val bufferSeconds: Float = 0f,
    val liveBitrateKbps: Int = 0,
    val signalDbm: Int? = null,
    val isConnected: Boolean = false,
    val isAntennaActive: Boolean = false,
    val isUsbDabDetected: Boolean = false,
    val usbNeedsPermission: Boolean = false,
    val antennaStatusText: String = "",
    val imageSourceLabel: String = ""
)

data class NowPlayingState(
    val station: RadioStation? = null,
    val status: StreamStatus = StreamStatus.Idle,
    val rdsTitle: String = "",
    val rdsArtist: String = "",
    val antennaArtwork: Bitmap? = null,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val reception: SignalReceptionInfo = SignalReceptionInfo()
) {
    val formattedRds: String
        get() = when {
            rdsArtist.isNotBlank() && rdsTitle.isNotBlank() -> "$rdsArtist – $rdsTitle"
            rdsTitle.isNotBlank() -> rdsTitle
            rdsArtist.isNotBlank() -> rdsArtist
            else -> ""
        }
}
