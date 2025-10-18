#ifndef LCDDISPLAY_H
#define LCDDISPLAY_H
#include <LiquidCrystal_I2C.h>
class LCDDisplay {
public:
    LCDDisplay(uint8_t addr, uint8_t cols, uint8_t rows);
    void begin();
    void print(uint8_t col, uint8_t row, const String &text);
    void clear();
private:
    LiquidCrystal_I2C lcd;
    uint8_t columns;
    uint8_t rows;
};
#endif