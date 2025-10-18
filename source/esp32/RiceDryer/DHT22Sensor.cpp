#include "DHT22Sensor.h"
DHT22Sensor::DHT22Sensor(uint8_t pin) : dht(pin, DHT22), sensorPin(pin) {}
void DHT22Sensor::begin() { dht.begin(); }
float DHT22Sensor::readTemperature() { return dht.readTemperature(); }
float DHT22Sensor::readHumidity() { return dht.readHumidity(); }