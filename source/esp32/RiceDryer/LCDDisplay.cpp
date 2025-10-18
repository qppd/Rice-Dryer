#include "LCDDisplay.h"
LCDDisplay::LCDDisplay(uint8_t addr, uint8_t cols, uint8_t rows)
    : lcd(addr, cols, rows), columns(cols), rows(rows) {}
void LCDDisplay::begin() { lcd.init(); lcd.backlight(); lcd.clear(); }
void LCDDisplay::print(uint8_t col, uint8_t row, const String &text) {
    lcd.setCursor(col, row); lcd.print(text);
}
void LCDDisplay::clear() { lcd.clear(); }