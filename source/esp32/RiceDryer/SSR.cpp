#include "SSR.h"
#include <Arduino.h>

SSR::SSR(uint8_t pin) : relayPin(pin), state(false) {}

void SSR::begin() { 
    pinMode(relayPin, OUTPUT); 
    digitalWrite(relayPin, HIGH); // Default HIGH for low-trigger relay (idle)
    state = false;
}

void SSR::sendPulse() {
    digitalWrite(relayPin, LOW);  // Trigger pulse (low trigger)
    delay(PULSE_DURATION);
    digitalWrite(relayPin, HIGH); // Return to idle state
}

void SSR::on() { 
    if (!state) { // Only send pulse if not already on
        sendPulse();
        state = true;
    }
}

void SSR::off() { 
    if (state) { // Only send pulse if not already off
        sendPulse();
        state = false;
    }
}

bool SSR::isOn() { return state; }