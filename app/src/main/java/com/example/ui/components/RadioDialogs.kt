package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import com.example.R
import com.example.data.model.CountryOption
import com.example.data.model.RadioBand
import com.example.data.model.RadioStation
import com.example.data.model.SignalReceptionInfo
import com.example.data.radio.CarRadioHardwareState
import com.example.data.radio.HardwareTunerAvailability

@Composable
fun AddCustomStreamDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, url: String, band: RadioBand, freq: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var freq by remember { mutableStateOf("") }
    var band by remember { mutableStateOf(RadioBand.DAB_PLUS) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.add_stream_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.add_stream_name)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_stream_name_input")
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.add_stream_url)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_stream_url_input")
                )
                OutlinedTextField(
                    value = freq,
                    onValueChange = { freq = it },
                    label = { Text(stringResource(R.string.add_stream_freq)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.add_stream_band),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(RadioBand.DAB_PLUS, RadioBand.FM).forEach { option ->
                        FilterChip(
                            selected = band == option,
                            onClick = { band = option },
                            label = { Text(option.label) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        )
                    }
                }
                if (showError) {
                    Text(
                        text = stringResource(R.string.add_stream_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedUrl = url.trim()
                    val validUrl = trimmedUrl.startsWith("http://", ignoreCase = true) ||
                        trimmedUrl.startsWith("https://", ignoreCase = true)
                    if (name.isBlank() || !validUrl) {
                        showError = true
                    } else {
                        onSave(name.trim(), trimmedUrl, band, freq.trim())
                    }
                },
                modifier = Modifier.testTag("save_custom_stream_button")
            ) {
                Text(stringResource(R.string.add_stream_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.add_stream_cancel))
            }
        }
    )
}

@Composable
fun AssignPresetSlotDialog(
    station: RadioStation,
    presets: List<RadioStation>,
    onAssignSlot: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val presetsBySlot = remember(presets) { presets.associateBy { it.presetSlot } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${station.name} speichern",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..6).chunked(3).forEach { rowSlots ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowSlots.forEach { slot ->
                            val existing = presetsBySlot[slot]
                            Button(
                                onClick = {
                                    onAssignSlot(slot)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Text(
                                    text = if (existing == null) "P$slot" else "P$slot*",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "Tippe eine Stationstaste (P1–P6) zum Speichern.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.add_stream_cancel))
            }
        }
    )
}

@Composable
fun CountryPickerDialog(
    countries: List<CountryOption>,
    selectedCode: String,
    onSelectCountry: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(countries, query) {
        val q = query.trim().lowercase()
        if (q.isBlank()) countries
        else countries.filter {
            it.nameDe.lowercase().contains(q) || it.code.lowercase().contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.country_picker_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.country_search_placeholder)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("country_search_input")
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered, key = { it.code }) { option ->
                        val isSelected = option.code.equals(selectedCode, ignoreCase = true)
                        Surface(
                            onClick = {
                                onSelectCountry(option.code)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("country_dialog_item_${option.code.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surface
                                    ) {
                                        Text(
                                            text = option.code,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = option.nameDe,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (option.stationCount > 0) {
                                    Text(
                                        text = "${option.stationCount}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.add_stream_cancel))
            }
        }
    )
}

@Composable
fun AntennaStatusDialog(
    hardwareState: CarRadioHardwareState,
    reception: SignalReceptionInfo,
    onRequestUsbPermission: () -> Unit,
    onRefreshHardware: () -> Unit,
    onRefreshImages: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.SettingsInputAntenna,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.antenna_dialog_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = hardwareState.statusDetailDe,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Aktiver Empfangsweg: ${reception.transportLabel} (${reception.qualityLabel})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (reception.imageSourceLabel.isNotBlank()) {
                            Text(
                                text = "Senderbilder: ${reception.imageSourceLabel}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (hardwareState.usbDeviceName != null) {
                            Text(
                                text = "USB DAB+ Gerät: ${hardwareState.usbDeviceName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Text(
                    text = buildString {
                        append("• Auto-Head-Unit erkannt: ")
                        append(if (hardwareState.isAutomotiveDevice) "Ja" else "Standard-Android")
                        append("\n• BroadcastRadio-Hardware: ")
                        append(if (hardwareState.hasBroadcastRadioFeature) "Gemeldet" else "Nicht gemeldet")
                        append("\n• Audio-Priorität: 1. DAB+/FM Antenne · 2. USB DAB+ · 3. Mobilfunk / WLAN / LAN")
                        append("\n• Senderbilder: Aktualisierung über Antenne oder LAN / WLAN")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (hardwareState.availability == HardwareTunerAvailability.SYSTEM_RESTRICTED) {
                    Text(
                        text = "Hinweis: Android gibt die interne Auto-Antennen-API (ACCESS_BROADCAST_RADIO) nur für werkseitig signierte System-Apps frei. Alternativ kannst du einen USB DAB+ Adapter anschließen oder Mobilfunk/WLAN/LAN nutzen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (hardwareState.availability == HardwareTunerAvailability.USB_DAB_CONNECTED_NEEDS_PERMISSION) {
                    Button(
                        onClick = onRequestUsbPermission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("antenna_usb_permission_button")
                    ) {
                        Text(stringResource(R.string.antenna_usb_allow))
                    }
                }

                Button(
                    onClick = onRefreshImages,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("antenna_refresh_images_button")
                ) {
                    Text(stringResource(R.string.antenna_images_refresh))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRefreshHardware,
                modifier = Modifier.testTag("antenna_refresh_button")
            ) {
                Text(stringResource(R.string.antenna_refresh))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.recognition_close))
            }
        }
    )
}
