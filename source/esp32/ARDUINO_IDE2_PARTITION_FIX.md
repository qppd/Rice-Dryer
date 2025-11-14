# How to Change Partition Scheme in Arduino IDE 2.x

## Arduino IDE 2.x doesn't show partition scheme in Tools menu by default

### Solution 1: Manually Edit boards.txt

1. **Find your ESP32 boards.txt file:**
   ```
   Windows: C:\Users\<YourUsername>\AppData\Local\Arduino15\packages\esp32\hardware\esp32\<version>\boards.txt
   ```

2. **Find your board section** (search for your board, e.g., "esp32.name=ESP32 Dev Module")

3. **Look for the line:**
   ```
   esp32.build.partitions=default
   ```

4. **Change it to:**
   ```
   esp32.build.partitions=huge_app
   ```
   
   **Or one of these alternatives:**
   - `huge_app` - 3MB APP / 1MB SPIFFS (BEST for your project)
   - `no_ota` - 2MB APP / 2MB SPIFFS
   - `min_spiffs` - 1.9MB APP / 190KB SPIFFS

5. **Save the file and restart Arduino IDE**

6. **Compile again**

---

### Solution 2: Use platform.local.txt (Cleaner Method)

1. **Navigate to:**
   ```
   C:\Users\<YourUsername>\AppData\Local\Arduino15\packages\esp32\hardware\esp32\<version>\
   ```

2. **Create a new file named:** `platform.local.txt`

3. **Add this line:**
   ```
   build.partitions=huge_app
   ```

4. **Save and restart Arduino IDE**

---

### Solution 3: Use platformio.ini (Switch to PlatformIO)

If you're open to using PlatformIO (better for ESP32 projects):

1. **Install PlatformIO extension in VS Code**

2. **Create platformio.ini in your project folder:**
   ```ini
   [env:esp32dev]
   platform = espressif32
   board = esp32dev
   framework = arduino
   board_build.partitions = huge_app.csv
   monitor_speed = 115200
   
   lib_deps = 
       mobizt/Firebase Arduino Client Library for ESP8266 and ESP32
       adafruit/DHT sensor library
       adafruit/Adafruit Unified Sensor
       bblanchon/ArduinoJson
       marcoschwartz/LiquidCrystal_I2C
       br3ttb/PID
       tzapu/WiFiManager
   ```

3. **Compile with PlatformIO** (much better memory management)

---

### Solution 4: Add Custom Board Definition

1. **Go to:**
   ```
   C:\Users\<YourUsername>\AppData\Local\Arduino15\packages\esp32\hardware\esp32\<version>\boards.txt
   ```

2. **Add at the end of the file:**
   ```
   ##############################################################
   
   esp32_huge.name=ESP32 Dev Module (Huge APP)
   esp32_huge.upload.tool=esptool_py
   esp32_huge.upload.maximum_size=3145728
   esp32_huge.upload.maximum_data_size=327680
   esp32_huge.upload.wait_for_upload_port=true
   esp32_huge.serial.disableDTR=true
   esp32_huge.serial.disableRTS=true
   esp32_huge.build.mcu=esp32
   esp32_huge.build.core=esp32
   esp32_huge.build.variant=esp32
   esp32_huge.build.board=ESP32_DEV
   esp32_huge.build.partitions=huge_app
   esp32_huge.build.flash_size=4MB
   esp32_huge.build.flash_mode=dio
   esp32_huge.build.boot=dio
   esp32_huge.build.flash_freq=80m
   esp32_huge.menu.CPUFreq.240=240MHz (WiFi/BT)
   esp32_huge.menu.CPUFreq.240.build.f_cpu=240000000L
   ```

3. **Restart Arduino IDE**

4. **Select "ESP32 Dev Module (Huge APP)" from Tools → Board menu**

---

## Quick Test - Which Partition Are You Using?

Add this to your setup() temporarily:
```cpp
void setup() {
  Serial.begin(115200);
  Serial.println("Partition info:");
  Serial.print("Sketch size: ");
  Serial.println(ESP.getSketchSize());
  Serial.print("Free sketch space: ");
  Serial.println(ESP.getFreeSketchSpace());
  Serial.print("Sketch MD5: ");
  Serial.println(ESP.getSketchMD5());
  // ... rest of your setup
}
```

This will show you how much space you actually have.

---

## Recommendation:

**Use Solution 1 or 2** - they're the simplest and don't require switching IDEs.

Just edit the partition setting to `huge_app` and you'll have 3MB for your sketch instead of 1.3MB.
