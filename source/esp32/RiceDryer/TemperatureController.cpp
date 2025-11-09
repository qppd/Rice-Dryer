#include "TemperatureController.h"
#include <Arduino.h>

// Static constants
const double TemperatureController::OUTPUT_MIN = 0.0;
const double TemperatureController::OUTPUT_MAX = 100.0;
const double TemperatureController::HEATING_THRESHOLD = 10.0; // Turn on heater if output > 10%

TemperatureController::TemperatureController() {
    // Initialize variables
    currentTemperature = 0.0;
    temperatureSetpoint = 40.0;  // Default setpoint
    pidOutput = 0.0;
    
    // Default PID parameters (tuned for rice dryer system)
    // These values may need adjustment based on your specific heating system
    kp = 2.0;   // Proportional gain - how aggressively to respond to current error
    ki = 0.1;   // Integral gain - how aggressively to respond to historic error
    kd = 0.5;   // Derivative gain - how aggressively to respond to rate of error change
    
    // Create PID controller instance
    pidController = new PID(&currentTemperature, &pidOutput, &temperatureSetpoint, kp, ki, kd, DIRECT);
}

void TemperatureController::begin() {
    // Set PID to automatic mode
    pidController->SetMode(AUTOMATIC);
    
    // Set output limits (0-100%)
    pidController->SetOutputLimits(OUTPUT_MIN, OUTPUT_MAX);
    
    // Set sample time (how often to compute - 1000ms = 1 second)
    pidController->SetSampleTime(1000);
    
    Serial.println("Temperature PID Controller initialized");
    Serial.print("PID Parameters - Kp: ");
    Serial.print(kp);
    Serial.print(", Ki: ");
    Serial.print(ki);
    Serial.print(", Kd: ");
    Serial.println(kd);
}

void TemperatureController::setPIDParameters(double newKp, double newKi, double newKd) {
    kp = newKp;
    ki = newKi;
    kd = newKd;
    
    // Update PID controller with new parameters
    pidController->SetTunings(kp, ki, kd);
    
    Serial.print("PID Parameters updated - Kp: ");
    Serial.print(kp);
    Serial.print(", Ki: ");
    Serial.print(ki);
    Serial.print(", Kd: ");
    Serial.println(kd);
}

void TemperatureController::setSetpoint(double setpoint) {
    temperatureSetpoint = setpoint;
    Serial.print("Temperature setpoint changed to: ");
    Serial.print(setpoint);
    Serial.println("°C");
}

bool TemperatureController::compute(double currentTemp) {
    // Update current temperature
    currentTemperature = currentTemp;
    
    // Validate temperature reading
    if (isnan(currentTemp) || currentTemp < -50 || currentTemp > 150) {
        Serial.println("Invalid temperature reading for PID");
        return false;
    }
    
    // Compute PID output
    bool computed = pidController->Compute();
    
    if (computed) {
        Serial.print("PID Compute - Temp: ");
        Serial.print(currentTemp);
        Serial.print("°C, Setpoint: ");
        Serial.print(temperatureSetpoint);
        Serial.print("°C, Output: ");
        Serial.print(pidOutput);
        Serial.println("%");
    }
    
    return computed;
}

double TemperatureController::getOutput() {
    return pidOutput;
}

double TemperatureController::getSetpoint() {
    return temperatureSetpoint;
}

void TemperatureController::setMode(bool automatic) {
    if (automatic) {
        pidController->SetMode(AUTOMATIC);
        Serial.println("PID Controller set to AUTOMATIC mode");
    } else {
        pidController->SetMode(MANUAL);
        Serial.println("PID Controller set to MANUAL mode");
    }
}

bool TemperatureController::shouldHeatOn() {
    // Turn on heater if PID output is above threshold
    bool heatOn = pidOutput > HEATING_THRESHOLD;
    
    // Additional safety check - don't heat if already above setpoint + 2°C (overshoot protection)
    if (currentTemperature > (temperatureSetpoint + 2.0)) {
        heatOn = false;
        Serial.println("Overshoot protection: Heater disabled");
    }
    
    return heatOn;
}

void TemperatureController::reset() {
    // Reset integral term to prevent windup
    pidController->SetMode(MANUAL);
    pidOutput = 0.0;
    pidController->SetMode(AUTOMATIC);
    
    Serial.println("PID Controller reset (integral windup cleared)");
}