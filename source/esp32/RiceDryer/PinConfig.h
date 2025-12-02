// PinConfig.h
// Centralized pin configuration for RiceDryer ESP32

#ifndef PINCONFIG_H
#define PINCONFIG_H

// Button pins (input with internal pull-up)
const uint8_t BUTTON_1 = 17;
const uint8_t BUTTON_2 = 16;
const uint8_t BUTTON_3 = 4;

// Relay pins (output)
const uint8_t RELAY_1 = 19;
const uint8_t RELAY_2 = 18;

// DHT22 sensor pin
const uint8_t DHT_PIN = 23;

// LCD configuration
const uint8_t LCD_ADDR = 0x27;
const uint8_t LCD_COLS = 20;
const uint8_t LCD_ROWS = 4;

// Legacy compatibility (for existing code that uses these names)
const uint8_t BUTTON_PIN = BUTTON_1;  // Default to first button
const uint8_t SSR_PIN = RELAY_1;     // Default to first relay

#endif // PINCONFIG_H
