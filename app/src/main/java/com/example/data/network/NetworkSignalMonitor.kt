package com.example.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NetworkSignalSnapshot(
    val isConnected: Boolean = false,
    val transportLabel: String = "Offline",
    val isLanOrWifi: Boolean = false,
    val networkBars: Int = 0,
    val signalDbm: Int? = null,
    val downstreamKbps: Int = 0
)

class NetworkSignalMonitor(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _snapshot = MutableStateFlow(readCurrentSnapshot())
    val snapshot: StateFlow<NetworkSignalSnapshot> = _snapshot.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            refresh()
        }

        override fun onLost(network: Network) {
            refresh()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            _snapshot.value = snapshotFromCapabilities(networkCapabilities)
        }
    }

    init {
        runCatching {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        }
    }

    fun refresh() {
        _snapshot.value = readCurrentSnapshot()
    }

    private fun readCurrentSnapshot(): NetworkSignalSnapshot {
        val activeNetwork = connectivityManager.activeNetwork ?: return NetworkSignalSnapshot()
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            ?: return NetworkSignalSnapshot()
        return snapshotFromCapabilities(capabilities)
    }

    private fun snapshotFromCapabilities(capabilities: NetworkCapabilities): NetworkSignalSnapshot {
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (!hasInternet) return NetworkSignalSnapshot()

        val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isEthernet = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        val transportLabel = when {
            isWifi -> "WLAN"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobilfunk"
            isEthernet -> "LAN"
            else -> "Online"
        }

        val downstreamKbps = capabilities.linkDownstreamBandwidthKbps.coerceAtLeast(0)
        val rawDbm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val strength = capabilities.signalStrength
            if (strength != NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED && strength != Int.MIN_VALUE) {
                strength
            } else {
                null
            }
        } else {
            null
        }

        val bars = when {
            rawDbm != null -> when {
                rawDbm >= -55 -> 5
                rawDbm >= -65 -> 4
                rawDbm >= -75 -> 3
                rawDbm >= -85 -> 2
                else -> 1
            }
            downstreamKbps >= 20_000 -> 5
            downstreamKbps >= 8_000 -> 4
            downstreamKbps >= 2_500 -> 3
            downstreamKbps >= 600 -> 2
            else -> 3
        }

        return NetworkSignalSnapshot(
            isConnected = true,
            transportLabel = transportLabel,
            isLanOrWifi = isWifi || isEthernet,
            networkBars = bars,
            signalDbm = rawDbm,
            downstreamKbps = downstreamKbps
        )
    }

    fun release() {
        runCatching {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }
}
