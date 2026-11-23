package com.example.data.radio

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.example.data.model.RadioBand
import com.example.data.model.RadioStation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.reflect.Proxy

private const val TAG = "CarRadioHardwareMonitor"
private const val ACTION_USB_PERMISSION = "com.example.USB_DAB_PERMISSION"

enum class ReceptionSourceMode {
    BROADCAST_RADIO_ANTENNA,
    USB_DAB_ANTENNA,
    CELLULAR_STREAM,
    WIFI_STREAM,
    ETHERNET_STREAM,
    OFFLINE
}

enum class HardwareTunerAvailability {
    ACTIVE_DAB_FM_TUNER,
    SYSTEM_RESTRICTED,
    USB_DAB_CONNECTED_NEEDS_PERMISSION,
    USB_DAB_READY,
    UNAVAILABLE
}

data class CarRadioHardwareState(
    val availability: HardwareTunerAvailability = HardwareTunerAvailability.UNAVAILABLE,
    val hasBroadcastRadioFeature: Boolean = false,
    val isAutomotiveDevice: Boolean = false,
    val isHardwarePlaying: Boolean = false,
    val activeHardwareBand: RadioBand? = null,
    val hardwareSignalPercent: Int? = null,
    val usbDeviceName: String? = null,
    val hardwareProgramName: String = "",
    val hardwareSongTitle: String = "",
    val hardwareSongArtist: String = "",
    val hardwareStationArtwork: Bitmap? = null,
    val statusDetailDe: String = "Stream-Betrieb (keine direkte Hardware-Antenne freigegeben)"
) {
    val antennaBars: Int
        get() = when (val pct = hardwareSignalPercent) {
            null -> if (isHardwarePlaying) 4 else 0
            in 80..100 -> 5
            in 60..79 -> 4
            in 40..59 -> 3
            in 20..39 -> 2
            in 1..19 -> 1
            else -> 0
        }
}

class CarRadioHardwareMonitor(context: Context) {

    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val usbManager = appContext.getSystemService(Context.USB_SERVICE) as? UsbManager

    private var radioManagerInstance: Any? = null
    private var activeRadioTuner: Any? = null

    private val _state = MutableStateFlow(CarRadioHardwareState())
    val state: StateFlow<CarRadioHardwareState> = _state.asStateFlow()

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED,
                UsbManager.ACTION_USB_DEVICE_DETACHED,
                ACTION_USB_PERMISSION -> {
                    refreshHardwareStatus()
                }
            }
        }
    }

    init {
        registerUsbReceiver()
        probeBroadcastRadioAndUsb()
    }

    private fun registerUsbReceiver() {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(ACTION_USB_PERMISSION)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                appContext.registerReceiver(usbReceiver, filter)
            }
        }
    }

    fun refreshHardwareStatus() {
        probeBroadcastRadioAndUsb()
    }

    fun requestUsbDabPermission() {
        val manager = usbManager ?: return
        val usbTuner = findUsbDabDevice() ?: return
        if (manager.hasPermission(usbTuner)) {
            refreshHardwareStatus()
            return
        }
        val intent = Intent(ACTION_USB_PERMISSION).setPackage(appContext.packageName)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(appContext, 0, intent, flags)
        runCatching {
            manager.requestPermission(usbTuner, pendingIntent)
        }.onFailure { e ->
            Log.w(TAG, "USB DAB permission request failed", e)
        }
    }

    /**
     * Attempts to tune the physical FM/DAB car radio tuner hardware when the Android head unit
     * exposes `android.hardware.radio.RadioManager` to this app.
     * Returns true if hardware tuning succeeded, false when streaming fallback should be used.
     */
    fun tryTuneViaAntenna(station: RadioStation): Boolean {
        val manager = radioManagerInstance ?: return false
        return try {
            val freqKhz = parseFmFrequencyKhz(station.frequencyOrChannel)
            val tuner = activeRadioTuner ?: openHardwareTuner(manager, station.band)
            if (tuner == null) return false
            activeRadioTuner = tuner

            if (station.band == RadioBand.FM && freqKhz != null) {
                val stepMethod = tuner.javaClass.methods.firstOrNull {
                    it.name == "tune" && it.parameterTypes.size == 2 &&
                        it.parameterTypes[0] == Int::class.javaPrimitiveType
                }
                if (stepMethod != null) {
                    val result = stepMethod.invoke(tuner, freqKhz, 0) as? Int ?: -1
                    if (result == 0) {
                        _state.value = _state.value.copy(
                            availability = HardwareTunerAvailability.ACTIVE_DAB_FM_TUNER,
                            isHardwarePlaying = true,
                            activeHardwareBand = station.band,
                            hardwareProgramName = station.name,
                            hardwareSongTitle = "",
                            hardwareSongArtist = "",
                            hardwareStationArtwork = null,
                            statusDetailDe = "${station.band.badgeText}-Antenne aktiv (${station.frequencyOrChannel})"
                        )
                        runCatching {
                            val programInfo = arrayOfNulls<Any>(1)
                            val getInfoMethod = tuner.javaClass.methods.firstOrNull {
                                it.name == "getProgramInformation" && it.parameterTypes.size == 1
                            }
                            val infoStatus = getInfoMethod?.invoke(tuner, programInfo) as? Int ?: -1
                            if (infoStatus == 0 && programInfo[0] != null) {
                                applyProgramInfo(programInfo[0]!!, station.band)
                            }
                        }
                        return true
                    }
                }
            }
            false
        } catch (se: SecurityException) {
            Log.i(TAG, "BroadcastRadio permission restricted by Android system; using stream fallback", se)
            _state.value = _state.value.copy(
                availability = HardwareTunerAvailability.SYSTEM_RESTRICTED,
                isHardwarePlaying = false,
                statusDetailDe = "Antennen-API vom Autoradio-System gesperrt (System-Signatur nötig)"
            )
            false
        } catch (e: Exception) {
            Log.w(TAG, "Hardware antenna tune fallback to stream", e)
            false
        }
    }

    fun stopHardwarePlayback() {
        runCatching {
            activeRadioTuner?.javaClass?.methods
                ?.firstOrNull { it.name == "setMuted" && it.parameterTypes.size == 1 }
                ?.invoke(activeRadioTuner, true)
        }
        if (_state.value.isHardwarePlaying) {
            _state.value = _state.value.copy(isHardwarePlaying = false)
        }
    }

    private fun probeBroadcastRadioAndUsb() {
        val isAuto = packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
        val hasBroadcastFeature = packageManager.hasSystemFeature("android.hardware.broadcastradio")

        @Suppress("WrongConstant")
        val serviceObj = runCatching {
            appContext.getSystemService("broadcastradio")
        }.getOrNull()
        radioManagerInstance = serviceObj

        if (serviceObj != null) {
            val probeResult = probeRadioManagerModules(serviceObj)
            if (probeResult == HardwareTunerAvailability.ACTIVE_DAB_FM_TUNER) {
                _state.value = _state.value.copy(
                    availability = HardwareTunerAvailability.ACTIVE_DAB_FM_TUNER,
                    hasBroadcastRadioFeature = true,
                    isAutomotiveDevice = isAuto,
                    statusDetailDe = "DAB+/FM-Antennentuner des Autoradios bereit"
                )
                return
            } else if (probeResult == HardwareTunerAvailability.SYSTEM_RESTRICTED) {
                val usbDevice = findUsbDabDevice()
                if (usbDevice != null) {
                    val hasPerm = usbManager?.hasPermission(usbDevice) == true
                    _state.value = _state.value.copy(
                        availability = if (hasPerm) {
                            HardwareTunerAvailability.USB_DAB_READY
                        } else {
                            HardwareTunerAvailability.USB_DAB_CONNECTED_NEEDS_PERMISSION
                        },
                        hasBroadcastRadioFeature = true,
                        isAutomotiveDevice = isAuto,
                        usbDeviceName = usbDevice.productName ?: usbDevice.deviceName,
                        statusDetailDe = if (hasPerm) {
                            "USB DAB+-Antenne verbunden (${usbDevice.productName ?: "Tuner"})"
                        } else {
                            "USB DAB+-Stick erkannt – Zugriff erlauben"
                        }
                    )
                    return
                }

                _state.value = _state.value.copy(
                    availability = HardwareTunerAvailability.SYSTEM_RESTRICTED,
                    hasBroadcastRadioFeature = true,
                    isAutomotiveDevice = isAuto,
                    statusDetailDe = "Auto-Tuner vorhanden, Systemzugriff geschützt – Stream aktiv"
                )
                return
            }
        }

        val usbDevice = findUsbDabDevice()
        if (usbDevice != null) {
            val hasPerm = usbManager?.hasPermission(usbDevice) == true
            _state.value = _state.value.copy(
                availability = if (hasPerm) {
                    HardwareTunerAvailability.USB_DAB_READY
                } else {
                    HardwareTunerAvailability.USB_DAB_CONNECTED_NEEDS_PERMISSION
                },
                hasBroadcastRadioFeature = hasBroadcastFeature,
                isAutomotiveDevice = isAuto,
                usbDeviceName = usbDevice.productName ?: usbDevice.deviceName,
                statusDetailDe = if (hasPerm) {
                    "USB DAB+-Antenne angeschlossen (${usbDevice.productName ?: "DAB+ Stick"})"
                } else {
                    "USB DAB+-Antenne erkannt – Berechtigung erteilen"
                }
            )
            return
        }

        _state.value = CarRadioHardwareState(
            availability = HardwareTunerAvailability.UNAVAILABLE,
            hasBroadcastRadioFeature = hasBroadcastFeature,
            isAutomotiveDevice = isAuto,
            statusDetailDe = if (isAuto || hasBroadcastFeature) {
                "Kein direkter Antennentreiber freigegeben – Mobilfunk/WLAN-Stream aktiv"
            } else {
                "Keine DAB+/FM-Hardware-Antenne erkannt – Mobilfunk/WLAN-Stream aktiv"
            }
        )
    }

    private fun probeRadioManagerModules(manager: Any): HardwareTunerAvailability {
        return try {
            val listModulesMethod = manager.javaClass.methods.firstOrNull {
                it.name == "listModules" && it.parameterTypes.size == 1
            } ?: return HardwareTunerAvailability.UNAVAILABLE

            val modules = mutableListOf<Any>()
            val status = listModulesMethod.invoke(manager, modules) as? Int ?: -1
            if (status == 0 && modules.isNotEmpty()) {
                HardwareTunerAvailability.ACTIVE_DAB_FM_TUNER
            } else {
                HardwareTunerAvailability.UNAVAILABLE
            }
        } catch (e: Exception) {
            val cause = e.cause ?: e
            if (cause is SecurityException || e is SecurityException) {
                HardwareTunerAvailability.SYSTEM_RESTRICTED
            } else {
                Log.d(TAG, "RadioManager module probe failed", cause)
                HardwareTunerAvailability.UNAVAILABLE
            }
        }
    }

    private fun openHardwareTuner(manager: Any, band: RadioBand): Any? {
        return try {
            val radioManagerClass = Class.forName("android.hardware.radio.RadioManager")
            val modulePropClass = Class.forName("android.hardware.radio.RadioManager\$ModuleProperties")
            val bandConfigClass = Class.forName("android.hardware.radio.RadioManager\$BandConfig")
            val callbackClass = Class.forName("android.hardware.radio.RadioTuner\$Callback")

            val modules = mutableListOf<Any>()
            val listMethod = radioManagerClass.getMethod("listModules", MutableList::class.java)
            val status = listMethod.invoke(manager, modules) as? Int ?: -1
            if (status != 0 || modules.isEmpty()) return null

            val firstModule = modules.first()
            val getBandsMethod = modulePropClass.getMethod("getBands")
            val bands = getBandsMethod.invoke(firstModule) as? Array<*> ?: return null
            if (bands.isEmpty()) return null

            val fmConfigClass = Class.forName("android.hardware.radio.RadioManager\$FmBandConfig")
            val fmDescriptorClass = Class.forName("android.hardware.radio.RadioManager\$FmBandDescriptor")
            val targetDescriptor = bands.firstOrNull { fmDescriptorClass.isInstance(it) } ?: bands.first()

            val bandConfig = if (fmDescriptorClass.isInstance(targetDescriptor)) {
                val builderClass = Class.forName("android.hardware.radio.RadioManager\$FmBandConfig\$Builder")
                val builder = builderClass.getConstructor(fmDescriptorClass).newInstance(targetDescriptor)
                builderClass.getMethod("setStereo", Boolean::class.javaPrimitiveType).invoke(builder, true)
                builderClass.getMethod("build").invoke(builder)
            } else {
                null
            } ?: return null

            val callbackProxy = Proxy.newProxyInstance(
                callbackClass.classLoader,
                arrayOf(callbackClass)
            ) { _, method, args ->
                when (method.name) {
                    "onAntennaStateChange" -> {
                        val connected = (args?.firstOrNull() as? Boolean) ?: true
                        _state.value = _state.value.copy(
                            statusDetailDe = if (connected) {
                                "Auto-Antenne verbunden"
                            } else {
                                "Auto-Antenne getrennt"
                            }
                        )
                    }
                    "onProgramInfoChanged" -> {
                        val info = args?.firstOrNull()
                        if (info != null) {
                            applyProgramInfo(info, band)
                        }
                    }
                }
                null
            }

            val openTunerMethod = radioManagerClass.getMethod(
                "openTuner",
                Int::class.javaPrimitiveType,
                bandConfigClass,
                Boolean::class.javaPrimitiveType,
                callbackClass,
                android.os.Handler::class.java
            )
            val id = modulePropClass.getMethod("getId").invoke(firstModule) as Int
            openTunerMethod.invoke(manager, id, bandConfig, true, callbackProxy, null)
        } catch (se: SecurityException) {
            _state.value = _state.value.copy(
                availability = HardwareTunerAvailability.SYSTEM_RESTRICTED,
                statusDetailDe = "Antennen-API vom Autoradio-System gesperrt (System-Signatur nötig)"
            )
            null
        } catch (e: Exception) {
            Log.d(TAG, "Unable to open RadioTuner hardware", e)
            null
        }
    }

    private fun findUsbDabDevice(): UsbDevice? {
        val devices = usbManager?.deviceList?.values ?: return null
        return devices.firstOrNull { device ->
            val name = "${device.productName.orEmpty()} ${device.manufacturerName.orEmpty()}".lowercase()
            device.vendorId in KNOWN_DAB_USB_VIDS ||
                name.contains("dab") ||
                name.contains("rtl") ||
                name.contains("dvb") ||
                name.contains("tuner") ||
                name.contains("radio")
        }
    }

    private fun applyProgramInfo(info: Any, fallbackBand: RadioBand) {
        val quality = runCatching {
            info.javaClass.getMethod("getSignalQuality").invoke(info) as? Int
        }.getOrNull()

        val metadata = runCatching {
            info.javaClass.getMethod("getMetadata").invoke(info)
        }.getOrNull()

        var parsedTitle = _state.value.hardwareSongTitle
        var parsedArtist = _state.value.hardwareSongArtist
        var parsedProgram = _state.value.hardwareProgramName
        var parsedArtwork = _state.value.hardwareStationArtwork

        if (metadata != null) {
            val getStringMethod = runCatching {
                metadata.javaClass.getMethod("getString", String::class.java)
            }.getOrNull()
            val getBitmapMethod = runCatching {
                metadata.javaClass.getMethod("getBitmap", String::class.java)
            }.getOrNull()

            if (getStringMethod != null) {
                fun readMetaString(key: String): String =
                    (runCatching { getStringMethod.invoke(metadata, key) as? String }.getOrNull())
                        ?.trim().orEmpty()

                val ps = readMetaString("android.hardware.radio.metadata.ps")
                val dabService = readMetaString("android.hardware.radio.metadata.dab_service_name")
                val rt = readMetaString("android.hardware.radio.metadata.rt")
                val title = readMetaString("android.hardware.radio.metadata.title")
                val artist = readMetaString("android.hardware.radio.metadata.artist")
                val dabComp = readMetaString("android.hardware.radio.metadata.dab_component_name")

                val bestProgram = ps.ifBlank { dabService }.ifBlank { dabComp }
                if (bestProgram.isNotBlank()) parsedProgram = bestProgram

                val bestTitle = title.ifBlank { rt }
                if (bestTitle.isNotBlank()) parsedTitle = bestTitle
                if (artist.isNotBlank()) parsedArtist = artist
            }

            if (getBitmapMethod != null) {
                val artBitmap = runCatching {
                    getBitmapMethod.invoke(metadata, "android.hardware.radio.metadata.art") as? Bitmap
                }.getOrNull()
                val iconBitmap = runCatching {
                    getBitmapMethod.invoke(metadata, "android.hardware.radio.metadata.icon") as? Bitmap
                }.getOrNull()
                val resolvedBitmap = artBitmap ?: iconBitmap
                if (resolvedBitmap != null) {
                    parsedArtwork = resolvedBitmap
                }
            }
        }

        _state.value = _state.value.copy(
            hardwareSignalPercent = quality ?: _state.value.hardwareSignalPercent,
            isHardwarePlaying = true,
            activeHardwareBand = _state.value.activeHardwareBand ?: fallbackBand,
            hardwareProgramName = parsedProgram,
            hardwareSongTitle = parsedTitle,
            hardwareSongArtist = parsedArtist,
            hardwareStationArtwork = parsedArtwork
        )
    }

    private fun parseFmFrequencyKhz(freqText: String): Int? {
        val match = Regex("""(\d{2,3})[.,](\d)""").find(freqText) ?: return null
        val mhzWhole = match.groupValues[1].toIntOrNull() ?: return null
        val tenths = match.groupValues[2].toIntOrNull() ?: return null
        val khz = mhzWhole * 1000 + tenths * 100
        return if (khz in 76000..108000) khz else null
    }

    fun release() {
        runCatching {
            activeRadioTuner?.javaClass?.getMethod("close")?.invoke(activeRadioTuner)
        }
        activeRadioTuner = null
        runCatching {
            appContext.unregisterReceiver(usbReceiver)
        }
    }

    companion object {
        // Common VIDs for RTL2832U / USB DAB+ car radio adapters (Realtek, TerraTec, Afatech, Dexatek, Elonics, etc.)
        private val KNOWN_DAB_USB_VIDS = setOf(
            0x0BDA, // Realtek (RTL2832U)
            0x0CCD, // TerraTec
            0x1D19, // Dexatek
            0x15A4, // Afatech
            0x1F4D, // G-Tek / Lifeview
            0x16C0, // Generic DAB+ / SDR USB
            0x0416, // Winbond / DAB Stick
            0x048D, // ITE Tech
            0x185B  // Compro
        )
    }
}
