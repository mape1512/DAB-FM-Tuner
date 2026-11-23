package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CountryOption
import com.example.data.model.RadioBand
import com.example.data.model.RadioStation
import com.example.ui.components.BandFilterBar
import com.example.ui.components.CountryFilterRow
import com.example.ui.components.StationRowCard

@Composable
fun StationsScreen(
    stations: List<RadioStation>,
    currentStationId: String?,
    searchQuery: String,
    selectedBand: RadioBand,
    activeCountry: CountryOption,
    selectedGenre: String?,
    isSyncing: Boolean,
    onSearchChange: (String) -> Unit,
    onSelectBand: (RadioBand) -> Unit,
    onSelectCountry: (String) -> Unit,
    onOpenCountryPicker: () -> Unit,
    onSelectGenre: (String) -> Unit,
    onRefreshOnline: () -> Unit,
    onOpenAddCustomStream: () -> Unit,
    onOpenImageRecognition: () -> Unit,
    onTuneStation: (RadioStation, List<RadioStation>) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    onRequestAssignPreset: (RadioStation) -> Unit,
    onDeleteCustomStation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val genres = listOf("Pop", "Rock", "Nachrichten", "Electronic", "Klassik", "Jazz", "Schlager")

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideMonitor = maxWidth >= 600.dp
        val columns = when {
            maxWidth >= 1060.dp -> 3
            isWideMonitor -> 2
            else -> 1
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 1160.dp)
                    .padding(top = 10.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = { Text(stringResource(R.string.search_placeholder)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("station_search_input")
                    )

                    if (isWideMonitor) {
                        BandFilterBar(
                            selectedBand = selectedBand,
                            onSelectBand = onSelectBand,
                            modifier = Modifier.weight(0.95f)
                        )
                    }

                    FilledTonalButton(
                        onClick = onOpenImageRecognition,
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("stations_image_recognition_button")
                    ) {
                        Icon(
                            Icons.Filled.CenterFocusStrong,
                            contentDescription = stringResource(R.string.cd_image_recognition)
                        )
                    }

                    FilledTonalButton(
                        onClick = onOpenAddCustomStream,
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("add_custom_stream_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add_station))
                    }
                }

                if (!isWideMonitor) {
                    BandFilterBar(
                        selectedBand = selectedBand,
                        onSelectBand = onSelectBand
                    )
                }

                CountryFilterRow(
                    activeCountry = activeCountry,
                    onSelectCountry = onSelectCountry,
                    onOpenCountryPicker = onOpenCountryPicker,
                    showQuickCountries = false
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilledTonalButton(
                            onClick = onRefreshOnline,
                            enabled = !isSyncing,
                            modifier = Modifier
                                .height(42.dp)
                                .testTag("refresh_stations_button")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Refresh,
                                    contentDescription = stringResource(R.string.cd_refresh_stations),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.refresh_button))
                        }
                    }
                    items(genres) { genre ->
                        FilterChip(
                            selected = selectedGenre.equals(genre, ignoreCase = true),
                            onClick = { onSelectGenre(genre) },
                            label = { Text(genre) },
                            modifier = Modifier.height(42.dp)
                        )
                    }
                }
            }

            if (stations.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Radio,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.empty_stations_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.empty_stations_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRefreshOnline, enabled = !isSyncing) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.refresh_button))
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier
                        .widthIn(max = 1160.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(stations, key = { it.id }) { station ->
                        StationRowCard(
                            station = station,
                            isPlaying = station.id == currentStationId,
                            onSelect = { onTuneStation(station, stations) },
                            onToggleFavorite = { onToggleFavorite(station) },
                            onQuickAssignPreset = { onRequestAssignPreset(station) },
                            onDeleteCustom = if (station.isCustom) {
                                { onDeleteCustomStation(station.id) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}
