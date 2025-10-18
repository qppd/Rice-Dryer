# RiceDryer ESP32 Project

## Overview
RiceDryer is an ESP32-based automated rice drying system designed for precise environmental monitoring and control. It integrates temperature and humidity sensing, solid-state relay control, user input via buttons and potentiometers, and real-time feedback on a 20x4 I2C LCD display. The project is modular, with encapsulated C++ classes for each hardware component, ensuring maintainability and scalability.

This project is part of the QPPD (Quezon Province Programmers/Developers) portfolio, showcasing practical IoT and embedded systems development for agricultural applications.

## Table of Contents
- [Overview](#overview)
- [Features](#features)
- [System Architecture](#system-architecture)
- [Hardware Components](#hardware-components)
- [Software Components](#software-components)
- [Directory Structure](#directory-structure)
- [Getting Started](#getting-started)
- [Installation](#installation)
- [Usage](#usage)
- [Code Structure](#code-structure)
- [Technical Specifications](#technical-specifications)
- [Future Enhancements](#future-enhancements)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)
- [About the Developer](#about-the-developer)
- [Contact](#contact)

## Features
- **Real-time Environmental Monitoring**: DHT22 sensor provides accurate temperature and humidity measurements.
- **Automated Control System**: Two solid-state relays for controlling a heater and exhaust fans, enabling precise drying and ventilation cycles.
- **User Interface**: Interactive control via two tactile buttons for mode selection and system operation.
- **Adjustable Parameters**: Two potentiometers enable fine-tuning of temperature thresholds and drying duration.
- **Visual Feedback**: 20x4 I2C LCD display shows real-time status, sensor readings, and system parameters.
- **Modular Design**: Object-oriented architecture with encapsulated C++ classes for each hardware component.
- **Energy Efficient**: Optimized power consumption suitable for continuous operation.
- **Scalable**: Easy to expand with additional sensors or control mechanisms.

## System Architecture
The RiceDryer system follows a modular architecture:
1. **Sensor Layer**: DHT22 environmental sensor for data acquisition.
2. **Control Layer**: ESP32 microcontroller processing sensor data and executing control logic.
3. **Actuator Layer**: Solid-state relays managing high-power devices—a heater and exhaust fans—for automated drying and ventilation.
4. **Interface Layer**: Buttons, potentiometers, and LCD for user interaction.

## Hardware Components
- **ESP32 Development Board**: Main microcontroller with WiFi and Bluetooth capabilities.
- **DHT22 Temperature & Humidity Sensor**: Digital sensor with I2C communication.
- **2x Solid State Relays (SSR)**: High-current switching devices for AC/DC loads. One SSR is connected to a heater, and the other to exhaust fans, allowing independent control of heating and ventilation.
- **2x Tactile Push Buttons**: User input for mode selection and control.
- **2x Potentiometers**: Analog input devices for adjustable parameters.
- **20x4 I2C LCD Display**: Large character display for comprehensive status information.
- **Power Supply**: 5V regulated power for ESP32 and peripherals.
- **Jumper Wires and Breadboard**: For prototyping and connections.

## Software Components
- **Arduino IDE**: Development environment for ESP32 programming.
- **Adafruit DHT Sensor Library**: For DHT22 communication and data reading.
- **LiquidCrystal I2C Library**: For controlling the 20x4 LCD display.
- **Custom C++ Classes**: Encapsulated modules for each hardware component.

## Directory Structure
```
RICE_DRYER/
├── diagram/                    # System diagrams, schematics, and wiring layouts
├── model/                      # Data models, simulation files, and design documents
├── source/
│   └── esp32/
│       └── RiceDryer/
│           ├── RiceDryer.ino         # Main Arduino sketch
│           ├── DHT22Sensor.h         # DHT22 sensor header file
│           ├── DHT22Sensor.cpp       # DHT22 sensor implementation
│           ├── SSR.h                 # Solid State Relay header file
│           ├── SSR.cpp               # Solid State Relay implementation
│           ├── Button.h              # Tactile button header file
│           ├── Button.cpp            # Tactile button implementation
│           ├── Potentiometer.h       # Potentiometer header file
│           ├── Potentiometer.cpp     # Potentiometer implementation
│           ├── LCDDisplay.h          # LCD display header file
│           └── LCDDisplay.cpp        # LCD display implementation
└── README.md                   # Project documentation
```

## Getting Started
### Prerequisites
Before beginning, ensure you have:
- Basic knowledge of Arduino programming and C++
- Understanding of electronic circuits and components
- Arduino IDE installed (version 1.8.x or higher)
- ESP32 board support installed in Arduino IDE
- USB cable for ESP32 programming

### Hardware Setup
1. Connect the DHT22 sensor to a digital GPIO pin on the ESP32.
2. Wire both solid-state relays to separate GPIO pins.
3. Connect tactile buttons to GPIO pins with pull-up resistors.
4. Attach potentiometers to analog input pins.
5. Connect the I2C LCD display to SDA and SCL pins of the ESP32.
6. Ensure proper power supply and ground connections for all components.
7. Refer to the wiring diagram in the `diagram/` folder for detailed connections.

## Installation
### Step 1: Install Arduino IDE
Download and install the Arduino IDE from the official website: https://www.arduino.cc/en/software

### Step 2: Add ESP32 Board Support
1. Open Arduino IDE.
2. Go to File > Preferences.
3. Add the ESP32 board manager URL: `https://dl.espressif.com/dl/package_esp32_index.json`
4. Go to Tools > Board > Boards Manager.
5. Search for "ESP32" and install the board package.

### Step 3: Install Required Libraries
1. Open Arduino IDE Library Manager (Sketch > Include Library > Manage Libraries).
2. Search and install the following libraries:
   - **Adafruit DHT Sensor Library** by Adafruit
   - **Adafruit Unified Sensor** (dependency for DHT library)
   - **LiquidCrystal I2C** by Frank de Brabander

### Step 4: Clone or Download Project
```bash
git clone https://github.com/qppd/RICE_DRYER.git
```
Or download the ZIP file and extract to your local machine.

### Step 5: Upload the Code
1. Open `RiceDryer.ino` in Arduino IDE.
2. Select your ESP32 board from Tools > Board.
3. Select the correct COM port from Tools > Port.
4. Click the Upload button to compile and upload the sketch.

## Usage
### Basic Operation
1. **Power On**: Connect the ESP32 to a power source.
2. **Initialization**: The LCD will display startup information.
3. **Monitoring**: Real-time temperature and humidity readings appear on the LCD.
4. **Control**: Use buttons to start/stop the drying process or switch modes.
5. **Adjustment**: Turn potentiometers to adjust temperature thresholds and timing.
6. **Automation**: Relays automatically activate based on sensor readings and user settings.

### Display Layout
- **Line 1**: Current temperature reading
- **Line 2**: Current humidity reading
- **Line 3**: System status (Running, Idle, Error)
- **Line 4**: User-defined parameters (threshold, timer)

### Button Functions
- **Button 1**: Start/Stop drying operation
- **Button 2**: Mode selection (Manual, Auto, Settings)

### Potentiometer Functions
- **Potentiometer 1**: Adjust temperature threshold (0-100 degrees Celsius)
- **Potentiometer 2**: Set drying duration (0-24 hours)

## Code Structure
The project follows object-oriented programming principles with encapsulated classes:

### DHT22Sensor Class
- **Purpose**: Interface with DHT22 temperature and humidity sensor
- **Methods**:
  - `begin()`: Initialize the sensor
  - `readTemperature()`: Get temperature in Celsius
  - `readHumidity()`: Get relative humidity percentage

### SSR Class
- **Purpose**: Control solid-state relay switching
- **Methods**:
  - `begin()`: Initialize relay pin
  - `on()`: Activate relay
  - `off()`: Deactivate relay
  - `isOn()`: Check relay state

### Button Class
- **Purpose**: Handle tactile button input with debouncing
- **Methods**:
  - `begin()`: Initialize button pin with pull-up
  - `isPressed()`: Check if button is currently pressed

### Potentiometer Class
- **Purpose**: Read analog values from potentiometers
- **Methods**:
  - `begin()`: Initialize analog input
  - `readValue()`: Get current potentiometer value (0-4095)

### LCDDisplay Class
- **Purpose**: Manage I2C LCD display output
- **Methods**:
  - `begin()`: Initialize LCD and backlight
  - `print(col, row, text)`: Display text at specified position
  - `clear()`: Clear display content

## Technical Specifications
- **Microcontroller**: ESP32 (Dual-core Xtensa LX6, 240 MHz)
- **Operating Voltage**: 3.3V logic, 5V power supply
- **Temperature Range**: -40 to 80 degrees Celsius (DHT22)
- **Humidity Range**: 0-100% RH (DHT22)
- **Accuracy**: ±0.5°C temperature, ±2% RH humidity
- **Relay Rating**: Depends on SSR model (typically 25A-40A AC)
- **Display**: 20 characters x 4 lines, I2C interface
- **Input Voltage**: 5V DC via USB or external adapter
- **Power Consumption**: Approximately 500mA (without relays)

## Future Enhancements
- **WiFi Connectivity**: Remote monitoring and control via web interface
- **Data Logging**: Store historical temperature and humidity data
- **Mobile App**: Android/iOS application for system management
- **Multiple Sensors**: Support for additional DHT22 sensors in different locations
- **PID Control**: Implement advanced temperature control algorithms
- **Alert System**: SMS or email notifications for critical conditions
- **Solar Power**: Integration with solar panels for off-grid operation
- **Cloud Integration**: Upload data to cloud platforms for analytics

## Troubleshooting
### LCD Not Displaying
- Check I2C connections (SDA, SCL)
- Verify LCD I2C address (typically 0x27 or 0x3F)
- Ensure proper power supply to LCD

### DHT22 Reading Errors
- Confirm sensor wiring (VCC, GND, Data)
- Check data pin connection to ESP32
- Verify library installation
- Add pull-up resistor (4.7kΩ) to data line

### Relays Not Switching
- Verify GPIO pin assignments
- Check relay power supply
- Test relay independently with multimeter
- Ensure proper current rating for load

### ESP32 Not Programming
- Select correct board and COM port
- Press and hold BOOT button during upload
- Check USB cable (use data cable, not charging-only)
- Install ESP32 USB drivers if needed

## Contributing
Contributions are welcome and encouraged! To contribute:
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/NewFeature`)
3. Commit your changes (`git commit -m 'Add new feature'`)
4. Push to the branch (`git push origin feature/NewFeature`)
5. Open a Pull Request

Please ensure your code follows the existing style and includes appropriate comments.

## License
This project is licensed under the MIT License. You are free to use, modify, and distribute this software for personal or commercial purposes with attribution.

## About the Developer
This project is developed by **Sajed Lopez Mendoza**, a full-stack developer and embedded systems engineer based in Quezon Province, Philippines.

## Contact
For questions, support, or collaboration opportunities:

- **GitHub**: https://github.com/qppd
- **Email**: quezon.province.pd@gmail.com
- **Portfolio**: https://sajed-mendoza.onrender.com
- **Facebook**: https://facebook.com/qppd.dev
- **Facebook Page**: https://facebook.com/QUEZONPROVINCEDEVS
- **TikTok**: @jed.lopez.mendoza.dev
- **YouTube**: @sajed-mendoza
- **Location**: 136 Sitio Crossing, Ilaya Panaon, Unisan, Quezon 4305, Philippines

---

**Note**: This project is actively maintained and updated. Check the repository for the latest version and improvements. Star the repository if you find it useful!
