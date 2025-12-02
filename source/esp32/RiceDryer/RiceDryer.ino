// RiceDryer.ino
// Main Arduino sketch for RiceDryer ESP32 project

// WiFi and Network
#include <WiFi.h>
#include <time.h>
#include <Preferences.h>

// Firebase
#include <Firebase_ESP_Client.h>
#include <addons/TokenHelper.h>
#include <addons/RTDBHelper.h>

// Custom modules
#include "WiFiManagerCustom.h"
#include "FirebaseConfig.h"

// Include component headers
#include "Button.h"
#include "DHT22Sensor.h"
#include "SSR.h"
#include "LCDDisplay.h"
#include "TemperatureController.h"

// Pin configuration
#include "PinConfig.h"

// Firebase objects
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

// WiFi Manager
WiFiManagerCustom wifiManager;

// Preferences for persistent storage
Preferences preferences;

// Device Information
String deviceId;
String pairingCode = "";
bool devicePaired = false;
unsigned long long pairingCodeExpiry = 0;  // Changed to unsigned long long for Unix timestamps

// LCD state tracking to prevent flickering
String lastLcdLine0 = "";
String lastLcdLine1 = "";
String lastLcdLine2 = "";
String lastLcdLine3 = "";

// Components
Button button1(BUTTON_1);
Button button2(BUTTON_2);
Button button3(BUTTON_3);
DHT22Sensor dht(DHT_PIN);
SSR relay1(RELAY_1);
SSR relay2(RELAY_2);
LCDDisplay lcd(LCD_ADDR, LCD_COLS, LCD_ROWS);
TemperatureController tempController;

// Legacy compatibility
Button& button = button1;  // Reference to first button for existing code
SSR& ssr = relay1;         // Reference to first relay for existing code

// Logic variables
bool dryingActive = false;
unsigned long lastSensorRead = 0;
unsigned long lastFirebaseUpdate = 0;
unsigned long lastHistoryLog = 0;
unsigned long lastCommandCheck = 0;
const unsigned long SENSOR_INTERVAL = 2000; // ms
const unsigned long FIREBASE_UPDATE_INTERVAL = 5000; // 5 seconds
const unsigned long HISTORY_LOG_INTERVAL = 30000; // 30 seconds
const unsigned long COMMAND_CHECK_INTERVAL = 1000; // 1 second
const unsigned long PAIRING_CODE_VALIDITY = 600000; // 10 minutes

float temperature = 0.0;
float humidity = 0.0;
float setpointTemp = 40.0;     // Default target temperature
float setpointHumidity = 20.0; // Default target humidity (stop when reached)
bool wifiConnected = false;
bool firebaseConnected = false;

// Button adjustment settings
const float TEMP_STEP = 1.0;      // Temperature adjustment increment (1°C)
const float HUMIDITY_STEP = 1.0;  // Humidity adjustment increment (1%)
const float TEMP_MIN = 30.0;
const float TEMP_MAX = 80.0;
const float HUMIDITY_MIN = 10.0;
const float HUMIDITY_MAX = 50.0;

// UI and control states
enum SettingMode {
  NORMAL_MODE,
  SET_TEMP_MODE,
  SET_HUMIDITY_MODE
};

SettingMode currentMode = NORMAL_MODE;
unsigned long modeStartTime = 0;
const unsigned long MODE_TIMEOUT = 5000; // 5 seconds timeout for setting modes

// Button states for debouncing
bool button1LastState = false;
bool button2LastState = false;
bool button3LastState = false;
unsigned long button1LastPress = 0;
unsigned long button2LastPress = 0;
unsigned long button3LastPress = 0;
const unsigned long BUTTON_DEBOUNCE = 200; // 200ms debounce

// Generate unique device ID from MAC address
String getDeviceId() {
  uint8_t mac[6];
  WiFi.macAddress(mac);
  char macStr[18];
  snprintf(macStr, sizeof(macStr), "%02X%02X%02X%02X%02X%02X",
           mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
  return String(macStr);
}

// Generate random 6-digit pairing code
String generatePairingCode() {
  String code = "";
  for (int i = 0; i < 6; i++) {
    code += String(random(0, 10));
  }
  return code;
}

// Initialize WiFi with WiFiManager
bool initWiFi() {
  lcd.clear();
  lcd.print(0, 0, "WiFi Setup");
  lcd.print(0, 1, "Connect to AP");
  
  if (!wifiManager.begin("RiceDryer_Setup", "password123")) {
    lcd.clear();
    lcd.print(0, 0, "WiFi Failed!");
    lcd.print(0, 1, "Restarting...");
    delay(3000);
    ESP.restart();
    return false;
  }
  
  lcd.clear();
  lcd.print(0, 0, "WiFi Connected!");
  lcd.print(0, 1, wifiManager.getLocalIP().c_str());
  delay(2000);
  
  wifiConnected = true;
  
  // Initialize NTP for real timestamps
  initNTP();
  
  return true;
}

// Initialize NTP (Network Time Protocol) for Philippines timezone
void initNTP() {
  lcd.clear();
  lcd.print(0, 0, "Syncing Time...");
  
  // Philippines timezone: UTC+8, no DST
  // NTP servers for Philippines
  configTime(8 * 3600, 0, "time.google.com", "pool.ntp.org", "time.cloudflare.com");
  
  // Wait for time to be set
  int retries = 0;
  while (time(nullptr) < 100000 && retries < 20) {
    delay(500);
    Serial.print(".");
    retries++;
  }
  
  if (time(nullptr) > 100000) {
    lcd.clear();
    lcd.print(0, 0, "Time Synced!");
    struct tm timeinfo;
    if (getLocalTime(&timeinfo)) {
      char timeStr[20];
      strftime(timeStr, sizeof(timeStr), "%Y-%m-%d %H:%M", &timeinfo);
      lcd.print(0, 1, timeStr);
      Serial.println("\nNTP time synchronized: " + String(timeStr));
    }
    delay(2000);
  } else {
    lcd.clear();
    lcd.print(0, 0, "Time Sync Failed");
    lcd.print(0, 1, "Using millis()");
    Serial.println("\nNTP sync failed, will use millis()");
    delay(2000);
  }
}

// Get current Unix timestamp in milliseconds
unsigned long long getTimestamp() {
  struct timeval tv;
  gettimeofday(&tv, NULL);
  unsigned long long timestamp = (unsigned long long)(tv.tv_sec) * 1000ULL + (tv.tv_usec / 1000ULL);
  
  // If NTP hasn't synced yet, fall back to millis()
  if (timestamp < 1000000000000ULL) {
    return millis();
  }
  
  return timestamp;
}

// Initialize Firebase connection
bool initFirebase() {
  lcd.clear();
  lcd.print(0, 0, "Connecting to");
  lcd.print(0, 1, "Firebase...");
  
  config.host = RiceDryerConfig::getFirebaseHost();
  config.signer.tokens.legacy_token = RiceDryerConfig::getFirebaseAuth();
  config.database_url = RiceDryerConfig::getDatabaseURL();
  config.timeout.serverResponse = 10 * 1000;
  
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
  
  if (Firebase.ready()) {
    lcd.clear();
    lcd.print(0, 0, "Firebase Ready!");
    delay(1000);
    firebaseConnected = true;
    
    // Register device
    registerDevice();
    return true;
  } else {
    lcd.clear();
    lcd.print(0, 0, "Firebase Failed!");
    delay(2000);
    return false;
  }
}

// Register device in Firebase
void registerDevice() {
  String path = "/devices/" + deviceId + "/deviceInfo";
  FirebaseJson json;
  json.set("macAddress", deviceId);
  json.set("firmwareVersion", "1.0.0");
  json.set("hardwareVersion", "1.0");
  json.set("lastBoot", (double)getTimestamp());  // Use NTP timestamp
  
  if (Firebase.RTDB.setJSON(&fbdo, path.c_str(), &json)) {
    Serial.println("Device registered successfully");
  } else {
    Serial.println("Device registration failed: " + fbdo.errorReason());
  }
  
  // Check if device is paired
  checkPairingStatus();
}

// Check if device is already paired
void checkPairingStatus() {
  // First check local storage (persists across reboots)
  preferences.begin("ricedryer", false);
  bool localPaired = preferences.getBool("paired", false);
  String localUserId = preferences.getString("userId", "");
  preferences.end();
  
  if (localPaired && localUserId != "") {
    // Device was paired before - verify with Firebase
    String path = "/devices/" + deviceId + "/deviceInfo/pairedTo";
    if (Firebase.RTDB.getString(&fbdo, path.c_str())) {
      String pairedTo = fbdo.stringData();
      
      // Check if still paired to the same user
      if (pairedTo == localUserId) {
        devicePaired = true;
        lcd.clear();
        lcd.print(0, 0, "Device Paired!");
        lcd.print(0, 1, "Ready to use");
        Serial.println("Device already paired to: " + localUserId);
        delay(1000);
        return;
      } else if (pairedTo == "" || pairedTo == "null") {
        // Unpaired on Firebase - clear local storage
        Serial.println("Device was unpaired remotely");
        clearPairingData();
      } else {
        // Paired to different user - clear and re-pair
        Serial.println("Device paired to different user, clearing local data");
        clearPairingData();
      }
    }
  }
  
  // Not paired locally or verification failed - check Firebase
  String path = "/devices/" + deviceId + "/deviceInfo/pairedTo";
  if (Firebase.RTDB.getString(&fbdo, path.c_str())) {
    String pairedTo = fbdo.stringData();
    if (pairedTo != "" && pairedTo != "null") {
      // Paired on Firebase but not locally - save to local storage
      devicePaired = true;
      savePairingData(pairedTo);
      lcd.clear();
      lcd.print(0, 0, "Device Paired!");
      lcd.print(0, 1, "Synced from cloud");
      Serial.println("Pairing synced from Firebase: " + pairedTo);
      delay(1000);
    } else {
      // Not paired - generate pairing code
      startPairingMode();
    }
  }
}

// Save pairing data to persistent storage
void savePairingData(String userId) {
  preferences.begin("ricedryer", false);
  preferences.putBool("paired", true);
  preferences.putString("userId", userId);
  preferences.end();
  Serial.println("Pairing data saved to flash: " + userId);
}

// Clear pairing data from persistent storage
void clearPairingData() {
  preferences.begin("ricedryer", false);
  preferences.putBool("paired", false);
  preferences.putString("userId", "");
  preferences.end();
  devicePaired = false;
  Serial.println("Pairing data cleared from flash");
}

// Start pairing mode
void startPairingMode() {
  pairingCode = generatePairingCode();
  unsigned long long currentTime = getTimestamp();
  pairingCodeExpiry = currentTime + PAIRING_CODE_VALIDITY;
  
  // Store pairing code in Firebase
  String path = "/devicePairing/" + pairingCode;
  FirebaseJson json;
  json.set("deviceId", deviceId);
  json.set("generatedAt", (double)currentTime);
  json.set("expiresAt", (double)pairingCodeExpiry);
  json.set("used", false);
  
  Firebase.RTDB.setJSON(&fbdo, path.c_str(), &json);
  
  Serial.println("Pairing mode started - Code: " + pairingCode);
  lcd.clear();
  lcd.print(0, 0, "Pairing Code:");
  lcd.print(0, 1, pairingCode.c_str());
  lcd.print(0, 2, "MAC Address:");
  lcd.print(0, 3, deviceId.c_str());
}

// Send data to Firebase
void sendDataToFirebase() {
  if (!firebaseConnected || !devicePaired) return;
  
  String path = "/devices/" + deviceId + "/current";
  FirebaseJson json;
  json.set("temperature", temperature);
  json.set("humidity", humidity);
  json.set("setpointTemp", setpointTemp);
  json.set("setpointHumidity", setpointHumidity);
  json.set("relay1Status", relay1.isOn());
  json.set("relay2Status", relay2.isOn());
  json.set("dryingActive", dryingActive);
  json.set("currentMode", (int)currentMode);
  json.set("pidOutput", tempController.getOutput());
  json.set("online", true);
  json.set("lastUpdate", (double)getTimestamp());  // Use NTP timestamp
  
  if (!Firebase.RTDB.setJSON(&fbdo, path.c_str(), &json)) {
    Serial.println("Failed to send data: " + fbdo.errorReason());
  }
}

// Log historical data
void logHistoricalData() {
  if (!firebaseConnected || !devicePaired) return;
  
  unsigned long long timestamp = getTimestamp();
  String path = "/devices/" + deviceId + "/history/" + String((unsigned long)(timestamp / 1000));  // Use seconds for key
  
  FirebaseJson json;
  json.set("temperature", temperature);
  json.set("humidity", humidity);
  json.set("setpointTemp", setpointTemp);
  json.set("setpointHumidity", setpointHumidity);
  json.set("relay1Status", relay1.isOn());
  json.set("relay2Status", relay2.isOn());
  json.set("dryingActive", dryingActive);
  json.set("pidOutput", tempController.getOutput());
  json.set("timestamp", (double)timestamp);  // Also include timestamp in the data
  
  if (!Firebase.RTDB.setJSON(&fbdo, path.c_str(), &json)) {
    Serial.println("Failed to log history: " + fbdo.errorReason());
  }
}

// Check for remote commands
void checkRemoteCommands() {
  if (!firebaseConnected || !devicePaired) return;
  
  String path = "/devices/" + deviceId + "/commands";
  if (Firebase.RTDB.getJSON(&fbdo, path.c_str())) {
    FirebaseJson &json = fbdo.jsonObject();
    FirebaseJsonData jsonData;
    
    // Check for START command
    if (json.get(jsonData, "action")) {
      String action = jsonData.stringValue;
      
      if (action == "START") {
        dryingActive = true;
        acknowledgeCommand("START");
      } else if (action == "STOP") {
        dryingActive = false;
        acknowledgeCommand("STOP");
      } else if (action == "SET_TEMP") {
        if (json.get(jsonData, "value")) {
          setpointTemp = jsonData.floatValue;
          tempController.setSetpoint(setpointTemp);  // Update PID setpoint
          acknowledgeCommand("SET_TEMP");
        }
      } else if (action == "SET_HUMIDITY") {
        if (json.get(jsonData, "value")) {
          setpointHumidity = jsonData.floatValue;
          acknowledgeCommand("SET_HUMIDITY");
        }
      }
      
      // Clear command
      Firebase.RTDB.deleteNode(&fbdo, path.c_str());
    }
  }
}

// Acknowledge command execution
void acknowledgeCommand(String command) {
  String path = "/devices/" + deviceId + "/commandAck";
  FirebaseJson json;
  json.set("command", command);
  json.set("timestamp", (double)getTimestamp());  // Use NTP timestamp
  json.set("acknowledged", true);
  
  Firebase.RTDB.setJSON(&fbdo, path.c_str(), &json);
}

void testDHT22() {
  lcd.clear();
  lcd.print(0, 0, "Testing DHT22...");
  float t = dht.readTemperature();
  float h = dht.readHumidity();
  char buf[17];
  snprintf(buf, sizeof(buf), "T:%2.1f H:%2.1f", t, h);
  lcd.print(0, 1, String(buf));
  delay(2000);
}



void testSSR() {
  lcd.clear();
  lcd.print(0, 0, "Testing Relay1...");
  lcd.print(0, 1, "ON for 2s");
  relay1.on();
  delay(2000);
  lcd.print(0, 1, "OFF for 2s");
  relay1.off();
  delay(1000);
  
  lcd.print(0, 0, "Testing Relay2...");
  lcd.print(0, 1, "ON for 2s");
  relay2.on();
  delay(2000);
  lcd.print(0, 1, "OFF for 2s");
  relay2.off();
  delay(2000);
}

void testLCD() {
  lcd.clear();
  lcd.print(0, 0, "Testing LCD...");
  lcd.print(0, 1, "Hello World!");
  delay(2000);
  lcd.clear();
}

// Handle button press with debouncing
bool isButtonPressed(Button& button, bool& lastState, unsigned long& lastPress) {
  bool currentState = button.isPressed();
  bool pressed = false;
  
  if (currentState && !lastState && (millis() - lastPress > BUTTON_DEBOUNCE)) {
    pressed = true;
    lastPress = millis();
  }
  
  lastState = currentState;
  return pressed;
}

// Handle button-based setpoint adjustment
void handleSetpointAdjustment() {
  // Button 2: Increase setpoint
  if (isButtonPressed(button2, button2LastState, button2LastPress)) {
    switch (currentMode) {
      case SET_TEMP_MODE:
        setpointTemp += TEMP_STEP;
        if (setpointTemp > TEMP_MAX) setpointTemp = TEMP_MAX;
        tempController.setSetpoint(setpointTemp);
        modeStartTime = millis(); // Reset timeout when adjusting
        break;
      case SET_HUMIDITY_MODE:
        setpointHumidity += HUMIDITY_STEP;
        if (setpointHumidity > HUMIDITY_MAX) setpointHumidity = HUMIDITY_MAX;
        modeStartTime = millis(); // Reset timeout when adjusting
        break;
      case NORMAL_MODE:
        // In normal mode, button 2 starts/stops drying (handled elsewhere)
        break;
    }
  }
  
  // Button 3: Decrease setpoint
  if (currentMode != NORMAL_MODE && isButtonPressed(button3, button3LastState, button3LastPress)) {
    switch (currentMode) {
      case SET_TEMP_MODE:
        setpointTemp -= TEMP_STEP;
        if (setpointTemp < TEMP_MIN) setpointTemp = TEMP_MIN;
        tempController.setSetpoint(setpointTemp);
        modeStartTime = millis(); // Reset timeout when adjusting
        break;
      case SET_HUMIDITY_MODE:
        setpointHumidity -= HUMIDITY_STEP;
        if (setpointHumidity < HUMIDITY_MIN) setpointHumidity = HUMIDITY_MIN;
        modeStartTime = millis(); // Reset timeout when adjusting
        break;
      default:
        break;
    }
  }
}

// Reset WiFi credentials and restart
void resetWiFiCredentials() {
  lcd.clear();
  lcd.print(0, 0, "Resetting WiFi...");
  lcd.print(0, 1, "Please wait...");
  
  wifiManager.reset();
  delay(2000);
  
  lcd.clear();
  lcd.print(0, 0, "WiFi Reset!");
  lcd.print(0, 1, "Restarting...");
  delay(2000);
  
  ESP.restart();
}

// Factory reset - clear all stored data
void factoryReset() {
  lcd.clear();
  lcd.print(0, 0, "Factory Reset");
  lcd.print(0, 1, "Clearing data...");
  
  // Clear pairing data
  clearPairingData();
  
  // Clear WiFi credentials
  wifiManager.reset();
  
  delay(2000);
  lcd.clear();
  lcd.print(0, 0, "Reset Complete!");
  lcd.print(0, 1, "Restarting...");
  delay(2000);
  
  ESP.restart();
}

// Check if humidity target is reached (dryer should stop)
bool isHumidityTargetReached() {
  return humidity <= setpointHumidity;
}

// Control drying logic
void controlDrying() {
  // ALWAYS turn off relays first if drying is not active
  if (!dryingActive) {
    // Force turn off all heating and disable PID when not drying
    relay1.off();
    relay2.off();
    tempController.setMode(false);  // Set PID to manual mode
    return;
  }
  
  // Enable PID when drying is active
  tempController.setMode(true);  // Set PID to automatic mode
  
  // Check if humidity target is reached
  if (isHumidityTargetReached()) {
    dryingActive = false;
    relay1.off();
    relay2.off();
    tempController.setMode(false);  // Disable PID
    
    // Display completion message
    lcd.clear();
    lcd.print(0, 0, "Drying Complete!");
    lcd.print(0, 1, "Target Reached");
    delay(2000);
    return;
  }
  
  // Update PID controller with current temperature and compute output
  bool pidComputed = tempController.compute(temperature);
  
  if (pidComputed) {
    // Get PID recommendation for heater control
    bool shouldHeat = tempController.shouldHeatOn();
    double pidOutput = tempController.getOutput();
    
    // Control relay1 (main heater) based on PID output
    if (shouldHeat && dryingActive) {  // Double-check dryingActive
      relay1.on();
      Serial.print("PID Heating ON - Output: ");
      Serial.print(pidOutput);
      Serial.println("%");
    } else {
      relay1.off();
      Serial.print("PID Heating OFF - Output: ");
      Serial.print(pidOutput);
      Serial.println("%");
    }
    
    // Only run fan (relay2) during active drying for air circulation
    if (dryingActive) {
      relay2.on();
    } else {
      relay2.off();
    }
  } else {
    // If PID didn't compute, ensure relays are off when not drying
    if (!dryingActive) {
      relay1.off();
      relay2.off();
    }
  }
}

// Update LCD display based on current mode
void updateDisplay() {
  String line0 = "";
  String line1 = "";
  String line2 = "";
  String line3 = "";
  
  switch (currentMode) {
    case SET_TEMP_MODE:
      line0 = "Set Temperature:";
      line1 = String(setpointTemp, 1) + "C";
      line2 = "Btn2:+ Btn3:-";
      break;
      
    case SET_HUMIDITY_MODE:
      line0 = "Set Humidity:";
      line1 = String(setpointHumidity, 1) + "%";
      line2 = "Btn2:+ Btn3:-";
      break;
      
    default: // NORMAL_MODE
      line0 = dryingActive ? "Drying: ON " : "Drying: OFF";
      char buf[21];
      snprintf(buf, sizeof(buf), "T:%2.1f H:%2.1f", temperature, humidity);
      line1 = String(buf);
      break;
  }
  
  // Only update lines that changed
  if (line0 != lastLcdLine0) {
    lcd.print(0, 0, "                    "); // Clear line with spaces
    lcd.print(0, 0, line0.c_str());
    lastLcdLine0 = line0;
  }
  if (line1 != lastLcdLine1) {
    lcd.print(0, 1, "                    ");
    lcd.print(0, 1, line1.c_str());
    lastLcdLine1 = line1;
  }
  if (line2 != lastLcdLine2) {
    lcd.print(0, 2, "                    ");
    lcd.print(0, 2, line2.c_str());
    lastLcdLine2 = line2;
  }
  if (line3 != lastLcdLine3) {
    lcd.print(0, 3, "                    ");
    lcd.print(0, 3, line3.c_str());
    lastLcdLine3 = line3;
  }
}

void runTestMenu() {
  int testIndex = 0;
  const int numTests = 3;
  void (*tests[])() = {testDHT22, testSSR, testLCD};
  const char* testNames[] = {"DHT22", "SSR", "LCD"};
  lcd.clear();
  lcd.print(0, 0, "Test Mode");
  lcd.print(0, 1, "Btn: Next Test");
  delay(1500);
  while (true) {
    lcd.clear();
    lcd.print(0, 0, String("Test: ") + testNames[testIndex]);
    lcd.print(0, 1, "Btn: Run Test");
    // Wait for button press to run test
    while (!button1.isPressed()) {
      delay(50);
    }
    tests[testIndex]();
    // Wait for button release
    while (button1.isPressed()) {
      delay(50);
    }
    // Next test
    testIndex = (testIndex + 1) % numTests;
    lcd.clear();
    lcd.print(0, 0, "Btn: Next Test");
    delay(500);
  }
}

void setup() {
  Serial.begin(115200);
  randomSeed(analogRead(0));
  
  // Initialize components
  button1.begin();
  button2.begin();
  button3.begin();
  dht.begin();
  relay1.begin();
  relay2.begin();
  lcd.begin();
  tempController.begin();
  
  lcd.clear();
  lcd.print(0, 0, "Rice Dryer v1.0");
  lcd.print(0, 1, "Initializing...");
  delay(2000);
  
  // Initialize PID with current setpoint
  tempController.setSetpoint(setpointTemp);
  
  // Display initial setpoints
  lcd.clear();
  lcd.print(0, 0, "Temp: " + String(setpointTemp, 1) + "C");
  lcd.print(0, 1, "Humid: " + String(setpointHumidity, 1) + "%");
  delay(2000);
  
  // If button1 held at startup, enter test mode
  if (button1.isPressed()) {
    runTestMenu();
  }
  
  // Get device ID
  deviceId = getDeviceId();
  
  // Initialize WiFi
  if (!initWiFi()) {
    lcd.clear();
    lcd.print(0, 0, "WiFi Setup Failed");
    lcd.print(0, 1, "Check Settings");
    while (1) delay(1000);
  }
  
  // Initialize Firebase
  if (!initFirebase()) {
    lcd.clear();
    lcd.print(0, 0, "Firebase Failed");
    lcd.print(0, 1, "Check Config");
    delay(5000);
  }
  
  lcd.clear();
  if (devicePaired) {
    lcd.print(0, 0, "Rice Dryer Ready");
    lcd.print(0, 1, "Press Btn to Start");
  } else {
    lcd.print(0, 0, "Pairing Code:");
    lcd.print(0, 1, pairingCode.c_str());
    lcd.print(0, 2, "MAC Address:");
    lcd.print(0, 3, deviceId.c_str());
  }
}

void loop() {
  // Check WiFi connection
  if (!wifiManager.isConnected()) {
    wifiConnected = false;
    lcd.clear();
    lcd.print(0, 0, "WiFi Disconnected");
    lcd.print(0, 1, "Reconnecting...");
    wifiManager.reconnect();
    delay(5000);
    return;
  } else if (!wifiConnected) {
    wifiConnected = true;
    lcd.clear();
    lcd.print(0, 0, "WiFi Reconnected!");
    delay(1000);
  }
  
  // Check if still in pairing mode
  if (!devicePaired) {
    unsigned long long currentTime = getTimestamp();
    if (currentTime > pairingCodeExpiry) {
      // Regenerate pairing code
      startPairingMode();
    }
    
    // Periodically check if device was paired via Android app
    static unsigned long lastPairingCheck = 0;
    if (millis() - lastPairingCheck > 2000) { // Check every 2 seconds
      checkPairingStatus();
      lastPairingCheck = millis();
    }
    
    // Only update if changed (prevent flickering)
    String line0 = "Pairing Code:";
    String line1 = String(pairingCode.c_str());
    String line2 = "MAC Address:";
    String line3 = String(deviceId.c_str());
    
    if (line0 != lastLcdLine0) {
      lcd.print(0, 0, line0.c_str());
      lastLcdLine0 = line0;
    }
    if (line1 != lastLcdLine1) {
      lcd.print(0, 1, line1.c_str());
      lastLcdLine1 = line1;
    }
    if (line2 != lastLcdLine2) {
      lcd.print(0, 2, line2.c_str());
      lastLcdLine2 = line2;
    }
    if (line3 != lastLcdLine3) {
      lcd.print(0, 3, line3.c_str());
      lastLcdLine3 = line3;
    }
    
    delay(500);
    return;
  }
  
  // === BUTTON HANDLING ===
  
  // Button 1: Toggle setting mode (Normal -> Set Temp -> Set Humidity -> Normal)
  if (isButtonPressed(button1, button1LastState, button1LastPress)) {
    if (currentMode == NORMAL_MODE) {
      currentMode = SET_TEMP_MODE;
      modeStartTime = millis();
      lcd.clear();
      lcd.print(0, 0, "Set Temp Mode");
      lcd.print(0, 1, "Btn2:+ Btn3:-");
      lcd.print(0, 2, "Btn1: Next Mode");
      delay(800);
    } else if (currentMode == SET_TEMP_MODE) {
      currentMode = SET_HUMIDITY_MODE;
      modeStartTime = millis();
      lcd.clear();
      lcd.print(0, 0, "Set Humidity Mode");
      lcd.print(0, 1, "Btn2:+ Btn3:-");
      lcd.print(0, 2, "Btn1: Next Mode");
      delay(800);
    } else {
      currentMode = NORMAL_MODE;
      lcd.clear();
      lcd.print(0, 0, "Normal Mode");
      lcd.print(0, 1, "Btn2: Start/Stop");
      delay(800);
    }
  }
  
  // === SETPOINT ADJUSTMENT (in setting modes) ===
  // Must be BEFORE normal mode button handling to prevent conflicts
  if (currentMode != NORMAL_MODE) {
    handleSetpointAdjustment();
  }
  
  // Button 2 & 3: Only in NORMAL mode for Start/Stop and WiFi reset
  if (currentMode == NORMAL_MODE) {
    // In normal mode, Button 2: Start/Stop drying
    if (isButtonPressed(button2, button2LastState, button2LastPress)) {
      dryingActive = !dryingActive;
      
      if (dryingActive) {
        lcd.clear();
        lcd.print(0, 0, "Starting Dryer...");
        lcd.print(0, 1, "Target T: " + String(setpointTemp, 1) + "C");
        lcd.print(0, 2, "Target H: " + String(setpointHumidity, 1) + "%");
        delay(1500);
      } else {
        // Force stop - immediately turn off relays
        relay1.off();
        relay2.off();
        tempController.setMode(false);
        lcd.clear();
        lcd.print(0, 0, "Stopping Dryer...");
        lcd.print(0, 1, "Force Stop");
        lcd.print(0, 2, "Relays OFF");
        delay(1000);
      }
    }
    
    // In normal mode, Button 3: Factory Reset (hold 5 seconds)
    if (isButtonPressed(button3, button3LastState, button3LastPress)) {
      unsigned long holdStart = millis();
      lcd.clear();
      lcd.print(0, 0, "Hold 5s for");
      lcd.print(0, 1, "Factory Reset");
      lcd.print(0, 2, "WiFi+Pairing");
      
      while (button3.isPressed() && (millis() - holdStart < 5000)) {
        // Show countdown
        int remaining = 5 - ((millis() - holdStart) / 1000);
        lcd.print(0, 3, String(remaining) + "s remaining...");
        delay(100);
      }
      
      if (millis() - holdStart >= 5000) {
        factoryReset();
      } else {
        lcd.clear();
        lcd.print(0, 0, "Reset Cancelled");
        delay(1000);
      }
    }
  }
  
  // === SETTING MODE TIMEOUT ===
  if (currentMode != NORMAL_MODE && (millis() - modeStartTime > MODE_TIMEOUT)) {
    currentMode = NORMAL_MODE;
    lcd.clear();
    lcd.print(0, 0, "Timeout - Normal");
    delay(500);
  }
  
  // === SENSOR READING ===
  if (millis() - lastSensorRead > SENSOR_INTERVAL) {
    temperature = dht.readTemperature();
    humidity = dht.readHumidity();
    lastSensorRead = millis();
    
    // Validate sensor readings
    if (isnan(temperature) || isnan(humidity)) {
      lcd.clear();
      lcd.print(0, 0, "Sensor Error!");
      lcd.print(0, 1, "Check DHT22");
      delay(1000);
      return;
    }
  }
  
  // === DRYING CONTROL ===
  controlDrying();
  
  // === FIREBASE COMMUNICATION ===
  if (millis() - lastFirebaseUpdate > FIREBASE_UPDATE_INTERVAL) {
    sendDataToFirebase();
    lastFirebaseUpdate = millis();
  }
  
  if (millis() - lastHistoryLog > HISTORY_LOG_INTERVAL) {
    logHistoricalData();
    lastHistoryLog = millis();
  }
  
  if (millis() - lastCommandCheck > COMMAND_CHECK_INTERVAL) {
    checkRemoteCommands();
    lastCommandCheck = millis();
  }
  
  // === DISPLAY UPDATE ===
  updateDisplay();
  
  delay(200);
}
