package com.qppd.ricedryer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_devices")
data class CachedDevice(
    @PrimaryKey
    val deviceId: String,
    val deviceName: String,
    val lastTemperature: Float,
    val lastHumidity: Float,
    val dryingActive: Boolean,
    val lastUpdate: Long,
    val isFavorite: Boolean = false
)

@Entity(tableName = "cached_readings")
data class CachedReading(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceId: String,
    val temperature: Float,
    val humidity: Float,
    val setpointTemp: Float,
    val setpointHumidity: Float,
    val heaterOn: Boolean,
    val fanOn: Boolean,
    val dryingActive: Boolean,
    val timestamp: Long
)
