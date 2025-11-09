#ifndef BUTTON_H
#define BUTTON_H

#include <Arduino.h>

class Button {
public:
    Button(uint8_t pin);
    void begin();
    bool isPressed();
private:
    uint8_t buttonPin;
};
#endif