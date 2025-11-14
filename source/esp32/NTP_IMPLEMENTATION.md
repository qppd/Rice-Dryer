# NTP Time Synchronization Update

## Changes Made (November 14, 2025)

### ESP32 Updates

#### 1. Added NTP Initialization
- **Location:** `RiceDryer.ino` - `initNTP()` function
- **Configuration:** 
  - Timezone: UTC+8 (Philippines - no DST)
  - NTP Servers: `time.google.com`, `pool.ntp.org`, `time.cloudflare.com`
- **Features:**
  - Auto-syncs time on WiFi connection
  - LCD displays sync status and current time
  - Fallback to `millis()` if NTP fails

#### 2. Added `getTimestamp()` Function
- Returns Unix timestamp in milliseconds
- Falls back to `millis()` if NTP hasn't synced
- Type: `unsigned long long` (64-bit) for large timestamp values

#### 3. Updated All Firebase Writes
- **`sendDataToFirebase()`**: `lastUpdate` now uses NTP timestamp
- **`logHistoricalData()`**: 
  - Key: Unix timestamp in seconds (for better Firebase sorting)
  - Data: Includes `timestamp` field with Unix milliseconds
- **`registerDevice()`**: `lastBoot` uses NTP timestamp
- **`startPairingMode()`**: 
  - Added `generatedAt` field
  - `expiresAt` uses NTP timestamp
- **`acknowledgeCommand()`**: Uses NTP timestamp

#### 4. Updated Pairing Code Expiry
- Changed `pairingCodeExpiry` type to `unsigned long long`
- Now uses real Unix timestamps for validation
- Properly expires after 10 minutes (600,000 ms)

### Android App Updates

#### 1. History Data Parsing
- Now reads `timestamp` field from history data
- Fallback: If no timestamp field, uses key * 1000 (seconds to milliseconds)
- Properly handles Unix timestamps

#### 2. Time Filtering
- `ChartsViewModel.getFilteredReadings()`: Now uses real Unix timestamps
- Time range filtering works correctly with `System.currentTimeMillis()`

#### 3. Pairing Code Validation
- Re-enabled expiry check now that ESP32 uses real timestamps
- Validates `expiresAt` against current time

#### 4. Chart Display
- X-axis now shows actual time (HH:mm format)
- Properly formats Unix timestamps to readable time

---

## Firebase Database Structure (Updated)

### Current Status Data
```json
{
  "devices": {
    "{deviceId}": {
      "current": {
        "lastUpdate": 1700000000000  // Unix timestamp in milliseconds
      }
    }
  }
}
```

### Historical Data
```json
{
  "devices": {
    "{deviceId}": {
      "history": {
        "1700000000": {  // Key: Unix timestamp in seconds
          "temperature": 45.5,
          "humidity": 25.3,
          "timestamp": 1700000000000  // Unix timestamp in milliseconds
        }
      }
    }
  }
}
```

### Device Pairing
```json
{
  "devicePairing": {
    "{code}": {
      "deviceId": "AABBCCDDEEFF",
      "generatedAt": 1700000000000,  // Unix timestamp in milliseconds
      "expiresAt": 1700000600000,    // generatedAt + 10 minutes
      "used": false
    }
  }
}
```

---

## Benefits

1. **Accurate Timestamps**: Real wall-clock time instead of relative time since boot
2. **Proper Time Filtering**: Charts can show "Last Hour", "Last 6 Hours", etc. correctly
3. **Cross-Device Sync**: Multiple devices show consistent times
4. **Pairing Expiry**: 10-minute pairing code expiry works correctly
5. **Data Persistence**: Historical data timestamps remain valid after device reboot
6. **Timezone Support**: Displays time in Philippines timezone (UTC+8)

---

## Notes

- **NTP Sync Time**: Takes 1-10 seconds after WiFi connection
- **Fallback Behavior**: If NTP fails, device uses `millis()` (time since boot)
- **Timestamp Format**: All timestamps are Unix time in milliseconds (Long/unsigned long long)
- **Firebase Keys**: History uses seconds for keys (smaller values), milliseconds in data
- **Timezone**: Hardcoded to Philippines (UTC+8), no daylight saving time

---

## Testing Checklist

- [ ] Verify NTP sync message on LCD after WiFi connects
- [ ] Check that `lastUpdate` timestamps are recent Unix time (not millis)
- [ ] Confirm pairing codes expire after 10 minutes
- [ ] Test history charts show correct time labels (HH:mm format)
- [ ] Verify time range filters work (Last Hour, Last 6 Hours, etc.)
- [ ] Check device list "Last Update" shows proper time ago
- [ ] Confirm historical data survives device reboot with correct timestamps
