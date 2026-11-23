package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.RadioBand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.radioDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "autotuner_prefs"
)

class RadioPreferences(private val context: Context) {

    private object Keys {
        val DARK_COCKPIT = booleanPreferencesKey("dark_cockpit")
        val SELECTED_COUNTRY = stringPreferencesKey("selected_country")
        val SELECTED_BAND = stringPreferencesKey("selected_band")
        val LAST_STATION_ID = stringPreferencesKey("last_station_id")
        val DRIVING_MODE = booleanPreferencesKey("driving_mode")
    }

    val isDarkCockpit: Flow<Boolean> = context.radioDataStore.data.map { prefs ->
        prefs[Keys.DARK_COCKPIT] ?: true
    }

    val selectedCountry: Flow<String> = context.radioDataStore.data.map { prefs ->
        prefs[Keys.SELECTED_COUNTRY] ?: "DE"
    }

    val selectedBand: Flow<RadioBand> = context.radioDataStore.data.map { prefs ->
        RadioBand.fromStored(prefs[Keys.SELECTED_BAND])
    }

    val lastStationId: Flow<String?> = context.radioDataStore.data.map { prefs ->
        prefs[Keys.LAST_STATION_ID]
    }

    val isDrivingMode: Flow<Boolean> = context.radioDataStore.data.map { prefs ->
        prefs[Keys.DRIVING_MODE] ?: false
    }

    suspend fun setDarkCockpit(enabled: Boolean) {
        context.radioDataStore.edit { it[Keys.DARK_COCKPIT] = enabled }
    }

    suspend fun setSelectedCountry(countryCode: String) {
        context.radioDataStore.edit { it[Keys.SELECTED_COUNTRY] = countryCode }
    }

    suspend fun setSelectedBand(band: RadioBand) {
        context.radioDataStore.edit { it[Keys.SELECTED_BAND] = band.name }
    }

    suspend fun setLastStationId(stationId: String) {
        context.radioDataStore.edit { it[Keys.LAST_STATION_ID] = stationId }
    }

    suspend fun setDrivingMode(enabled: Boolean) {
        context.radioDataStore.edit { it[Keys.DRIVING_MODE] = enabled }
    }
}
