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
Button button(BUTTON_PIN);
DHT22Sensor dht(DHT_PIN);
Potentiometer pot(POT_PIN);
SSR ssr(SSR_PIN);
LCDDisplay lcd(LCD_ADDR, LCD_COLS, LCD_ROWS);

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
float setpoint = 40.0; // Default target temp
bool wifiConnected = false;
bool firebaseConnected = false;

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
  
  config.host = FirebaseConfig::getFirebaseHost();
  config.signer.tokens.legacy_token = FirebaseConfig::getFirebaseAuth();
  config.database_url = FirebaseConfig::getDatabaseURL();
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
  json.set("setpoint", setpoint);
  json.set("ssrStatus", ssr.isOn());
  json.set("dryingActive", dryingActive);
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
  json.set("setpoint", setpoint);
  json.set("ssrStatus", ssr.isOn());
  
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
          setpoint = jsonData.floatValue;
          acknowledgeCommand("SET_TEMP");
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
  lcd.print(0, 0, "Testing SSR...");
  lcd.print(0, 1, "ON for 2s");
  ssr.on();
  delay(2000);
  lcd.print(0, 1, "OFF for 2s");
  ssr.off();
  delay(2000);
}

void testLCD() {
  lcd.clear();
  lcd.print(0, 0, "Testing LCD...");
  lcd.print(0, 1, "Hello World!");
  delay(2000);
  lcd.clear();
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
    while (!button.isPressed()) {
      delay(50);
    }
    tests[testIndex]();
    // Wait for button release
    while (button.isPressed()) {
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
  button.begin();
  dht.begin();
  pot.begin();
  ssr.begin();
  lcd.begin();
  
  lcd.clear();
  lcd.print(0, 0, "Rice Dryer v1.0");
  lcd.print(0, 1, "Initializing...");
  delay(2000);
  
  // If button held at startup, enter test mode
  if (button.isPressed()) {
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
  
  // Read button to toggle drying
  static bool lastButtonState = false;
  bool buttonState = button.isPressed();
  if (buttonState && !lastButtonState) {
    dryingActive = !dryingActive;
    lcd.clear();
    lcd.print(0, 0, dryingActive ? "Drying: ON" : "Drying: OFF");
    lcd.print(0, 1, "Temp/Humid/Setpt");
    delay(300); // Debounce
  }
  lastButtonState = buttonState;

  // Read potentiometer for setpoint
  potValue = pot.readValue();
  setpoint = map(potValue, 0, 4095, 30, 60); // Setpoint range: 30-60°C

  // Read sensors periodically
  if (millis() - lastSensorRead > SENSOR_INTERVAL) {
    temperature = dht.readTemperature();
    humidity = dht.readHumidity();
    lastSensorRead = millis();
  }

  // Control SSR based on drying logic
  if (dryingActive) {
    if (temperature < setpoint) {
      ssr.on();
    } else {
      ssr.off();
    }
  } else {
    ssr.off();
  }
  
  // Send data to Firebase periodically
  if (millis() - lastFirebaseUpdate > FIREBASE_UPDATE_INTERVAL) {
    sendDataToFirebase();
    lastFirebaseUpdate = millis();
  }
  
  // Log historical data
  if (millis() - lastHistoryLog > HISTORY_LOG_INTERVAL) {
    logHistoricalData();
    lastHistoryLog = millis();
  }
  
  // Check for remote commands
  if (millis() - lastCommandCheck > COMMAND_CHECK_INTERVAL) {
    checkRemoteCommands();
    lastCommandCheck = millis();
  }

  // Update LCD display
  lcd.print(0, 0, dryingActive ? "Drying: ON " : "Drying: OFF");
  char buf[17];
  snprintf(buf, sizeof(buf), "T:%2.1f H:%2.1f S:%2.0f", temperature, humidity, setpoint);
  lcd.print(0, 1, String(buf));

  delay(200);
}
