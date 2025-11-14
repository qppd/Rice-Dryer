package com.qppd.ricedryer.data.repository

import com.google.firebase.database.*
import com.qppd.ricedryer.data.local.AppDatabase
import com.qppd.ricedryer.data.local.CachedDevice
import com.qppd.ricedryer.data.local.CachedReading
import com.qppd.ricedryer.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import android.util.Log

class DeviceRepository(private val database: AppDatabase) {
    private val firebaseDatabase = FirebaseDatabase.getInstance()
    private val devicesRef = firebaseDatabase.getReference("devices")
    private val usersRef = firebaseDatabase.getReference("users")
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Get user's devices from Firebase
    fun getUserDevices(userId: String): Flow<List<String>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val devices = snapshot.child("devices").children.mapNotNull { it.value as? String }
                trySend(devices)
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("DeviceRepository", "Error getting user devices: ${error.message}")
                close(error.toException())
            }
        }
        
        val ref = usersRef.child(userId)
        ref.addValueEventListener(listener)
        
        awaitClose { ref.removeEventListener(listener) }
    }
    
    // Get device data from Firebase with real-time updates
    fun getDeviceData(deviceId: String): Flow<DeviceData?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    // ESP32 writes to /devices/{deviceId}/current, not /status
                    val currentSnapshot = snapshot.child("current")
                    
                    // Map ESP32 field names to Android model
                    val status = DeviceStatus(
                        temperature = currentSnapshot.child("temperature").getValue(Float::class.java) ?: 0f,
                        humidity = currentSnapshot.child("humidity").getValue(Float::class.java) ?: 0f,
                        setpointTemp = currentSnapshot.child("setpointTemp").getValue(Float::class.java) ?: 40f,
                        setpointHumidity = currentSnapshot.child("setpointHumidity").getValue(Float::class.java) ?: 20f,
                        dryingActive = currentSnapshot.child("dryingActive").getValue(Boolean::class.java) ?: false,
                        heaterOn = currentSnapshot.child("relay1Status").getValue(Boolean::class.java) ?: false,
                        fanOn = currentSnapshot.child("relay2Status").getValue(Boolean::class.java) ?: false,
                        wifiConnected = currentSnapshot.child("online").getValue(Boolean::class.java) ?: false,
                        firebaseConnected = currentSnapshot.child("online").getValue(Boolean::class.java) ?: false,
                        lastUpdate = currentSnapshot.child("lastUpdate").getValue(Long::class.java) ?: 0L,
                        errorMessage = ""
                    )
                    
                    val deviceData = DeviceData(
                        deviceId = deviceId,
                        deviceInfo = snapshot.child("deviceInfo").getValue(DeviceInfo::class.java) ?: DeviceInfo(),
                        status = status,
                        settings = snapshot.child("settings").getValue(DeviceSettings::class.java) ?: DeviceSettings(),
                        commands = snapshot.child("commands").getValue(DeviceCommands::class.java) ?: DeviceCommands()
                    )
                    trySend(deviceData)
                    
                    // Cache to local database
                    repositoryScope.launch {
                        cacheDeviceData(deviceData)
                    }
                } catch (e: Exception) {
                    Log.e("DeviceRepository", "Error parsing device data: ${e.message}")
                    trySend(null)
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("DeviceRepository", "Error getting device data: ${error.message}")
                close(error.toException())
            }
        }
        
        val ref = devicesRef.child(deviceId)
        ref.addValueEventListener(listener)
        
        awaitClose { ref.removeEventListener(listener) }
    }
    
    // Get device history readings
    fun getDeviceHistory(deviceId: String, limit: Int = 100): Flow<List<SensorReading>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val readings = snapshot.children.mapNotNull { childSnapshot ->
                    try {
                        // Parse the sensor reading data
                        val temperature = childSnapshot.child("temperature").getValue(Float::class.java) ?: 0f
                        val humidity = childSnapshot.child("humidity").getValue(Float::class.java) ?: 0f
                        val setpointTemp = childSnapshot.child("setpointTemp").getValue(Float::class.java) ?: 40f
                        val setpointHumidity = childSnapshot.child("setpointHumidity").getValue(Float::class.java) ?: 20f
                        val relay1Status = childSnapshot.child("relay1Status").getValue(Boolean::class.java) ?: false
                        val relay2Status = childSnapshot.child("relay2Status").getValue(Boolean::class.java) ?: false
                        val dryingActive = childSnapshot.child("dryingActive").getValue(Boolean::class.java) ?: false
                        
                        // ESP32 now includes timestamp field with NTP time
                        val timestamp = childSnapshot.child("timestamp").getValue(Long::class.java) 
                            ?: childSnapshot.key?.toLongOrNull()?.times(1000) // Fallback: key is in seconds
                            ?: 0L
                        
                        SensorReading(
                            temperature = temperature,
                            humidity = humidity,
                            setpointTemp = setpointTemp,
                            setpointHumidity = setpointHumidity,
                            relay1Status = relay1Status,
                            relay2Status = relay2Status,
                            dryingActive = dryingActive,
                            timestamp = timestamp
                        )
                    } catch (e: Exception) {
                        Log.e("DeviceRepository", "Error parsing history entry: ${e.message}")
                        null
                    }
                }.sortedByDescending { it.timestamp }.take(limit)
                
                trySend(readings)
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("DeviceRepository", "Error getting device history: ${error.message}")
                close(error.toException())
            }
        }
        
        val ref = devicesRef.child(deviceId).child("history").limitToLast(limit)
        ref.addValueEventListener(listener)
        
        awaitClose { ref.removeEventListener(listener) }
    }
    
    // Send command to device
    suspend fun sendCommand(deviceId: String, command: String, value: Float = 0f): Result<Unit> {
        return try {
            // ESP32 expects "action" field, not "command"
            val commandData = mapOf(
                "action" to command,
                "value" to value,
                "timestamp" to System.currentTimeMillis(),
                "processed" to false
            )
            
            devicesRef.child(deviceId).child("commands").setValue(commandData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DeviceRepository", "Error sending command: ${e.message}")
            Result.failure(e)
        }
    }
    
    // Pair device with user
    suspend fun pairDevice(userId: String, deviceId: String, pairingCode: String): Result<DeviceInfo> {
        return try {
            // Check if pairing code exists in /devicePairing/{code}
            val pairingRef = firebaseDatabase.getReference("devicePairing").child(pairingCode)
            val pairingSnapshot = pairingRef.get().await()
            
            if (!pairingSnapshot.exists()) {
                throw Exception("Invalid pairing code")
            }
            
            val pairingDeviceId = pairingSnapshot.child("deviceId").value as? String
            val used = pairingSnapshot.child("used").value as? Boolean ?: false
            
            // Verify the pairing code matches the device
            if (pairingDeviceId != deviceId) {
                throw Exception("Pairing code does not match device ID")
            }
            
            // Check if pairing code has already been used
            if (used) {
                throw Exception("Pairing code already used")
            }
            
            // ESP32 now uses NTP timestamps - check if code is expired
            val expiresAt = pairingSnapshot.child("expiresAt").value as? Long ?: 0L
            if (expiresAt > 0 && System.currentTimeMillis() > expiresAt) {
                throw Exception("Pairing code expired")
            }
            
            // Get device info
            val deviceSnapshot = devicesRef.child(deviceId).child("deviceInfo").get().await()
            val deviceInfo = deviceSnapshot.getValue(DeviceInfo::class.java)
                ?: throw Exception("Device not found")
            
            // Check if device is already paired
            if (deviceInfo.pairedTo.isNotEmpty() && deviceInfo.pairedTo != "null") {
                throw Exception("Device already paired")
            }
            
            // Pair device
            devicesRef.child(deviceId).child("deviceInfo/pairedTo").setValue(userId).await()
            
            // Mark pairing code as used
            pairingRef.child("used").setValue(true).await()
            pairingRef.child("pairedTo").setValue(userId).await()
            pairingRef.child("pairedAt").setValue(System.currentTimeMillis()).await()
            
            // Add device to user's device list
            val userDevicesRef = usersRef.child(userId).child("devices")
            val currentDevices = userDevicesRef.get().await().children.mapNotNull { it.value as? String }
            if (!currentDevices.contains(deviceId)) {
                userDevicesRef.child(currentDevices.size.toString()).setValue(deviceId).await()
            }
            
            Result.success(deviceInfo)
        } catch (e: Exception) {
            Log.e("DeviceRepository", "Error pairing device: ${e.message}")
            Result.failure(e)
        }
    }
    
    // Unpair device
    suspend fun unpairDevice(userId: String, deviceId: String): Result<Unit> {
        return try {
            // Remove pairing from device
            devicesRef.child(deviceId).child("deviceInfo/pairedTo").removeValue().await()
            
            // Remove device from user's list
            val userDevicesRef = usersRef.child(userId).child("devices")
            val snapshot = userDevicesRef.get().await()
            snapshot.children.forEach { child ->
                if (child.value == deviceId) {
                    child.ref.removeValue().await()
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DeviceRepository", "Error unpairing device: ${e.message}")
            Result.failure(e)
        }
    }
    
    // Update device name
    suspend fun updateDeviceName(deviceId: String, name: String): Result<Unit> {
        return try {
            devicesRef.child(deviceId).child("deviceInfo/deviceName").setValue(name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DeviceRepository", "Error updating device name: ${e.message}")
            Result.failure(e)
        }
    }
    
    // Cache device data locally
    private suspend fun cacheDeviceData(deviceData: DeviceData) {
        try {
            val cachedDevice = CachedDevice(
                deviceId = deviceData.deviceId,
                deviceName = deviceData.deviceInfo.deviceName,
                lastTemperature = deviceData.status.temperature,
                lastHumidity = deviceData.status.humidity,
                dryingActive = deviceData.status.dryingActive,
                lastUpdate = deviceData.status.lastUpdate
            )
            database.deviceDao().insertDevice(cachedDevice)
            
            // Cache current reading
            val cachedReading = CachedReading(
                deviceId = deviceData.deviceId,
                temperature = deviceData.status.temperature,
                humidity = deviceData.status.humidity,
                setpointTemp = deviceData.status.setpointTemp,
                setpointHumidity = deviceData.status.setpointHumidity,
                heaterOn = deviceData.status.heaterOn,
                fanOn = deviceData.status.fanOn,
                dryingActive = deviceData.status.dryingActive,
                timestamp = System.currentTimeMillis()
            )
            database.readingDao().insertReading(cachedReading)
        } catch (e: Exception) {
            Log.e("DeviceRepository", "Error caching device data: ${e.message}")
        }
    }
    
    // Get cached devices (offline support)
    fun getCachedDevices(): Flow<List<CachedDevice>> {
        return database.deviceDao().getAllDevices()
    }
    
    // Get cached readings (offline support)
    fun getCachedReadings(deviceId: String, limit: Int = 100): Flow<List<CachedReading>> {
        return database.readingDao().getReadings(deviceId, limit)
    }
}
