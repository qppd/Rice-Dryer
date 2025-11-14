# ESP32 Rice Dryer - Code Size Optimization Guide

## Changes Made to Reduce Flash Memory Usage

### 1. **Removed ArduinoOTA Library** ✅
- Removed `#include <ArduinoOTA.h>`
- Removed `ArduinoOTA.setHostname()` and `ArduinoOTA.begin()` from setup()
- Removed `ArduinoOTA.handle()` from loop()
- **Savings: ~30-50KB**

### 2. **Removed Test Mode Functions** ✅
- Removed `testDHT22()`, `testPotentiometer()`, `testSSR()`, `testLCD()`
- Removed `runTestMenu()` function
- Removed test mode entry check in setup()
- **Savings: ~5-8KB**

### 3. **Removed Serial Debug Output** ✅
- Removed all `Serial.println()` debug statements
- Removed `Serial.begin(115200)` from setup()
- **Savings: ~10-15KB** (string literals consume significant flash)

### 4. **Simplified WiFiManager** ✅
- Removed custom HTML styling
- Removed verbose connection messages
- **Savings: ~2-3KB**

### 5. **Optimized TemperatureController** ✅
- Removed verbose PID computation logging
- Removed parameter update messages
- **Savings: ~3-5KB**

## Additional Compilation Optimizations for Arduino IDE

### Option 1: Change Partition Scheme (RECOMMENDED)
1. In Arduino IDE, go to **Tools → Partition Scheme**
2. Select one of these options:
   - **"Minimal SPIFFS (1.9MB APP with OTA/190KB SPIFFS)"** - Maximum app space
   - **"No OTA (2MB APP/2MB SPIFFS)"** - If you don't need OTA updates
   - **"Huge APP (3MB No OTA/1MB SPIFFS)"** - Largest app space available

### Option 2: Enable Compiler Optimizations
Add these lines to your `platform.txt` or use Arduino IDE preferences:

**For Arduino IDE 2.x:**
1. Go to **File → Preferences**
2. Under "Compiler warnings", select "None" to reduce code size slightly
3. Note: Full optimization flags require modifying platform.txt

**Manual platform.txt modification** (Advanced):
Location: `C:\Users\<YourUser>\AppData\Local\Arduino15\packages\esp32\hardware\esp32\<version>\platform.txt`

Find the line starting with `compiler.c.flags=` and add:
```
-Os -flto
```

### Option 3: Disable Debug Features in ESP32 Core
In `platform.txt` or via board menu if available:
```
-DCORE_DEBUG_LEVEL=0
```

### Option 4: Use External PSRAM (If your ESP32 has PSRAM)
1. Go to **Tools → PSRAM**
2. Select **"Enabled"**

## Library-Specific Optimizations

### Firebase ESP Client Library
The Firebase library is quite large. If issues persist, consider:

1. **Use minimal Firebase features only:**
   - Only include the addons you need (`TokenHelper.h`, `RTDBHelper.h`)
   - Don't include Cloud Firestore or Cloud Functions if not needed

2. **Partition Scheme Selection:**
   - With Firebase, you MUST use a larger app partition
   - Use "Huge APP" or "Minimal SPIFFS" partition schemes

### WiFiManager Library
Consider using ESP32's built-in WiFi provisioning instead if space is critical, but current implementation is already minimal.

## Build Settings for Arduino IDE

### Recommended Settings:
```
Board: "ESP32 Dev Module" (or your specific ESP32 board)
Upload Speed: 921600
CPU Frequency: 240MHz
Flash Frequency: 80MHz
Flash Mode: QIO
Flash Size: 4MB (or your actual flash size)
Partition Scheme: "Minimal SPIFFS (1.9MB APP)" or "Huge APP (3MB No OTA)"
Core Debug Level: None
PSRAM: Disabled (or Enabled if you have PSRAM)
```

## Verification

After making these changes, compile and check the memory usage:
```
Sketch uses XXXXX bytes (XX%) of program storage space. Maximum is XXXXXX bytes.
Global variables use XXXXX bytes (XX%) of dynamic memory.
```

You should see a reduction of **50-80KB** in program storage compared to the original code.

## What Firebase Features are Preserved?

✅ **All core Firebase functionality remains:**
- Real-time Database read/write operations
- Device registration and pairing
- Remote command execution
- Historical data logging
- Connection status monitoring
- Automatic reconnection

❌ **Removed features:**
- Over-the-Air (OTA) firmware updates
- Hardware test mode menu
- Serial debug output
- Custom WiFi portal styling

## If Still Too Large

If the code is still too large after these optimizations:

1. **Remove historical data logging** - Comment out `logHistoricalData()` calls (~5KB savings)
2. **Reduce Firebase update frequency** - Increase `FIREBASE_UPDATE_INTERVAL` and `HISTORY_LOG_INTERVAL`
3. **Remove pairing code system** - If you can hardcode device registration (~8KB savings)
4. **Simplify LCD messages** - Use shorter strings throughout
5. **Consider ESP32 with more flash** - Use ESP32 with 8MB or 16MB flash

## Testing After Optimization

1. Verify WiFi connection works
2. Test Firebase connectivity
3. Confirm device pairing works
4. Test temperature control and relay operation
5. Verify remote commands work from app
6. Check data logging to Firebase

## Questions or Issues?

If compilation still fails:
1. Check which partition scheme you're using
2. Verify ESP32 board has sufficient flash (minimum 4MB recommended)
3. Consider using PlatformIO instead of Arduino IDE for better optimization control
