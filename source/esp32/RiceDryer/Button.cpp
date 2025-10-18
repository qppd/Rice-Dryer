#include "Button.h"
#include <Arduino.h>
Button::Button(uint8_t pin) : buttonPin(pin) {}
void Button::begin() { pinMode(buttonPin, INPUT_PULLUP); }
bool Button::isPressed() { return digitalRead(buttonPin) == LOW; }