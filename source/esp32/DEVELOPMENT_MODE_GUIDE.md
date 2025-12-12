# RiceDryer ESP32 - Development Mode Guide

## Overview

The RiceDryer firmware now supports two compile-time operation modes:

- **🔧 DEVELOPMENT_MODE**: Isolated hardware testing without WiFi/Firebase
- **🚀 PRODUCTION_MODE**: Full production firmware with all features

---

## How to Switch Modes

Open `RiceDryer.ino` and locate line 28:

```cpp
//#define DEVELOPMENT_MODE  // Uncomment for testing, comment for production
```

### Enable Development Mode
**Uncomment** the line:
```cpp
#define DEVELOPMENT_MODE  // Uncomment for testing, comment for production
```

### Enable Production Mode (Default)
**Comment** the line or leave it as is:
```cpp
//#define DEVELOPMENT_MODE  // Uncomment for testing, comment for production
```

---

## 🔧 DEVELOPMENT MODE

### Purpose
Testing individual hardware components without requiring:
- WiFi connection
- Firebase configuration
- Android app pairing
- Production control logic

### What's Enabled
✅ **LCD Display** (20x4 I2C, address 0x27)  
✅ **DHT22 Sensor** (GPIO 23)  
✅ **Heater Relay** (GPIO 19, bistable, trigger LOW)  
✅ **Blower Relay** (GPIO 18, bistable, trigger LOW)  
✅ **Serial Command Interface** (115200 baud)

### What's Disabled
❌ WiFi & Captive Portal  
❌ Firebase & Pairing System  
❌ PID Temperature Control  
❌ Drying Start/Stop Logic  
❌ 3-Button UI Modes  
❌ Remote Commands  
❌ Historical Logging  
❌ NTP Time Sync

### Usage

1. **Enable Development Mode** in the firmware
2. **Upload** to ESP32
3. **Open Serial Monitor** at **115200 baud**
4. **Type commands** to test hardware

### Available Commands

| Command | Description | Example |
|---------|-------------|---------|
| `HELP` | Show all available commands | `HELP` |
| `STATUS` | Show current hardware status | `STATUS` |
| `LCD_TEST` | Test all LCD lines (1-4) | `LCD_TEST` |
| `LCD_CLEAR` | Clear the LCD display | `LCD_CLEAR` |
| `LCD_PRINT:<text>` | Print custom text on LCD | `LCD_PRINT:Hello World` |
| `DHT_READ` | Read temperature & humidity | `DHT_READ` |
| `RELAY_HEATER_ON` | Turn heater relay ON (GPIO 19 → LOW) | `RELAY_HEATER_ON` |
| `RELAY_HEATER_OFF` | Turn heater relay OFF (GPIO 19 → HIGH) | `RELAY_HEATER_OFF` |
| `RELAY_BLOWER_ON` | Turn blower relay ON (GPIO 18 → LOW) | `RELAY_BLOWER_ON` |
| `RELAY_BLOWER_OFF` | Turn blower relay OFF (GPIO 18 → HIGH) | `RELAY_BLOWER_OFF` |

### Example Test Session

```
=================================
  DEVELOPMENT MODE ACTIVATED
=================================
WiFi, Firebase, and production
logic are DISABLED.

Type HELP for available commands.
=================================

> HELP
=== DEVELOPMENT MODE COMMANDS ===
LCD_TEST          - Test all LCD lines
LCD_CLEAR         - Clear LCD display
LCD_PRINT:<text>  - Print text on LCD
DHT_READ          - Read DHT22 sensor
RELAY_HEATER_ON   - Turn heater relay ON (GPIO 19)
RELAY_HEATER_OFF  - Turn heater relay OFF
RELAY_BLOWER_ON   - Turn blower relay ON (GPIO 18)
RELAY_BLOWER_OFF  - Turn blower relay OFF
STATUS            - Show current hardware status
HELP              - Show this help message
=================================

> LCD_TEST
LCD test executed

> DHT_READ
Temperature: 28.3 C, Humidity: 65.2 %

> RELAY_HEATER_ON
Heater relay (GPIO 19) turned ON

> RELAY_HEATER_OFF
Heater relay (GPIO 19) turned OFF

> STATUS
=== DEVELOPMENT MODE STATUS ===
Heater Relay (GPIO 19): OFF
Blower Relay (GPIO 18): OFF
Temperature: 28.3 C
Humidity: 65.2 %
```

### LCD Display in Development Mode

When development mode starts, the LCD shows:
```
DEVELOPMENT MODE
Hardware Test
Open Serial @115200
Type HELP
```

Then the LCD updates based on commands:
- `LCD_TEST` → Shows test pattern
- `DHT_READ` → Shows sensor readings
- `RELAY_*_ON/OFF` → Shows relay status
- `LCD_PRINT:<text>` → Shows custom text

---

## 🚀 PRODUCTION MODE (Default)

### Purpose
Full production firmware with all features enabled for actual rice drying operations.

### What's Enabled
✅ **WiFi Manager** with captive portal  
✅ **Firebase Real-time Database** sync  
✅ **Device Pairing** system  
✅ **PID Temperature Control**  
✅ **Auto-stop** at target humidity  
✅ **3-Button UI** (Normal/Set Temp/Set Humidity)  
✅ **Remote Commands** from Android app  
✅ **Historical Data Logging** (30-second intervals)  
✅ **NTP Time Synchronization** (Philippines UTC+8)  
✅ **Safety Logic** & sensor validation

### Features

#### Hardware Control
- **Heater Relay (GPIO 19)**: PID-controlled heating
- **Blower Relay (GPIO 18)**: Continuous air circulation
- **DHT22 Sensor (GPIO 23)**: Temperature & humidity monitoring
- **LCD Display (20x4)**: Real-time status

#### Button Functions
- **Button 1 (GPIO 17)**: Toggle mode (Normal → Set Temp → Set Humidity)
- **Button 2 (GPIO 16)**: 
  - Normal Mode: Start/Stop drying
  - Setting Modes: Increase setpoint
- **Button 3 (GPIO 4)**:
  - Normal Mode: Factory reset (hold 5s)
  - Setting Modes: Decrease setpoint

#### UI Modes
1. **Normal Mode**: View current status, start/stop drying
2. **Set Temperature Mode**: Adjust target temperature (30-80°C, 1°C steps)
3. **Set Humidity Mode**: Adjust target humidity (10-50%, 1% steps)

#### Firebase Paths
- `/devices/{deviceId}/current` - Real-time status (5s interval)
- `/devices/{deviceId}/history/{timestamp}` - Historical logs (30s interval)
- `/devices/{deviceId}/commands` - Remote commands from app
- `/devices/{deviceId}/deviceInfo` - Device metadata & pairing
- `/devicePairing/{pairingCode}` - Temporary pairing codes (10-min expiry)

#### Pairing Process
1. Device generates 6-digit code
2. Code displayed on LCD with MAC address
3. User enters code in Android app
4. Device paired and ready for use
5. Pairing persists across reboots

---

## Compilation & Flash Size

### Development Mode
- **Smaller flash size** (WiFi/Firebase libraries not linked)
- **Faster compilation**
- **No credentials needed**

### Production Mode
- **Larger flash size** (full feature set)
- **Requires valid `FirebaseConfig.cpp`**
- **Requires WiFi credentials** (configured via captive portal)

---

## Hardware Pin Configuration

### Digital Pins
| Pin | Component | Direction | Notes |
|-----|-----------|-----------|-------|
| GPIO 17 | Button 1 | INPUT_PULLUP | Mode toggle |
| GPIO 16 | Button 2 | INPUT_PULLUP | Start/Stop or Increase |
| GPIO 4 | Button 3 | INPUT_PULLUP | Factory reset or Decrease |
| GPIO 19 | Heater Relay | OUTPUT | Bistable, trigger LOW |
| GPIO 18 | Blower Relay | OUTPUT | Bistable, trigger LOW |
| GPIO 23 | DHT22 Sensor | INPUT/OUTPUT | Data line |

### I2C (LCD)
| Pin | Component | Protocol |
|-----|-----------|----------|
| GPIO 21 | SDA | I2C |
| GPIO 22 | SCL | I2C |

**LCD Address**: 0x27 (20x4 display)

---

## Troubleshooting

### Development Mode Issues

#### "Sensor Error!" on LCD
- Check DHT22 wiring (VCC, GND, Data to GPIO 23)
- Verify DHT22 is not faulty
- Check for loose connections

#### Relays Not Switching
- Verify bistable relay control board connections
- GPIO 19 and GPIO 18 should pulse LOW to trigger
- Check relay power supply (usually 5V or 12V)

#### LCD Not Displaying
- Verify I2C address is 0x27 (check with I2C scanner)
- Check SDA (GPIO 21) and SCL (GPIO 22) connections
- Verify LCD backpack has power (usually 5V)

### Production Mode Issues

#### WiFi Not Connecting
- Press Button 3 for 5 seconds to factory reset
- Connect to "RiceDryer_Setup" AP
- Enter WiFi credentials via captive portal

#### Firebase Connection Failed
- Verify `FirebaseConfig.cpp` has correct credentials
- Check internet connectivity
- Verify Firebase Realtime Database rules allow read/write

#### Device Not Pairing
- Pairing code expires after 10 minutes
- Code regenerates automatically
- Ensure Android app is using correct Firebase project

---

## Safety Notes

⚠️ **Important Safety Considerations**:

1. **High Voltage**: Relays control AC heater elements - use proper electrical safety
2. **Temperature Limits**: PID setpoint range is 30-80°C (configurable)
3. **Sensor Validation**: System stops if DHT22 returns NaN (not-a-number)
4. **Auto-Stop**: Drying stops when target humidity is reached
5. **Factory Reset**: Hold Button 3 for 5s to clear WiFi + pairing data

---

## Migration Between Modes

### From Development to Production
1. Comment `#define DEVELOPMENT_MODE`
2. Upload firmware
3. Device will request WiFi setup
4. Connect to captive portal and configure WiFi
5. Device will register with Firebase and generate pairing code

### From Production to Development
1. Uncomment `#define DEVELOPMENT_MODE`
2. Upload firmware
3. All production features disabled
4. Serial commands active immediately

---

## Code Structure

```
RiceDryer.ino
├── Mode Configuration (#define DEVELOPMENT_MODE)
│
├── Conditional Includes
│   ├── WiFi, Firebase (PRODUCTION only)
│   └── Component headers (both modes)
│
├── Development Mode Code (#ifdef DEVELOPMENT_MODE)
│   ├── devDHT, devHeater, devBlower, devLCD
│   ├── handleDevelopmentSerialCommands()
│   └── runDevelopmentMode()
│
├── Production Mode Code (#else)
│   ├── All Firebase/WiFi variables
│   ├── All helper functions
│   ├── productionSetup()
│   └── productionLoop()
│
└── Main Entry Points (setup & loop)
    └── Dispatcher based on mode
```

---

## Summary

| Feature | Development | Production |
|---------|-------------|------------|
| WiFi | ❌ | ✅ |
| Firebase | ❌ | ✅ |
| Pairing | ❌ | ✅ |
| LCD | ✅ | ✅ |
| DHT22 | ✅ | ✅ |
| Relays | ✅ | ✅ |
| PID Control | ❌ | ✅ |
| Button UI | ❌ | ✅ |
| Serial Commands | ✅ | ❌ |
| Remote Commands | ❌ | ✅ |
| Auto-stop | ❌ | ✅ |
| Historical Logs | ❌ | ✅ |

**Default Mode**: PRODUCTION_MODE (full feature set)

---

## Quick Reference Card

### Switch to Development Mode
```cpp
// Line 28 in RiceDryer.ino
#define DEVELOPMENT_MODE
```

### Switch to Production Mode
```cpp
// Line 28 in RiceDryer.ino
//#define DEVELOPMENT_MODE
```

### Serial Monitor Commands (Development Only)
```
HELP                 - Show all commands
STATUS              - Show hardware status
LCD_TEST            - Test LCD display
DHT_READ            - Read sensor
RELAY_HEATER_ON     - Turn on heater
RELAY_HEATER_OFF    - Turn off heater
RELAY_BLOWER_ON     - Turn on blower
RELAY_BLOWER_OFF    - Turn off blower
LCD_PRINT:text      - Custom LCD text
```

---

**Version**: 1.0.0  
**Last Updated**: December 2025  
**Firmware**: RiceDryer ESP32
