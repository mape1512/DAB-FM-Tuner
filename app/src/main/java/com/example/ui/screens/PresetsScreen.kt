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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.RadioStation
import com.example.ui.components.PresetTile
import com.example.ui.components.StationRowCard

@Composable
fun PresetsScreen(
    presets: List<RadioStation>,
    favorites: List<RadioStation>,
    recentlyPlayed: List<RadioStation>,
    currentStationId: String?,
    onTuneStation: (RadioStation, List<RadioStation>) -> Unit,
    onAssignCurrentToPreset: (Int) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    onRequestAssignPreset: (RadioStation) -> Unit,
    modifier: Modifier = Modifier
) {
    val presetsBySlot = remember(presets) { presets.associateBy { it.presetSlot } }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideMonitor = maxWidth >= 600.dp
        val presetColumns = if (isWideMonitor) 4 else 3
        val stationColumns = if (isWideMonitor) 2 else 1

        LazyVerticalGrid(
            columns = GridCells.Fixed(stationColumns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(stationColumns) }) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 1160.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Stationstasten (P1–P12)",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.presets_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    (1..12).chunked(presetColumns).forEach { rowSlots ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowSlots.forEach { slot ->
                                val station = presetsBySlot[slot]
                                PresetTile(
                                    slotNumber = slot,
                                    station = station,
                                    isCurrent = station != null && station.id == currentStationId,
                                    onTap = {
                                        if (station != null) {
                                            onTuneStation(station, presets)
                                        } else {
                                            onAssignCurrentToPreset(slot)
                                        }
                                    },
                                    onLongPressAssign = { onAssignCurrentToPreset(slot) },
                                    compactHeadUnit = isWideMonitor,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            item(span = { GridItemSpan(stationColumns) }) {
                Text(
                    text = "Favoriten (${favorites.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }

            if (favorites.isEmpty()) {
                item(span = { GridItemSpan(stationColumns) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.empty_favorites_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.empty_favorites_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(favorites, key = { "fav_${it.id}" }) { station ->
                    StationRowCard(
                        station = station,
                        isPlaying = station.id == currentStationId,
                        onSelect = { onTuneStation(station, favorites) },
                        onToggleFavorite = { onToggleFavorite(station) },
                        onQuickAssignPreset = { onRequestAssignPreset(station) }
                    )
                }
            }

            if (recentlyPlayed.isNotEmpty()) {
                item(span = { GridItemSpan(stationColumns) }) {
                    Text(
                        text = "Zuletzt gehört",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
                items(recentlyPlayed, key = { "recent_${it.id}" }) { station ->
                    StationRowCard(
                        station = station,
                        isPlaying = station.id == currentStationId,
                        onSelect = { onTuneStation(station, recentlyPlayed) },
                        onToggleFavorite = { onToggleFavorite(station) },
                        onQuickAssignPreset = { onRequestAssignPreset(station) }
                    )
                }
            }
        }
    }
}
