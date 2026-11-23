package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.CountryOption
import com.example.data.model.NowPlayingState
import com.example.data.model.RadioBand
import com.example.data.model.RadioStation
import com.example.ui.components.BandFilterBar
import com.example.ui.components.BandStationDropdownSection
import com.example.ui.components.StationRowCard
import com.example.ui.components.TransportControlsDeck
import com.example.ui.components.TunerDisplayDeck

@Composable
fun CockpitScreen(
    nowPlaying: NowPlayingState,
    filteredStations: List<RadioStation>,
    dabStations: List<RadioStation>,
    fmStations: List<RadioStation>,
    selectedBand: RadioBand,
    activeCountry: CountryOption,
    isDrivingMode: Boolean,
    onSelectBand: (RadioBand) -> Unit,
    onOpenCountryPicker: () -> Unit,
    onOpenAntennaDialog: () -> Unit = {},
    onTuneStation: (RadioStation, List<RadioStation>) -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRetryStream: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    onRequestAssignPresetDialog: (RadioStation) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isHeadUnitLandscape = maxWidth >= 560.dp && (maxWidth > maxHeight || maxWidth >= 680.dp)
        val isCompactHeadUnit = isHeadUnitLandscape && maxHeight < 580.dp
        val stationColumns = if (maxWidth >= 860.dp) 2 else 1

        if (isHeadUnitLandscape) {
            // 8-Inch Automotive Head-Unit Split Cockpit
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = if (isCompactHeadUnit) 12.dp else 16.dp,
                        vertical = if (isCompactHeadUnit) 8.dp else 12.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(if (isCompactHeadUnit) 12.dp else 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(if (isCompactHeadUnit) 8.dp else 14.dp)
                ) {
                    TunerDisplayDeck(
                        nowPlaying = nowPlaying,
                        activeCountry = activeCountry,
                        isDrivingMode = isDrivingMode,
                        onOpenCountryPicker = onOpenCountryPicker,
                        onOpenAntennaDialog = onOpenAntennaDialog,
                        onRetryStream = onRetryStream,
                        onToggleFavorite = onToggleFavorite,
                        compactHeadUnit = isCompactHeadUnit
                    )
                    TransportControlsDeck(
                        nowPlaying = nowPlaying,
                        isDrivingMode = isDrivingMode,
                        onPrevious = onPrevious,
                        onTogglePlayPause = onTogglePlayPause,
                        onNext = onNext,
                        onVolumeChange = onVolumeChange,
                        onToggleMute = onToggleMute,
                        compactHeadUnit = isCompactHeadUnit
                    )
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(stationColumns),
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isCompactHeadUnit) 8.dp else 12.dp)
                ) {
                    item(span = { GridItemSpan(stationColumns) }) {
                        BandStationDropdownSection(
                            dabStations = dabStations,
                            fmStations = fmStations,
                            currentStation = nowPlaying.station,
                            onSelectStation = onTuneStation,
                            compactHeadUnit = isCompactHeadUnit
                        )
                    }
                    item(span = { GridItemSpan(stationColumns) }) {
                        BandFilterBar(
                            selectedBand = selectedBand,
                            onSelectBand = onSelectBand
                        )
                    }
                    if (!isDrivingMode) {
                        item(span = { GridItemSpan(stationColumns) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedBand.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${filteredStations.size} verfügbar",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        items(filteredStations.take(40), key = { it.id }) { station ->
                            StationRowCard(
                                station = station,
                                isPlaying = station.id == nowPlaying.station?.id,
                                onSelect = { onTuneStation(station, filteredStations) },
                                onToggleFavorite = { onToggleFavorite(station) },
                                onQuickAssignPreset = { onRequestAssignPresetDialog(station) }
                            )
                        }
                    }
                }
            }
        } else {
            // Portrait Cockpit (Phone or Vertical 8" Display)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    TunerDisplayDeck(
                        nowPlaying = nowPlaying,
                        activeCountry = activeCountry,
                        isDrivingMode = isDrivingMode,
                        onOpenCountryPicker = onOpenCountryPicker,
                        onOpenAntennaDialog = onOpenAntennaDialog,
                        onRetryStream = onRetryStream,
                        onToggleFavorite = onToggleFavorite,
                        modifier = Modifier.widthIn(max = 640.dp)
                    )
                }
                item {
                    TransportControlsDeck(
                        nowPlaying = nowPlaying,
                        isDrivingMode = isDrivingMode,
                        onPrevious = onPrevious,
                        onTogglePlayPause = onTogglePlayPause,
                        onNext = onNext,
                        onVolumeChange = onVolumeChange,
                        onToggleMute = onToggleMute,
                        modifier = Modifier.widthIn(max = 640.dp)
                    )
                }
                item {
                    BandStationDropdownSection(
                        dabStations = dabStations,
                        fmStations = fmStations,
                        currentStation = nowPlaying.station,
                        onSelectStation = onTuneStation,
                        compactHeadUnit = false,
                        modifier = Modifier.widthIn(max = 640.dp)
                    )
                }
                item {
                    BandFilterBar(
                        selectedBand = selectedBand,
                        onSelectBand = onSelectBand,
                        modifier = Modifier.widthIn(max = 640.dp)
                    )
                }
                if (!isDrivingMode) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 640.dp)
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedBand.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${filteredStations.size} verfügbar",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(filteredStations.take(25), key = { it.id }) { station ->
                        StationRowCard(
                            station = station,
                            isPlaying = station.id == nowPlaying.station?.id,
                            onSelect = { onTuneStation(station, filteredStations) },
                            onToggleFavorite = { onToggleFavorite(station) },
                            onQuickAssignPreset = { onRequestAssignPresetDialog(station) },
                            modifier = Modifier.widthIn(max = 640.dp)
                        )
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}
