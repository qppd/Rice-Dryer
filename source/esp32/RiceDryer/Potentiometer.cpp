#include "Potentiometer.h"
#include <Arduino.h>
Potentiometer::Potentiometer(uint8_t pin) : potPin(pin) {}
void Potentiometer::begin() { /* No setup needed for analog input */ }
int Potentiometer::readValue() { return analogRead(potPin); }