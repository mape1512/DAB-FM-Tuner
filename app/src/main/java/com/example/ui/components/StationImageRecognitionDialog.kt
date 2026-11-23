package com.example.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.ai.ImageRecognitionState
import com.example.data.ai.RecognitionConfidence
import com.example.data.ai.RecognizedStationInfo
import com.example.data.model.RadioBand
import com.example.data.model.RadioStation

@Composable
fun StationImageRecognitionDialog(
    state: ImageRecognitionState,
    currentStationId: String?,
    onPickImageUri: (android.net.Uri) -> Unit,
    onCaptureBitmap: (Bitmap) -> Unit,
    onTuneStation: (RadioStation, List<RadioStation>) -> Unit,
    onRequestAssignPreset: (RadioStation) -> Unit,
    onApplySearchQuery: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickImageUri(uri)
        }
    }

    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            onCaptureBitmap(bitmap)
        }
    }

    val launchGallery = {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
    val launchCamera = {
        cameraPreviewLauncher.launch(null)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            val isWideMonitor = maxWidth >= 560.dp
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .widthIn(max = if (isWideMonitor) 760.dp else 520.dp)
                    .fillMaxWidth()
                    .testTag("image_recognition_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (isWideMonitor) 20.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CenterFocusStrong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.recognition_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.recognition_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.recognition_close))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = launchCamera,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("recognition_camera_button")
                        ) {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.recognition_camera_button))
                        }
                        FilledTonalButton(
                            onClick = launchGallery,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("recognition_gallery_button")
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.recognition_gallery_button))
                        }
                    }

                    when (state) {
                        is ImageRecognitionState.Idle -> {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Unterstützte Motive",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "• Senderlogos (z. B. SWR3, 1LIVE, Ö3, SRF 3, Antenne Bayern)\n• Autoradio-Displays, RDS-Anzeigen & FM-Frequenzskalen (z. B. 98.4 MHz)\n• DAB+-Kanalnamen & Werbeplakate",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        is ImageRecognitionState.Analyzing -> {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Image(
                                        bitmap = state.previewBitmap.asImageBitmap(),
                                        contentDescription = stringResource(R.string.cd_image_recognition),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(88.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(22.dp),
                                                strokeWidth = 2.5.dp
                                            )
                                            Text(
                                                text = stringResource(R.string.recognition_analyzing),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(
                                            text = "Erkenne Senderlogo, Frequenz und DAB+/FM-Band…",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        is ImageRecognitionState.Result -> {
                            RecognitionResultContent(
                                previewBitmap = state.previewBitmap,
                                recognized = state.recognized,
                                matchedStations = state.matchedStations,
                                currentStationId = currentStationId,
                                isWideMonitor = isWideMonitor,
                                onTuneStation = { station ->
                                    onTuneStation(station, state.matchedStations)
                                    onDismiss()
                                },
                                onRequestAssignPreset = onRequestAssignPreset,
                                onApplySearchQuery = { query ->
                                    onApplySearchQuery(query)
                                    onDismiss()
                                },
                                onReset = onReset
                            )
                        }

                        is ImageRecognitionState.Error -> {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (state.previewBitmap != null) {
                                        Image(
                                            bitmap = state.previewBitmap.asImageBitmap(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.ErrorOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = state.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        OutlinedButton(onClick = onReset) {
                                            Text(stringResource(R.string.recognition_retry))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecognitionResultContent(
    previewBitmap: Bitmap,
    recognized: RecognizedStationInfo,
    matchedStations: List<RadioStation>,
    currentStationId: String?,
    isWideMonitor: Boolean,
    onTuneStation: (RadioStation) -> Unit,
    onRequestAssignPreset: (RadioStation) -> Unit,
    onApplySearchQuery: (String) -> Unit,
    onReset: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.cd_image_recognition),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(if (isWideMonitor) 92.dp else 72.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = recognized.stationName.ifBlank { "Kein Sendername erkannt" },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (recognized.confidence) {
                                RecognitionConfidence.HIGH -> MaterialTheme.colorScheme.primaryContainer
                                RecognitionConfidence.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer
                                RecognitionConfidence.LOW -> MaterialTheme.colorScheme.surface
                            }
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.recognition_confidence_badge,
                                    recognized.confidence.labelDe
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    val metaBadges = buildList {
                        if (recognized.bandHint != RadioBand.ALL) add(recognized.bandHint.label)
                        if (recognized.frequencyOrChannel.isNotBlank()) add(recognized.frequencyOrChannel)
                        if (recognized.countryCode.isNotBlank()) add(recognized.countryCode)
                    }
                    if (metaBadges.isNotEmpty()) {
                        Text(
                            text = metaBadges.joinToString(" · "),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = recognized.visualSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (matchedStations.isNotEmpty()) {
            Text(
                text = stringResource(R.string.recognition_matches_title, matchedStations.size),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = if (isWideMonitor) 220.dp else 250.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(matchedStations, key = { it.id }) { station ->
                    val isPlaying = station.id == currentStationId
                    Surface(
                        onClick = { onTuneStation(station) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPlaying) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recognition_match_${station.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = station.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${station.band.badgeText} · ${station.frequencyOrChannel} · ${station.countryCode}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(onClick = { onRequestAssignPreset(station) }) {
                                    Icon(
                                        imageVector = Icons.Filled.BookmarkAdd,
                                        contentDescription = stringResource(R.string.presets_title),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = { onTuneStation(station) },
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.recognition_play_now))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.recognition_no_matches),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (recognized.stationName.isNotBlank()) {
                            Button(onClick = { onApplySearchQuery(recognized.stationName) }) {
                                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.recognition_use_search))
                            }
                        }
                        OutlinedButton(onClick = onReset) {
                            Text(stringResource(R.string.recognition_retry))
                        }
                    }
                }
            }
        }
    }
}
