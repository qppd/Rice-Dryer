# Firebase Database Path Alignment

## Fixed Issues (November 14, 2025)

### 1. Status Data Path
**Problem:** ESP32 writes to `/devices/{deviceId}/current` but Android was reading from `/devices/{deviceId}/status`

**Solution:** Updated `DeviceRepository.getDeviceData()` to read from `/current` node

### 2. Field Name Mismatches
**Problem:** ESP32 and Android used different field names

**ESP32 Fields → Android Fields:**
- `relay1Status` → `heaterOn`
- `relay2Status` → `fanOn`
- `online` → `wifiConnected` and `firebaseConnected`

**Solution:** Added manual field mapping in `DeviceRepository.getDeviceData()`

### 3. Command Field Name
**Problem:** ESP32 expects `"action"` field but Android was sending `"command"`

**Solution:** Changed `DeviceRepository.sendCommand()` to use `"action"` field

### 4. History Data Structure
**Problem:** SensorReading model used `heaterOn`/`fanOn` but ESP32 writes `relay1Status`/`relay2Status`

**Solution:** Updated `SensorReading` model to use ESP32 field names with backward-compatible properties

---

## Current Firebase Database Structure

### Device Registration & Info
```
/devices/{deviceId}/deviceInfo/
├── macAddress: String
├── firmwareVersion: String
├── hardwareVersion: String
├── lastBoot: Long (millis)
├── pairedTo: String (userId)
├── deviceName: String
├── pairingCode: String
└── pairingCodeExpiry: Long (millis)
```

### Real-time Status (Current Data)
```
/devices/{deviceId}/current/
├── temperature: Float
├── humidity: Float
├── setpointTemp: Float
├── setpointHumidity: Float
├── relay1Status: Boolean (heater)
├── relay2Status: Boolean (fan)
├── dryingActive: Boolean
├── currentMode: Int
├── pidOutput: Float
├── online: Boolean
└── lastUpdate: Long (millis)
```

### Commands (Android → ESP32)
```
/devices/{deviceId}/commands/
├── action: String ("START", "STOP", "SET_TEMP", "SET_HUMIDITY")
├── value: Float (for SET_TEMP and SET_HUMIDITY)
├── timestamp: Long
└── processed: Boolean
```

### Command Acknowledgment (ESP32 → Android)
```
/devices/{deviceId}/commandAck/
├── command: String
├── timestamp: Long
└── acknowledged: Boolean
```

### Historical Data
```
/devices/{deviceId}/history/{timestamp}/
├── temperature: Float
├── humidity: Float
├── setpointTemp: Float
├── setpointHumidity: Float
├── relay1Status: Boolean
├── relay2Status: Boolean
├── dryingActive: Boolean
└── pidOutput: Float
```

### User Device Association
```
/users/{userId}/devices: [deviceId1, deviceId2, ...]
```

### Device Pairing
```
/devicePairing/{pairingCode}/
├── deviceId: String
├── generatedAt: Long (millis)
├── expiresAt: Long (millis)
└── used: Boolean
```

---

## Command Action Types

| Action | Description | Value Field |
|--------|-------------|-------------|
| `START` | Start drying process | Not used |
| `STOP` | Stop drying process | Not used |
| `SET_TEMP` | Update target temperature | Temperature in °C |
| `SET_HUMIDITY` | Update target humidity | Humidity in % |

---

## Notes

1. **Timestamp Format:** ESP32 uses `millis()` (time since boot in milliseconds), not Unix timestamps. Android converts to Unix timestamps for display.

2. **Connection Status:** ESP32 sets `online: true` when connected. Android maps this to both `wifiConnected` and `firebaseConnected`.

3. **Relay Mapping:**
   - `relay1Status` = Heater/SSR1
   - `relay2Status` = Fan/SSR2

4. **Command Flow:**
   - Android writes to `/devices/{deviceId}/commands`
   - ESP32 reads, processes, and deletes the command
   - ESP32 writes acknowledgment to `/devices/{deviceId}/commandAck`

5. **Pairing Code Expiry:** Validation is handled by ESP32 since it uses `millis()`. Android skips expiry check during pairing.
