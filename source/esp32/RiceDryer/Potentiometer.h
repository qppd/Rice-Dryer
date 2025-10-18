#ifndef POTENTIOMETER_H
#define POTENTIOMETER_H
class Potentiometer {
public:
    Potentiometer(uint8_t pin);
    void begin();
    int readValue();
private:
    uint8_t potPin;
};
#endif