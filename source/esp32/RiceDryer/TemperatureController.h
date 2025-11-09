#ifndef TEMPERATURECONTROLLER_H
#define TEMPERATURECONTROLLER_H

#include <PID_v1.h>

class TemperatureController {
public:
    TemperatureController();
    
    // Initialize PID controller
    void begin();
    
    // Set PID parameters
    void setPIDParameters(double kp, double ki, double kd);
    
    // Set temperature setpoint
    void setSetpoint(double setpoint);
    
    // Update temperature input and compute PID output
    bool compute(double currentTemp);
    
    // Get PID output (0-100% for PWM or time-based control)
    double getOutput();
    
    // Get current setpoint
    double getSetpoint();
    
    // Enable/disable PID controller
    void setMode(bool automatic);
    
    // Check if heater should be on (for relay control)
    bool shouldHeatOn();
    
    // Reset PID integral term (useful when switching modes)
    void reset();

private:
    // PID variables
    double currentTemperature;
    double temperatureSetpoint;
    double pidOutput;
    
    // PID parameters (tuned for rice dryer heating system)
    double kp;  // Proportional gain
    double ki;  // Integral gain  
    double kd;  // Derivative gain
    
    // PID controller instance
    PID* pidController;
    
    // Output limits
    static const double OUTPUT_MIN;
    static const double OUTPUT_MAX;
    
    // Heating threshold (output percentage above which heater turns on)
    static const double HEATING_THRESHOLD;
};

#endif // TEMPERATURECONTROLLER_H