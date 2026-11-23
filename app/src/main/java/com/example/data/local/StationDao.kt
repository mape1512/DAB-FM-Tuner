package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface StationDao {

    @Query("SELECT * FROM stations ORDER BY isFavorite DESC, votes DESC, name ASC")
    fun observeAllStations(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE isFavorite = 1 ORDER BY name ASC")
    fun observeFavorites(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE presetSlot IS NOT NULL ORDER BY presetSlot ASC")
    fun observePresets(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE lastPlayedAt > 0 ORDER BY lastPlayedAt DESC LIMIT 10")
    fun observeRecentlyPlayed(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations")
    suspend fun getAllStationsOnce(): List<StationEntity>

    @Query("SELECT * FROM stations WHERE id = :id LIMIT 1")
    suspend fun getStationById(id: String): StationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStations(stations: List<StationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStation(station: StationEntity)

    @Query("UPDATE stations SET isFavorite = :isFavorite WHERE id = :stationId")
    suspend fun setFavorite(stationId: String, isFavorite: Boolean)

    @Query("UPDATE stations SET presetSlot = NULL WHERE presetSlot = :slot")
    suspend fun clearPresetSlot(slot: Int)

    @Query("UPDATE stations SET presetSlot = :slot WHERE id = :stationId")
    suspend fun setPresetSlot(stationId: String, slot: Int)

    @Transaction
    suspend fun assignPreset(stationId: String, slot: Int) {
        clearPresetSlot(slot)
        setPresetSlot(stationId, slot)
    }

    @Query("UPDATE stations SET lastPlayedAt = :timestamp WHERE id = :stationId")
    suspend fun updateLastPlayed(stationId: String, timestamp: Long)

    @Query("UPDATE stations SET faviconUrl = :faviconUrl WHERE id = :stationId")
    suspend fun updateFaviconUrl(stationId: String, faviconUrl: String)

    @Query("DELETE FROM stations WHERE id = :stationId AND isCustom = 1")
    suspend fun deleteCustomStation(stationId: String)
}
