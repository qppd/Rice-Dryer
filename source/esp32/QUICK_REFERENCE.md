# 🔧 Development Mode - Quick Reference

## Enable Development Mode
Edit `RiceDryer.ino` **line 28**:
```cpp
#define DEVELOPMENT_MODE  // Uncomment this line
```

## Serial Commands (115200 baud)

### Basic Commands
```
HELP               - Show all commands
STATUS             - Show current hardware status
```

### LCD Commands
```
LCD_TEST           - Test all 4 LCD lines
LCD_CLEAR          - Clear the display
LCD_PRINT:Hello    - Print "Hello" on LCD
```

### Sensor Command
```
DHT_READ           - Read temperature & humidity from DHT22
```

### Relay Commands (Heater - GPIO 19)
```
RELAY_HEATER_ON    - Turn heater relay ON (GPIO 19 → LOW)
RELAY_HEATER_OFF   - Turn heater relay OFF (GPIO 19 → HIGH)
```

### Relay Commands (Blower - GPIO 18)
```
RELAY_BLOWER_ON    - Turn blower relay ON (GPIO 18 → LOW)
RELAY_BLOWER_OFF   - Turn blower relay OFF (GPIO 18 → HIGH)
```

## Example Test Sequence
```
1. Upload firmware with DEVELOPMENT_MODE enabled
2. Open Serial Monitor (115200 baud)
3. Type: HELP
4. Type: LCD_TEST
5. Type: DHT_READ
6. Type: RELAY_HEATER_ON
7. Wait 2 seconds
8. Type: RELAY_HEATER_OFF
9. Type: STATUS
```

## Hardware Pins
| Component | GPIO | Type |
|-----------|------|------|
| DHT22 Sensor | 23 | Data |
| Heater Relay | 19 | Output (Active LOW) |
| Blower Relay | 18 | Output (Active LOW) |
| LCD SDA | 21 | I2C |
| LCD SCL | 22 | I2C |

## LCD Display Address
- **Address**: 0x27
- **Size**: 20x4 characters

## Switch Back to Production Mode
Edit `RiceDryer.ino` **line 28**:
```cpp
//#define DEVELOPMENT_MODE  // Comment this line
```

## Need Help?
See **DEVELOPMENT_MODE_GUIDE.md** for full documentation.
