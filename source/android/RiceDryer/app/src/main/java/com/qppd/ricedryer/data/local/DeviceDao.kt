package com.qppd.ricedryer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM cached_devices ORDER BY isFavorite DESC, lastUpdate DESC")
    fun getAllDevices(): Flow<List<CachedDevice>>
    
    @Query("SELECT * FROM cached_devices WHERE deviceId = :deviceId")
    fun getDevice(deviceId: String): Flow<CachedDevice?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: CachedDevice)
    
    @Query("DELETE FROM cached_devices WHERE deviceId = :deviceId")
    suspend fun deleteDevice(deviceId: String)
    
    @Query("UPDATE cached_devices SET isFavorite = :isFavorite WHERE deviceId = :deviceId")
    suspend fun updateFavorite(deviceId: String, isFavorite: Boolean)
}

@Dao
interface ReadingDao {
    @Query("SELECT * FROM cached_readings WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT :limit")
    fun getReadings(deviceId: String, limit: Int = 100): Flow<List<CachedReading>>
    
    @Query("SELECT * FROM cached_readings WHERE deviceId = :deviceId AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    fun getReadingsInRange(deviceId: String, startTime: Long, endTime: Long): Flow<List<CachedReading>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: CachedReading)
    
    @Query("DELETE FROM cached_readings WHERE deviceId = :deviceId")
    suspend fun deleteReadingsForDevice(deviceId: String)
    
    @Query("DELETE FROM cached_readings WHERE timestamp < :timestamp")
    suspend fun deleteOldReadings(timestamp: Long)
}
