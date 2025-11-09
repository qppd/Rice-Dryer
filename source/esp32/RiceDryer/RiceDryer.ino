// RiceDryer.ino
// Main Arduino sketch for RiceDryer ESP32 project

// WiFi and Network
#include <WiFi.h>
#include <time.h>

// Firebase
#include <Firebase_ESP_Client.h>
#include <addons/TokenHelper.h>
#include <addons/RTDBHelper.h>

// OTA Updates
#include <ArduinoOTA.h>

// Utilities
#include <ArduinoJson.h>
#include <EEPROM.h>

// Custom modules
#include "WiFiManagerCustom.h"
#include "FirebaseConfig.h"

// Include component headers
#include "Button.h"
#include "DHT22Sensor.h"
#include "Potentiometer.h"
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

// Device Information
String deviceId;
String pairingCode = "";
bool devicePaired = false;
unsigned long pairingCodeExpiry = 0;

// Components
Button button1(BUTTON_1);
Button button2(BUTTON_2);
Button button3(BUTTON_3);
DHT22Sensor dht(DHT_PIN);
Potentiometer pot(POT_1);
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
int potValue = 0;
float setpointTemp = 40.0;     // Default target temperature
float setpointHumidity = 20.0; // Default target humidity (stop when reached)
bool wifiConnected = false;
bool firebaseConnected = false;

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
  return true;
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
  json.set("lastBoot", millis());
  
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
  String path = "/devices/" + deviceId + "/deviceInfo/pairedTo";
  if (Firebase.RTDB.getString(&fbdo, path.c_str())) {
    String pairedTo = fbdo.stringData();
    if (pairedTo != "" && pairedTo != "null") {
      devicePaired = true;
      lcd.clear();
      lcd.print(0, 0, "Device Paired!");
      delay(1000);
    } else {
      // Generate pairing code
      startPairingMode();
    }
  }
}

// Start pairing mode
void startPairingMode() {
  pairingCode = generatePairingCode();
  pairingCodeExpiry = millis() + PAIRING_CODE_VALIDITY;
  
  // Store pairing code in Firebase
  String path = "/devicePairing/" + pairingCode;
  FirebaseJson json;
  json.set("deviceId", deviceId);
  json.set("expiresAt", pairingCodeExpiry);
  json.set("used", false);
  
  Firebase.RTDB.setJSON(&fbdo, path.c_str(), &json);
  
  lcd.clear();
  lcd.print(0, 0, "Pairing Code:");
  lcd.print(0, 1, pairingCode.c_str());
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
  json.set("lastUpdate", millis());
  
  if (!Firebase.RTDB.setJSON(&fbdo, path.c_str(), &json)) {
    Serial.println("Failed to send data: " + fbdo.errorReason());
  }
}

// Log historical data
void logHistoricalData() {
  if (!firebaseConnected || !devicePaired) return;
  
  String timestamp = String(millis());
  String path = "/devices/" + deviceId + "/history/" + timestamp;
  
  FirebaseJson json;
  json.set("temperature", temperature);
  json.set("humidity", humidity);
  json.set("setpointTemp", setpointTemp);
  json.set("setpointHumidity", setpointHumidity);
  json.set("relay1Status", relay1.isOn());
  json.set("relay2Status", relay2.isOn());
  json.set("dryingActive", dryingActive);
  json.set("pidOutput", tempController.getOutput());
  
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
  json.set("timestamp", millis());
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

void testPotentiometer() {
  lcd.clear();
  lcd.print(0, 0, "Testing Pot...");
  int val = pot.readValue();
  char buf[17];
  snprintf(buf, sizeof(buf), "Value: %4d", val);
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

// Handle potentiometer input based on current mode
void handlePotentiometer() {
  potValue = pot.readValue();
  
  switch (currentMode) {
    case SET_TEMP_MODE:
      setpointTemp = map(potValue, 0, 4095, 30, 80); // Temperature range: 30-80°C
      tempController.setSetpoint(setpointTemp);       // Update PID setpoint
      break;
    case SET_HUMIDITY_MODE:
      setpointHumidity = map(potValue, 0, 4095, 10, 50); // Humidity range: 10-50%
      break;
    default:
      // In normal mode, pot doesn't change setpoints
      break;
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

// Check if humidity target is reached (dryer should stop)
bool isHumidityTargetReached() {
  return humidity <= setpointHumidity;
}

// Control drying logic
void controlDrying() {
  if (!dryingActive) {
    // Turn off all heating and disable PID when not drying
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
    if (shouldHeat) {
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
    
    // Always run fan (relay2) during drying for air circulation
    relay2.on();
  }
}

// Update LCD display based on current mode
void updateDisplay() {
  lcd.clear();
  
  switch (currentMode) {
    case SET_TEMP_MODE:
      lcd.print(0, 0, "Set Temperature:");
      lcd.print(0, 1, String(setpointTemp, 1) + "C (Use Pot)");
      break;
      
    case SET_HUMIDITY_MODE:
      lcd.print(0, 0, "Set Humidity:");
      lcd.print(0, 1, String(setpointHumidity, 1) + "% (Use Pot)");
      break;
      
    default: // NORMAL_MODE
      lcd.print(0, 0, dryingActive ? "Drying: ON " : "Drying: OFF");
      char buf[17];
      snprintf(buf, sizeof(buf), "T:%2.1f H:%2.1f", temperature, humidity);
      lcd.print(0, 1, String(buf));
      break;
  }
}

void runTestMenu() {
  int testIndex = 0;
  const int numTests = 4;
  void (*tests[])() = {testDHT22, testPotentiometer, testSSR, testLCD};
  const char* testNames[] = {"DHT22", "Potentiometer", "SSR", "LCD"};
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
  pot.begin();
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
  
  // Setup OTA
  ArduinoOTA.setHostname("RiceDryer");
  ArduinoOTA.begin();
  
  lcd.clear();
  if (devicePaired) {
    lcd.print(0, 0, "Rice Dryer Ready");
    lcd.print(0, 1, "Press Btn to Start");
  } else {
    lcd.print(0, 0, "Pairing Code:");
    lcd.print(0, 1, pairingCode.c_str());
  }
}

void loop() {
  // Handle OTA updates
  ArduinoOTA.handle();
  
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
    if (millis() > pairingCodeExpiry) {
      // Regenerate pairing code
      startPairingMode();
    }
    
    // Check if device was paired
    checkPairingStatus();
    
    lcd.print(0, 0, "Pairing Code:");
    lcd.print(0, 1, pairingCode.c_str());
    delay(500);
    return;
  }
  
  // === BUTTON HANDLING ===
  
  // Button 1: Toggle setting mode (temp/humidity)
  if (isButtonPressed(button1, button1LastState, button1LastPress)) {
    if (currentMode == NORMAL_MODE) {
      currentMode = SET_TEMP_MODE;
      modeStartTime = millis();
      lcd.clear();
      lcd.print(0, 0, "Setting Temp Mode");
      delay(500);
    } else if (currentMode == SET_TEMP_MODE) {
      currentMode = SET_HUMIDITY_MODE;
      modeStartTime = millis();
      lcd.clear();
      lcd.print(0, 0, "Setting Humid Mode");
      delay(500);
    } else {
      currentMode = NORMAL_MODE;
      lcd.clear();
      lcd.print(0, 0, "Normal Mode");
      delay(500);
    }
  }
  
  // Button 2: Start/Stop drying
  if (isButtonPressed(button2, button2LastState, button2LastPress)) {
    dryingActive = !dryingActive;
    
    if (dryingActive) {
      lcd.clear();
      lcd.print(0, 0, "Starting Dryer...");
      lcd.print(0, 1, "Target H: " + String(setpointHumidity, 1) + "%");
      delay(1000);
    } else {
      lcd.clear();
      lcd.print(0, 0, "Stopping Dryer...");
      lcd.print(0, 1, "Force Stop");
      delay(1000);
    }
    
    // Return to normal mode when starting/stopping
    currentMode = NORMAL_MODE;
  }
  
  // Button 3: Reset WiFi credentials
  if (isButtonPressed(button3, button3LastState, button3LastPress)) {
    // Hold button for 3 seconds to confirm reset
    unsigned long holdStart = millis();
    lcd.clear();
    lcd.print(0, 0, "Hold 3s to Reset");
    lcd.print(0, 1, "WiFi Credentials");
    
    while (button3.isPressed() && (millis() - holdStart < 3000)) {
      delay(100);
    }
    
    if (millis() - holdStart >= 3000) {
      resetWiFiCredentials();
    } else {
      lcd.clear();
      lcd.print(0, 0, "Reset Cancelled");
      delay(1000);
    }
  }
  
  // === SETTING MODE TIMEOUT ===
  if (currentMode != NORMAL_MODE && (millis() - modeStartTime > MODE_TIMEOUT)) {
    currentMode = NORMAL_MODE;
    lcd.clear();
    lcd.print(0, 0, "Timeout - Normal");
    delay(500);
  }
  
  // === POTENTIOMETER HANDLING ===
  handlePotentiometer();
  
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
