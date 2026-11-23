package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularOff
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import com.example.R
import com.example.data.model.CountryCatalog
import com.example.data.model.CountryOption
import com.example.data.model.NowPlayingState
import com.example.data.model.RadioBand
import com.example.data.model.RadioStation
import com.example.data.model.SignalReceptionInfo
import com.example.data.model.StreamStatus
import com.example.ui.theme.OnAirEmerald
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun BandBadge(
    band: RadioBand,
    modifier: Modifier = Modifier
) {
    val isDab = band == RadioBand.DAB_PLUS
    val containerColor = if (isDab) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (isDab) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (isDab) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
        )
    ) {
        Text(
            text = band.badgeText,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StationArtwork(
    faviconUrl: String,
    stationName: String,
    modifier: Modifier = Modifier,
    hardwareArtwork: Bitmap? = null
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        when {
            hardwareArtwork != null -> {
                Image(
                    bitmap = hardwareArtwork.asImageBitmap(),
                    contentDescription = "$stationName ${stringResource(R.string.cd_station_logo)}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            faviconUrl.isBlank() -> {
                Image(
                    painter = painterResource(id = R.drawable.img_station_default),
                    contentDescription = "$stationName ${stringResource(R.string.cd_station_logo)}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                val imageRequest = remember(faviconUrl, context) {
                    ImageRequest.Builder(context)
                        .data(faviconUrl)
                        .crossfade(true)
                        .build()
                }
                SubcomposeAsyncImage(
                    model = imageRequest,
                    contentDescription = "$stationName ${stringResource(R.string.cd_station_logo)}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Image(
                            painter = painterResource(id = R.drawable.img_station_default),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    },
                    error = {
                        Image(
                            painter = painterResource(id = R.drawable.img_station_default),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun TunerDisplayDeck(
    nowPlaying: NowPlayingState,
    activeCountry: CountryOption,
    isDrivingMode: Boolean,
    onOpenCountryPicker: () -> Unit,
    onOpenAntennaDialog: () -> Unit = {},
    onRetryStream: () -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    modifier: Modifier = Modifier,
    compactHeadUnit: Boolean = false
) {
    val station = nowPlaying.station
    val isDab = station?.band != RadioBand.FM
    val accentColor = if (isDab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    val isPlaying = nowPlaying.status == StreamStatus.Playing
    val isBuffering = nowPlaying.status == StreamStatus.Buffering
    val errorStatus = nowPlaying.status as? StreamStatus.Error

    val glowColor by animateColorAsState(
        targetValue = if (isPlaying) accentColor.copy(alpha = 0.24f) else accentColor.copy(alpha = 0.08f),
        animationSpec = tween(450),
        label = "tunerGlow"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(glowColor, Color.Transparent),
                        radius = 650f
                    )
                )
                .padding(
                    when {
                        isDrivingMode -> 22.dp
                        compactHeadUnit -> 12.dp
                        else -> 18.dp
                    }
                )
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(if (compactHeadUnit) 8.dp else 14.dp)) {
                // Status & Tech Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        val dotColor = when (nowPlaying.status) {
                            StreamStatus.Playing -> OnAirEmerald
                            StreamStatus.Buffering -> MaterialTheme.colorScheme.primary
                            is StreamStatus.Error -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Text(
                            text = when (nowPlaying.status) {
                                StreamStatus.Playing -> stringResource(R.string.status_playing)
                                StreamStatus.Buffering -> stringResource(R.string.status_buffering)
                                StreamStatus.Paused -> stringResource(R.string.status_paused)
                                is StreamStatus.Error -> stringResource(R.string.status_error)
                                StreamStatus.Idle -> stringResource(R.string.status_idle)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = dotColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            onClick = onOpenCountryPicker,
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.testTag("tuner_country_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Public,
                                    contentDescription = stringResource(R.string.cd_country_select),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = activeCountry.code,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        SignalReceptionPill(
                            reception = nowPlaying.reception,
                            modifier = Modifier.testTag("tuner_signal_indicator")
                        )
                        if (station != null) {
                            BandBadge(band = station.band)
                        }
                    }
                }

                // Live Signal Strength Telemetry Strip (Antenna + Cellular/WLAN)
                SignalTelemetryStrip(
                    reception = nowPlaying.reception,
                    isStreaming = isPlaying || isBuffering,
                    onClick = onOpenAntennaDialog
                )

                // Artwork + Station Title + Frequency Display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StationArtwork(
                        faviconUrl = station?.faviconUrl.orEmpty(),
                        stationName = station?.name ?: stringResource(R.string.app_name),
                        hardwareArtwork = nowPlaying.antennaArtwork,
                        modifier = Modifier.size(
                            when {
                                isDrivingMode -> 96.dp
                                compactHeadUnit -> 64.dp
                                else -> 82.dp
                            }
                        )
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = station?.frequencyOrChannel ?: "--- MHz",
                            style = if (isDrivingMode) {
                                MaterialTheme.typography.headlineMedium
                            } else {
                                MaterialTheme.typography.titleMedium
                            },
                            color = accentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = station?.name ?: stringResource(R.string.no_station_selected),
                            style = when {
                                isDrivingMode -> MaterialTheme.typography.displayMedium
                                compactHeadUnit -> MaterialTheme.typography.titleLarge
                                else -> MaterialTheme.typography.headlineMedium
                            },
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = if (compactHeadUnit) 1 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (station != null && station.genreTags.isNotBlank() && !compactHeadUnit) {
                            Text(
                                text = station.genreTags.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (station != null) {
                        IconButton(
                            onClick = { onToggleFavorite(station) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("tuner_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (station.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = stringResource(R.string.cd_favorite_toggle),
                                tint = if (station.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Tuner Dial & Live Spectrum Canvas
                FrequencyDialScale(
                    stationKey = station?.id ?: "none",
                    accentColor = accentColor,
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (compactHeadUnit) 22.dp else 34.dp)
                )

                // RDS / DLS Radiotext Box or Error Retry
                if (errorStatus != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = errorStatus.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = onRetryStream,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("retry_stream_button")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.retry_button), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.testTag("tuner_now_playing_track")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = if (compactHeadUnit) 8.dp else 11.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (isBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = accentColor
                                )
                            } else {
                                Icon(
                                    imageVector = if (nowPlaying.rdsTitle.isNotBlank()) Icons.Filled.MusicNote else Icons.Filled.GraphicEq,
                                    contentDescription = null,
                                    tint = if (isPlaying) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.now_playing_track_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPlaying) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                val trackTitleText = when {
                                    nowPlaying.rdsTitle.isNotBlank() -> nowPlaying.rdsTitle
                                    isPlaying -> stringResource(R.string.now_playing_live_no_meta)
                                    else -> stringResource(R.string.rds_waiting)
                                }
                                Text(
                                    text = trackTitleText,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = if (compactHeadUnit) 1 else 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("tuner_track_title")
                                )
                                if (nowPlaying.rdsArtist.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = stringResource(
                                                R.string.now_playing_by_artist,
                                                nowPlaying.rdsArtist
                                            ),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.testTag("tuner_track_artist")
                                        )
                                    }
                                }
                            }

                            AudioSpectrumBars(
                                isPlaying = isPlaying,
                                accentColor = accentColor,
                                modifier = Modifier
                                    .width(42.dp)
                                    .height(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FrequencyDialScale(
    stationKey: String,
    accentColor: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val targetFraction = (abs(stationKey.hashCode()) % 80 + 10) / 100f
    val needlePosition by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "dialNeedle"
    )
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val activeAlpha = if (isPlaying) 1f else 0.6f

    Canvas(modifier = modifier) {
        val totalTicks = 40
        val step = size.width / totalTicks
        val baselineY = size.height * 0.72f

        for (i in 0..totalTicks) {
            val x = i * step
            val isMajor = i % 5 == 0
            val tickHeight = if (isMajor) size.height * 0.45f else size.height * 0.24f
            drawLine(
                color = tickColor,
                start = Offset(x, baselineY - tickHeight),
                end = Offset(x, baselineY),
                strokeWidth = if (isMajor) 2.5f else 1.5f,
                cap = StrokeCap.Round
            )
        }

        val needleX = size.width * needlePosition
        drawLine(
            color = accentColor.copy(alpha = 0.25f * activeAlpha),
            start = Offset(needleX, 2f),
            end = Offset(needleX, size.height - 2f),
            strokeWidth = 10f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = accentColor.copy(alpha = activeAlpha),
            start = Offset(needleX, 2f),
            end = Offset(needleX, size.height - 2f),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun AudioSpectrumBars(
    isPlaying: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spectrum")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spectrumPhase"
    )

    Canvas(modifier = modifier) {
        val barCount = 5
        val gap = 4f
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        for (i in 0 until barCount) {
            val heightRatio = if (isPlaying) {
                0.28f + 0.68f * ((sin(phase + i * 1.15f) + 1f) / 2f)
            } else {
                0.2f
            }
            val barHeight = size.height * heightRatio
            drawRoundRect(
                color = accentColor,
                topLeft = Offset(i * (barWidth + gap), size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(3f, 3f)
            )
        }
    }
}

@Composable
fun TransportControlsDeck(
    nowPlaying: NowPlayingState,
    isDrivingMode: Boolean,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier,
    compactHeadUnit: Boolean = false
) {
    val isPlayingOrBuffering =
        nowPlaying.status == StreamStatus.Playing || nowPlaying.status == StreamStatus.Buffering
    val accentColor = if (nowPlaying.station?.band == RadioBand.FM) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = if (compactHeadUnit) 8.dp else 14.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (compactHeadUnit) 6.dp else 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onPrevious,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .size(
                            when {
                                isDrivingMode -> 68.dp
                                compactHeadUnit -> 50.dp
                                else -> 58.dp
                            }
                        )
                        .testTag("prev_station_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = stringResource(R.string.cd_previous_station),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(
                                when {
                                    isDrivingMode -> 36.dp
                                    compactHeadUnit -> 26.dp
                                    else -> 30.dp
                                }
                            )
                        )
                    }
                }

                Surface(
                    onClick = onTogglePlayPause,
                    shape = CircleShape,
                    color = accentColor,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(
                            when {
                                isDrivingMode -> 84.dp
                                compactHeadUnit -> 60.dp
                                else -> 74.dp
                            }
                        )
                        .testTag("play_pause_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlayingOrBuffering) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(
                                if (isPlayingOrBuffering) R.string.cd_pause else R.string.cd_play
                            ),
                            tint = if (nowPlaying.station?.band == RadioBand.FM) {
                                MaterialTheme.colorScheme.onSecondary
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            },
                            modifier = Modifier.size(
                                when {
                                    isDrivingMode -> 42.dp
                                    compactHeadUnit -> 32.dp
                                    else -> 38.dp
                                }
                            )
                        )
                    }
                }

                Surface(
                    onClick = onNext,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .size(
                            when {
                                isDrivingMode -> 68.dp
                                compactHeadUnit -> 50.dp
                                else -> 58.dp
                            }
                        )
                        .testTag("next_station_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = stringResource(R.string.cd_next_station),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(
                                when {
                                    isDrivingMode -> 36.dp
                                    compactHeadUnit -> 26.dp
                                    else -> 30.dp
                                }
                            )
                        )
                    }
                }
            }

            // In-App Volume / Mute Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("mute_button")
                ) {
                    Icon(
                        imageVector = if (nowPlaying.isMuted || nowPlaying.volume == 0f) {
                            Icons.AutoMirrored.Filled.VolumeOff
                        } else {
                            Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = stringResource(R.string.cd_mute_toggle),
                        tint = if (nowPlaying.isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Slider(
                    value = if (nowPlaying.isMuted) 0f else nowPlaying.volume,
                    onValueChange = onVolumeChange,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("volume_slider")
                )

                Text(
                    text = "${((if (nowPlaying.isMuted) 0f else nowPlaying.volume) * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(44.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PresetTile(
    slotNumber: Int,
    station: RadioStation?,
    isCurrent: Boolean,
    onTap: () -> Unit,
    onLongPressAssign: () -> Unit,
    modifier: Modifier = Modifier,
    compactHeadUnit: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val activeBorderColor = when {
        isCurrent && station?.band == RadioBand.FM -> MaterialTheme.colorScheme.secondary
        isCurrent -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    val containerColor = when {
        isCurrent && station?.band == RadioBand.FM -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        isCurrent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        station != null -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }

    Surface(
        modifier = modifier
            .height(if (compactHeadUnit) 58.dp else 72.dp)
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = onTap,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPressAssign()
                }
            )
            .testTag("preset_slot_$slotNumber"),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        border = BorderStroke(if (isCurrent) 2.dp else 1.dp, activeBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "P$slotNumber",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isCurrent) activeBorderColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (station != null) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (station.band == RadioBand.DAB_PLUS) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.secondary
                            )
                    )
                }
            }
            Text(
                text = station?.name ?: stringResource(R.string.preset_empty),
                style = MaterialTheme.typography.labelLarge,
                color = if (station != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BandFilterBar(
    selectedBand: RadioBand,
    onSelectBand: (RadioBand) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioBand.entries.forEach { band ->
            val selected = band == selectedBand
            val testTag = when (band) {
                RadioBand.ALL -> "band_filter_all"
                RadioBand.DAB_PLUS -> "band_filter_dab"
                RadioBand.FM -> "band_filter_fm"
            }
            FilterChip(
                selected = selected,
                onClick = { onSelectBand(band) },
                label = {
                    Text(
                        text = band.label,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = if (band == RadioBand.FM) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    selectedLabelColor = if (band == RadioBand.FM) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag(testTag)
            )
        }
    }
}

@Composable
fun BandStationDropdownSection(
    dabStations: List<RadioStation>,
    fmStations: List<RadioStation>,
    currentStation: RadioStation?,
    onSelectStation: (RadioStation, List<RadioStation>) -> Unit,
    modifier: Modifier = Modifier,
    compactHeadUnit: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BandStationDropdownSelector(
            title = stringResource(R.string.dab_dropdown_title),
            band = RadioBand.DAB_PLUS,
            stations = dabStations,
            currentStation = currentStation,
            onSelectStation = { station -> onSelectStation(station, dabStations) },
            compactHeadUnit = compactHeadUnit,
            testTagName = "dab_dropdown_button",
            modifier = Modifier.weight(1f)
        )
        BandStationDropdownSelector(
            title = stringResource(R.string.fm_dropdown_title),
            band = RadioBand.FM,
            stations = fmStations,
            currentStation = currentStation,
            onSelectStation = { station -> onSelectStation(station, fmStations) },
            compactHeadUnit = compactHeadUnit,
            testTagName = "fm_dropdown_button",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BandStationDropdownSelector(
    title: String,
    band: RadioBand,
    stations: List<RadioStation>,
    currentStation: RadioStation?,
    onSelectStation: (RadioStation) -> Unit,
    compactHeadUnit: Boolean,
    testTagName: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val isBandActive = currentStation?.band == band
    val isDab = band == RadioBand.DAB_PLUS

    val accentColor = if (isDab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    val containerColor = when {
        isBandActive && isDab -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
        isBandActive -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = if (isBandActive) accentColor else MaterialTheme.colorScheme.outline

    val displayText = if (isBandActive && currentStation != null) {
        currentStation.name
    } else {
        stringResource(R.string.dropdown_select_station, stations.size)
    }

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = MaterialTheme.shapes.medium,
            color = containerColor,
            border = BorderStroke(if (isBandActive) 2.dp else 1.dp, borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compactHeadUnit) 58.dp else 66.dp)
                .testTag(testTagName)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = accentColor,
                        maxLines = 1
                    )
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .widthIn(min = 240.dp, max = 360.dp)
                .heightIn(max = 360.dp)
        ) {
            if (stations.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.dropdown_empty_band),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = { expanded = false },
                    enabled = false
                )
            } else {
                stations.forEachIndexed { index, station ->
                    val isSelected = station.id == currentStation?.id
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = station.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = station.frequencyOrChannel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        },
                        trailingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = accentColor
                                )
                            }
                        } else null,
                        onClick = {
                            expanded = false
                            onSelectStation(station)
                        },
                        modifier = Modifier.testTag("${testTagName}_item_${station.id}")
                    )
                    if (index < stations.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    }
                }
            }
        }
    }
}

@Composable
fun SignalStrengthBars(
    bars: Int,
    modifier: Modifier = Modifier
) {
    val clampedBars = bars.coerceIn(0, 5)
    val activeColor = when {
        clampedBars <= 0 -> MaterialTheme.colorScheme.error
        clampedBars <= 2 -> MaterialTheme.colorScheme.error
        clampedBars == 3 -> MaterialTheme.colorScheme.primary
        else -> OnAirEmerald
    }
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)

    Canvas(modifier = modifier) {
        val barCount = 5
        val gap = 3.dp.toPx()
        val barWidth = ((size.width - gap * (barCount - 1)) / barCount).coerceAtLeast(2f)
        for (i in 0 until barCount) {
            val heightFraction = 0.28f + (i / (barCount - 1f)) * 0.72f
            val barHeight = size.height * heightFraction
            val isActive = i < clampedBars
            drawRoundRect(
                color = if (isActive) activeColor else inactiveColor,
                topLeft = Offset(i * (barWidth + gap), size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}

@Composable
fun SignalReceptionPill(
    reception: SignalReceptionInfo,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SignalStrengthBars(
                bars = reception.bars,
                modifier = Modifier
                    .width(24.dp)
                    .height(14.dp)
            )
            Text(
                text = "${reception.bars}/5",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SignalTelemetryStrip(
    reception: SignalReceptionInfo,
    isStreaming: Boolean,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val detailText = buildString {
        append(stringResource(R.string.signal_label))
        append(": ")
        append(reception.qualityLabel)
        append(" · ")
        append(reception.transportLabel)
        if (!reception.isAntennaActive && isStreaming && reception.bufferSeconds > 0f) {
            append(" · Puffer ")
            append(String.format(Locale.GERMANY, "%.1fs", reception.bufferSeconds))
        }
        reception.signalDbm?.let { dbm ->
            append(" · ")
            append("$dbm dBm")
        }
    }

    val transportIcon = when {
        reception.isAntennaActive -> Icons.Filled.SettingsInputAntenna
        reception.isUsbDabDetected -> Icons.Filled.Usb
        reception.transportLabel == "Mobilfunk" -> Icons.Filled.SignalCellularAlt
        reception.transportLabel == "WLAN" -> Icons.Filled.Wifi
        !reception.isConnected -> Icons.Filled.SignalCellularOff
        else -> Icons.Filled.SettingsInputAntenna
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(
            1.dp,
            if (reception.isAntennaActive || reception.usbNeedsPermission) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("tuner_antenna_status_strip")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SignalStrengthBars(
                bars = reception.bars,
                modifier = Modifier
                    .width(26.dp)
                    .height(15.dp)
            )
            Icon(
                imageVector = transportIcon,
                contentDescription = null,
                tint = if (reception.isAntennaActive || reception.isUsbDabDetected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(16.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = detailText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (reception.antennaStatusText.isNotBlank()) {
                    Text(
                        text = reception.antennaStatusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (reception.isAntennaActive || reception.usbNeedsPermission) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun CountryFilterRow(
    activeCountry: CountryOption,
    onSelectCountry: (String) -> Unit,
    onOpenCountryPicker: () -> Unit,
    modifier: Modifier = Modifier,
    showQuickCountries: Boolean = true
) {
    val quickCodes = (listOf(activeCountry.code) + CountryCatalog.quickCountryCodes).distinct()

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            Surface(
                onClick = onOpenCountryPicker,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("active_country_picker_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Public,
                        contentDescription = stringResource(R.string.cd_country_select),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${activeCountry.code} · ${activeCountry.nameDe}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        if (showQuickCountries) {
            items(quickCodes.filter { it != activeCountry.code }, key = { it }) { code ->
                FilterChip(
                    selected = false,
                    onClick = { onSelectCountry(code) },
                    label = {
                        Text(
                            text = "$code · ${CountryCatalog.displayNameFor(code)}",
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("country_filter_${code.lowercase()}")
                )
            }
            item {
                FilterChip(
                    selected = false,
                    onClick = onOpenCountryPicker,
                    label = {
                        Text(
                            text = stringResource(R.string.country_more),
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("country_more_button")
                )
            }
        }
    }
}

@Composable
fun StationRowCard(
    station: RadioStation,
    isPlaying: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onQuickAssignPreset: (() -> Unit)? = null,
    onDeleteCustom: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isPlaying && station.band == RadioBand.FM -> MaterialTheme.colorScheme.secondary
        isPlaying -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        onClick = onSelect,
        modifier = modifier
            .fillMaxWidth()
            .testTag("station_card_${station.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(if (isPlaying) 2.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StationArtwork(
                faviconUrl = station.faviconUrl,
                stationName = station.name,
                modifier = Modifier.size(54.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = station.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isPlaying) {
                        Icon(
                            imageVector = Icons.Filled.Radio,
                            contentDescription = null,
                            tint = borderColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BandBadge(band = station.band)
                    Text(
                        text = station.frequencyOrChannel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (station.presetSlot != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "P${station.presetSlot}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (station.genreTags.isNotBlank()) {
                    Text(
                        text = station.genreTags,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onQuickAssignPreset != null) {
                IconButton(
                    onClick = onQuickAssignPreset,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.BookmarkAdd,
                        contentDescription = stringResource(R.string.presets_title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (station.isCustom && onDeleteCustom != null) {
                IconButton(
                    onClick = onDeleteCustom,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("favorite_station_${station.id}")
            ) {
                Icon(
                    imageVector = if (station.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(R.string.cd_favorite_toggle),
                    tint = if (station.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
