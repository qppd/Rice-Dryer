# RiceDryer ESP32 - Code Flow Diagram

## Compilation Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                        RiceDryer.ino                                │
│                                                                     │
│  Line 28: //#define DEVELOPMENT_MODE                               │
│           ^                                                         │
│           └─────────── CHANGE THIS LINE ──────────┐                │
│                                                     │                │
└─────────────────────────────────────────────────────┼────────────────┘
                                                      │
                        ┌─────────────────────────────┴─────────────────────────────┐
                        │                                                           │
                        ▼                                                           ▼
          ┌─────────────────────────┐                            ┌──────────────────────────┐
          │  DEVELOPMENT_MODE       │                            │  PRODUCTION_MODE         │
          │  (#define enabled)      │                            │  (#define commented)     │
          └─────────────────────────┘                            └──────────────────────────┘
                        │                                                           │
                        │                                                           │
                        ▼                                                           ▼
          ┌─────────────────────────┐                            ┌──────────────────────────┐
          │  Include Only:          │                            │  Include All:            │
          │  • Button.h             │                            │  • WiFi.h                │
          │  • DHT22Sensor.h        │                            │  • Firebase_ESP_Client.h │
          │  • SSR.h                │                            │  • WiFiManagerCustom.h   │
          │  • LCDDisplay.h         │                            │  • FirebaseConfig.h      │
          │  • TemperatureController│                            │  • All component headers │
          │  • PinConfig.h          │                            │  • PinConfig.h           │
          └─────────────────────────┘                            └──────────────────────────┘
                        │                                                           │
                        │                                                           │
                        ▼                                                           ▼
          ┌─────────────────────────┐                            ┌──────────────────────────┐
          │  Define Components:     │                            │  Define Components:      │
          │  • devDHT (DHT22)       │                            │  • dht (DHT22)           │
          │  • devHeater (SSR)      │                            │  • relay1, relay2 (SSR)  │
          │  • devBlower (SSR)      │                            │  • button1, button2,     │
          │  • devLCD (LCD)         │                            │    button3 (Button)      │
          │                         │                            │  • lcd (LCD)             │
          │  NO WiFi/Firebase vars  │                            │  • tempController (PID)  │
          │                         │                            │  • Firebase objects      │
          │                         │                            │  • WiFi objects          │
          └─────────────────────────┘                            └──────────────────────────┘
                        │                                                           │
                        │                                                           │
                        ▼                                                           ▼
          ┌─────────────────────────┐                            ┌──────────────────────────┐
          │  Define Functions:      │                            │  Define Functions:       │
          │  • runDevelopmentMode() │                            │  • productionSetup()     │
          │  • handleDevelopment    │                            │  • productionLoop()      │
          │    SerialCommands()     │                            │  • initWiFi()            │
          │                         │                            │  • initFirebase()        │
          │                         │                            │  • checkPairingStatus()  │
          │                         │                            │  • controlDrying()       │
          │                         │                            │  • sendDataToFirebase()  │
          │                         │                            │  • checkRemoteCommands() │
          │                         │                            │  • (50+ functions)       │
          └─────────────────────────┘                            └──────────────────────────┘
                        │                                                           │
                        └─────────────────────────────┬─────────────────────────────┘
                                                      │
                                                      ▼
                        ┌──────────────────────────────────────────────────────────┐
                        │                    setup()                               │
                        │  ┌────────────────────────────────────────────────────┐  │
                        │  │  #ifdef DEVELOPMENT_MODE                           │  │
                        │  │    Serial.begin(115200);                           │  │
                        │  │    runDevelopmentMode();  ← infinite loop          │  │
                        │  │  #else                                             │  │
                        │  │    productionSetup();                              │  │
                        │  │  #endif                                            │  │
                        │  └────────────────────────────────────────────────────┘  │
                        └──────────────────────────────────────────────────────────┘
                                                      │
                                                      ▼
                        ┌──────────────────────────────────────────────────────────┐
                        │                    loop()                                │
                        │  ┌────────────────────────────────────────────────────┐  │
                        │  │  #ifdef DEVELOPMENT_MODE                           │  │
                        │  │    delay(1000);  ← never reached                   │  │
                        │  │  #else                                             │  │
                        │  │    productionLoop();                               │  │
                        │  │  #endif                                            │  │
                        │  └────────────────────────────────────────────────────┘  │
                        └──────────────────────────────────────────────────────────┘
```

---

## Development Mode Execution Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                         ESP32 Boot                                  │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│  setup() → Serial.begin(115200) → runDevelopmentMode()             │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Initialize Hardware:                                               │
│    devDHT.begin()                                                   │
│    devHeater.begin()                                                │
│    devBlower.begin()                                                │
│    devLCD.begin()                                                   │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Display on LCD:                                                    │
│    Line 0: "DEVELOPMENT MODE"                                       │
│    Line 1: "Hardware Test"                                          │
│    Line 2: "Open Serial @115200"                                    │
│    Line 3: "Type HELP"                                              │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Print to Serial:                                                   │
│    "DEVELOPMENT MODE ACTIVATED"                                     │
│    "WiFi, Firebase, and production logic are DISABLED."             │
│    "Type HELP for available commands."                              │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
                       ┌───────────────┐
                       │  Infinite Loop │
                       │  while(true)   │
                       └───────┬────────┘
                               │
                    ┌──────────┴──────────┐
                    │                     │
                    ▼                     │
        ┌────────────────────────┐        │
        │  Read Serial Input     │        │
        │  handleDevelopment     │        │
        │  SerialCommands()      │        │
        └────────┬───────────────┘        │
                 │                        │
                 ▼                        │
        ┌────────────────────────┐        │
        │  Parse Command:        │        │
        │  • LCD_TEST            │        │
        │  • DHT_READ            │        │
        │  • RELAY_HEATER_ON     │        │
        │  • RELAY_BLOWER_OFF    │        │
        │  • STATUS              │        │
        │  • HELP                │        │
        │  • (etc)               │        │
        └────────┬───────────────┘        │
                 │                        │
                 ▼                        │
        ┌────────────────────────┐        │
        │  Execute Action:       │        │
        │  • Update LCD          │        │
        │  • Read sensor         │        │
        │  • Control relays      │        │
        │  • Print status        │        │
        └────────┬───────────────┘        │
                 │                        │
                 ▼                        │
        ┌────────────────────────┐        │
        │  delay(50);            │        │
        └────────┬───────────────┘        │
                 │                        │
                 └────────────────────────┘
```

---

## Production Mode Execution Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                         ESP32 Boot                                  │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│  setup() → productionSetup()                                        │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Initialize Hardware:                                               │
│    button1.begin(), button2.begin(), button3.begin()               │
│    dht.begin(), relay1.begin(), relay2.begin()                     │
│    lcd.begin(), tempController.begin()                             │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Check Button 1 at Startup:                                         │
│    If pressed → runTestMenu() (legacy test mode)                    │
│    If not pressed → Continue                                        │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Initialize WiFi:                                                   │
│    initWiFi() → WiFiManager captive portal                          │
│    Connect to WiFi network                                          │
│    Initialize NTP (Philippines UTC+8)                               │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Initialize Firebase:                                               │
│    initFirebase() → Connect to Realtime Database                    │
│    registerDevice() → Write device info                             │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Check Pairing:                                                     │
│    checkPairingStatus()                                             │
│      If paired → Ready to use                                       │
│      If not paired → Generate 6-digit code, display on LCD          │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
                       ┌───────────────┐
                       │  loop() calls │
                       │  production   │
                       │  Loop()       │
                       └───────┬───────┘
                               │
                    ┌──────────┴──────────┐
                    │                     │
                    ▼                     │
        ┌────────────────────────┐        │
        │  Check WiFi Status     │        │
        │  Reconnect if needed   │        │
        └────────┬───────────────┘        │
                 │                        │
                 ▼                        │
        ┌────────────────────────┐        │
        │  If Not Paired:        │        │
        │  • Display pairing code│        │
        │  • Check for pairing   │        │
        │  • Return (skip rest)  │        │
        └────────┬───────────────┘        │
                 │                        │
                 ▼                        │
        ┌────────────────────────┐        │
        │  Handle Buttons:       │        │
        │  • Button 1: Mode      │        │
        │  • Button 2: Start/+   │        │
        │  • Button 3: Reset/-   │        │
        └────────┬───────────────┘        │
                 │                        │
                 ▼                        │
        ┌────────────────────────┐        │
        │  Setpoint Adjustment:  │        │
        │  • Temperature         │        │
        │  • Humidity            │        │
        └────────┬───────────────┘        │
                 │                        │
                 ▼                        │
        ┌────────────────────────┐        │
        │  Read Sensors:         │        │
        │  • DHT22 (2s interval) │        │
        │  • Validate readings   │        │
        └────────┬───────────────┘        │
                 │                        │
                 ▼                        │
        ┌────────────────────────┐        │
        │  Control Drying:       │        │
        │  • PID temperature     │        │
        │  • Check humidity      │        │
        │  • Control relays      │        │
        │  • Auto-stop logic     │        │
        └────────┬───────────────┘        │
                 │                        │
                 ▼                        │
        ┌────────────────────────┐        │
        │  Firebase Communication│        │
        │  • Update current (5s) │        │
        │  • Log history (30s)   │        │
        │  • Check commands (1s) │        │
        └────────┬───────────────┘        │
                 │                        │
                 ▼                        │
        ┌────────────────────────┐        │
        │  Update LCD Display    │        │
        │  • Normal mode: Status │        │
        │  • Set mode: Setpoints │        │
        └────────┬───────────────┘        │
                 │                        │
                 ▼                        │
        ┌────────────────────────┐        │
        │  delay(200);           │        │
        └────────┬───────────────┘        │
                 │                        │
                 └────────────────────────┘
```

---

## File Layout

```
RiceDryer.ino (1212 lines)
│
├── [Lines 1-28] ─────────────────┐
│   Mode Configuration Header      │  📋 EDIT HERE TO SWITCH MODES
│   //#define DEVELOPMENT_MODE     │
└──────────────────────────────────┘
│
├── [Lines 30-47] ────────────────┐
│   Conditional Includes           │  📚 WiFi/Firebase only if PRODUCTION
│   #ifndef DEVELOPMENT_MODE       │
│     #include <WiFi.h>            │
│     #include <Firebase...>       │
│   #endif                         │
└──────────────────────────────────┘
│
├── [Lines 49-781] ───────────────┐
│   Production Mode Variables      │  🚀 PRODUCTION ONLY
│   #ifndef DEVELOPMENT_MODE       │     (Firebase, WiFi, PID, etc.)
│     FirebaseData fbdo;           │
│     String deviceId;             │
│     (50+ functions)              │
│   #endif (implied)               │
└──────────────────────────────────┘
│
├── [Lines 783-920] ──────────────┐
│   Development Mode Code          │  🔧 DEVELOPMENT ONLY
│   #ifdef DEVELOPMENT_MODE        │     (Serial commands, testing)
│     DHT22Sensor devDHT;          │
│     void runDevelopmentMode();   │
│     void handleDevelopment...    │
└──────────────────────────────────┘
│
├── [Lines 922-1184] ─────────────┐
│   Production Mode Functions      │  🚀 PRODUCTION ONLY
│   #else                          │     (productionSetup/Loop)
│     void productionSetup();      │
│     void productionLoop();       │
│   #endif                         │
└──────────────────────────────────┘
│
└── [Lines 1186-1212] ────────────┐
    Main Entry Points              │  🎯 MODE DISPATCHER
    void setup() {                 │     (Calls appropriate mode)
      #ifdef DEVELOPMENT_MODE      │
        runDevelopmentMode();      │
      #else                        │
        productionSetup();         │
      #endif                       │
    }                              │
    void loop() {                  │
      #ifdef DEVELOPMENT_MODE      │
        delay(1000);               │
      #else                        │
        productionLoop();          │
      #endif                       │
    }                              │
└──────────────────────────────────┘
```

---

## Memory Map (Approximate)

### Development Mode
```
┌────────────────────────────────────┐
│  Program Flash (Estimated)         │
│                                    │
│  Arduino Core         ~100 KB     │
│  Component Classes     ~30 KB     │
│  Development Code      ~10 KB     │
│                                    │
│  TOTAL                ~140 KB     │
│  (WiFi/Firebase NOT included)     │
└────────────────────────────────────┘
```

### Production Mode
```
┌────────────────────────────────────┐
│  Program Flash (Estimated)         │
│                                    │
│  Arduino Core         ~100 KB     │
│  WiFi Library          ~80 KB     │
│  Firebase Library     ~150 KB     │
│  Component Classes     ~30 KB     │
│  Production Code       ~60 KB     │
│                                    │
│  TOTAL                ~420 KB     │
│  (Full feature set)               │
└────────────────────────────────────┘
```

---

## Decision Tree: Which Mode?

```
                    ┌────────────────────────┐
                    │  What do you want      │
                    │  to do?                │
                    └───────────┬────────────┘
                                │
                ┌───────────────┴───────────────┐
                │                               │
                ▼                               ▼
    ┌──────────────────────┐      ┌────────────────────────┐
    │  Test individual     │      │  Run the rice dryer    │
    │  components          │      │  for actual drying     │
    └──────────┬───────────┘      └────────┬───────────────┘
               │                           │
               ▼                           ▼
    ┌──────────────────────┐      ┌────────────────────────┐
    │  DEVELOPMENT_MODE    │      │  PRODUCTION_MODE       │
    │                      │      │                        │
    │  ✅ No WiFi needed   │      │  ✅ WiFi required      │
    │  ✅ Serial commands  │      │  ✅ Firebase sync      │
    │  ✅ Manual control   │      │  ✅ PID control        │
    │  ✅ Quick testing    │      │  ✅ Auto-stop          │
    │  ❌ No app needed    │      │  ✅ Remote control     │
    │  ❌ No Firebase      │      │  ✅ Button UI          │
    │  ❌ No PID           │      │  ✅ Pairing system     │
    └──────────────────────┘      └────────────────────────┘
```

---

## Visual Mode Comparison

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        DEVELOPMENT MODE vs PRODUCTION MODE                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  🔧 DEVELOPMENT_MODE                    🚀 PRODUCTION_MODE                      │
│  ┌──────────────────────────┐          ┌────────────────────────────┐          │
│  │                          │          │                            │          │
│  │   ┌──────────────┐       │          │    ┌───────────────┐       │          │
│  │   │   ESP32      │       │          │    │    ESP32      │       │          │
│  │   └──────┬───────┘       │          │    └───────┬───────┘       │          │
│  │          │               │          │            │               │          │
│  │          ├─ Serial ──────┼─────────→ PC         ├─ WiFi ────────┼──→ Router│
│  │          │    (115200)   │          │            │               │          │
│  │          ├─ LCD          │          │            ├─ LCD          │          │
│  │          ├─ DHT22        │          │            ├─ DHT22        │          │
│  │          ├─ Heater Relay │          │            ├─ Heater Relay │          │
│  │          └─ Blower Relay │          │            ├─ Blower Relay │          │
│  │                          │          │            ├─ Button 1     │          │
│  │  Commands:               │          │            ├─ Button 2     │          │
│  │  • LCD_TEST              │          │            └─ Button 3     │          │
│  │  • DHT_READ              │          │                            │          │
│  │  • RELAY_HEATER_ON       │          │    Firebase ←──────────────┼───→ App  │
│  │  • RELAY_BLOWER_OFF      │          │    (Realtime Database)     │          │
│  │  • STATUS                │          │                            │          │
│  │  • HELP                  │          │    Features:               │          │
│  │                          │          │    • PID Control           │          │
│  │  No WiFi ❌              │          │    • Auto-stop             │          │
│  │  No Firebase ❌          │          │    • Remote Commands       │          │
│  │  No PID ❌               │          │    • Historical Logs       │          │
│  │                          │          │    • Pairing System        │          │
│  └──────────────────────────┘          └────────────────────────────┘          │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

**Created**: December 7, 2025  
**Project**: RiceDryer ESP32 Firmware  
**Version**: 1.0.0
