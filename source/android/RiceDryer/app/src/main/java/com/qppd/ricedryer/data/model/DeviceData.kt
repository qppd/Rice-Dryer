package com.qppd.ricedryer.data.model

data class DeviceData(
    val deviceId: String = "",
    val deviceInfo: DeviceInfo = DeviceInfo(),
    val status: DeviceStatus = DeviceStatus(),
    val settings: DeviceSettings = DeviceSettings(),
    val commands: DeviceCommands = DeviceCommands(),
    val history: Map<String, SensorReading> = emptyMap()
)

data class DeviceInfo(
    val macAddress: String = "",
    val firmwareVersion: String = "",
    val hardwareVersion: String = "",
    val lastBoot: Long = 0,
    val pairedTo: String = "",
    val deviceName: String = "Rice Dryer",
    val pairingCode: String = "",
    val pairingCodeExpiry: Long = 0
)

data class DeviceStatus(
    val temperature: Float = 0f,
    val humidity: Float = 0f,
    val setpointTemp: Float = 40f,
    val setpointHumidity: Float = 20f,
    val dryingActive: Boolean = false,
    val heaterOn: Boolean = false,
    val fanOn: Boolean = false,
    val wifiConnected: Boolean = false,
    val firebaseConnected: Boolean = false,
    val lastUpdate: Long = 0,
    val errorMessage: String = ""
)

data class DeviceSettings(
    val autoStop: Boolean = true,
    val maxTemp: Float = 80f,
    val minTemp: Float = 30f,
    val maxHumidity: Float = 50f,
    val minHumidity: Float = 10f,
    val tempUnit: String = "C" // C or F
)

data class DeviceCommands(
    val command: String = "",
    val value: Float = 0f,
    val timestamp: Long = 0,
    val processed: Boolean = false
)

data class SensorReading(
    val temperature: Float = 0f,
    val humidity: Float = 0f,
    val setpointTemp: Float = 40f,
    val setpointHumidity: Float = 20f,
    val relay1Status: Boolean = false,  // ESP32 field name for heater
    val relay2Status: Boolean = false,  // ESP32 field name for fan
    val dryingActive: Boolean = false,
    val timestamp: Long = 0
) {
    // Convenience properties for backward compatibility
    val heaterOn: Boolean get() = relay1Status
    val fanOn: Boolean get() = relay2Status
}
