// RiceDryer.ino
// Main Arduino sketch for RiceDryer ESP32 project

// Include component headers
#include "Button.h"
#include "DHT22Sensor.h"
#include "Potentiometer.h"
#include "SSR.h"
#include "LCDDisplay.h"


// Pin configuration
#include "PinConfig.h"

// Components
Button button(BUTTON_PIN);
DHT22Sensor dht(DHT_PIN);
Potentiometer pot(POT_PIN);
SSR ssr(SSR_PIN);
LCDDisplay lcd(LCD_ADDR, LCD_COLS, LCD_ROWS);

// Logic variables
bool dryingActive = false;
unsigned long lastSensorRead = 0;
const unsigned long SENSOR_INTERVAL = 2000; // ms
float temperature = 0.0;
float humidity = 0.0;
int potValue = 0;
float setpoint = 40.0; // Default target temp

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
  button.begin();
  dht.begin();
  pot.begin();
  ssr.begin();
  lcd.begin();
  // If button held at startup, enter test mode
  if (button.isPressed()) {
    runTestMenu();
  }
  lcd.print(0, 0, "Rice Dryer Ready");
  lcd.print(0, 1, "Press Btn to Start");
}

void loop() {
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

  // Update LCD display
  lcd.print(0, 0, dryingActive ? "Drying: ON " : "Drying: OFF");
  char buf[17];
  snprintf(buf, sizeof(buf), "T:%2.1f H:%2.1f S:%2.0f", temperature, humidity, setpoint);
  lcd.print(0, 1, String(buf));

  delay(200);
}
