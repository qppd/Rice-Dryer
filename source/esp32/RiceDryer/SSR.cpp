#include "SSR.h"
#include <Arduino.h>
SSR::SSR(uint8_t pin) : relayPin(pin), state(false) {}
void SSR::begin() { pinMode(relayPin, OUTPUT); off(); }
void SSR::on() { digitalWrite(relayPin, HIGH); state = true; }
void SSR::off() { digitalWrite(relayPin, LOW); state = false; }
bool SSR::isOn() { return state; }