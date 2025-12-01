#ifndef SSR_H
#define SSR_H

#include <Arduino.h>

class SSR {
public:
    SSR(uint8_t pin);
    void begin();
    void on();
    void off();
    bool isOn();
private:
    uint8_t relayPin;
    bool state;
    void sendPulse();
    static const int PULSE_DURATION = 50; // milliseconds for latching relay pulse
};
#endif