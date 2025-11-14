# Rice Dryer Android Application

Professional IoT monitoring and control application for the Rice Dryer ESP32 system.

## 📱 Features

### ✅ Implemented Features

#### Authentication System
- Email/password registration and login
- Password reset functionality
- Secure Firebase Authentication integration
- Persistent user sessions

#### Real-time Device Dashboard
- Live temperature and humidity gauges with smooth animations
- Dual setpoint controls (temperature and humidity)
- START/STOP drying controls
- Heater and fan status indicators
- WiFi and Firebase connection status
- Real-time data updates every 5 seconds

#### Device Pairing
- 6-digit pairing code input
- Device ID (MAC address) validation
- Pairing code expiry handling
- Multi-device support

#### Charts & Analytics
- Interactive line charts for temperature and humidity
- Multiple time range options (1 hour, 6 hours, 24 hours, 1 week)
- Historical data visualization
- Statistics (average, min, max values)
- Zoom and pan support

#### Offline Support
- Local Room database caching
- Offline data access
- Automatic sync when connection restored

### 🚧 To Be Implemented

- Device list with multi-device management
- Push notifications for alerts
- Device settings and customization
- Export data to CSV
- User profile management
- Temperature unit switching (°C/°F)

## 🏗️ Architecture

### Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Navigation**: Jetpack Navigation Compose
- **Local Database**: Room
- **Backend**: Firebase Realtime Database
- **Authentication**: Firebase Auth
- **Charts**: MPAndroidChart

### Project Structure

```
app/src/main/java/com/qppd/ricedryer/
├── data/
│   ├── local/              # Room database entities and DAOs
│   │   ├── AppDatabase.kt
│   │   ├── CachedDevice.kt
│   │   └── DeviceDao.kt
│   ├── model/              # Data models
│   │   ├── DeviceData.kt
│   │   └── UserProfile.kt
│   └── repository/         # Data repositories
│       ├── AuthRepository.kt
│       └── DeviceRepository.kt
├── ui/
│   ├── auth/              # Authentication screens
│   │   ├── AuthViewModel.kt
│   │   ├── LoginScreen.kt
│   │   └── RegisterScreen.kt
│   ├── dashboard/         # Main dashboard
│   │   ├── DashboardViewModel.kt
│   │   └── DashboardScreen.kt
│   ├── charts/            # Charts and analytics
│   │   ├── ChartsViewModel.kt
│   │   └── ChartsScreen.kt
│   ├── pairing/           # Device pairing
│   │   ├── PairDeviceViewModel.kt
│   │   └── PairDeviceScreen.kt
│   └── theme/             # Material Design 3 theme
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── navigation/            # Navigation setup
│   ├── Screen.kt
│   └── RiceDryerNavGraph.kt
└── MainActivity.kt        # Entry point
```

## 🚀 Getting Started

### Prerequisites

1. **Android Studio** (Latest stable version)
2. **JDK 11** or higher
3. **Firebase Account** with project set up
4. **ESP32 Device** running Rice Dryer firmware

### Installation Steps

1. **Clone the repository**
   ```bash
   cd source/android/RiceDryer
   ```

2. **Set up Firebase** (REQUIRED)
   - Follow the detailed guide in `FIREBASE_SETUP.md`
   - Download `google-services.json` from Firebase Console
   - Place it in `app/` directory
   - Enable Authentication and Realtime Database

3. **Open in Android Studio**
   - Open the `source/android/RiceDryer` folder
   - Wait for Gradle sync to complete

4. **Build the project**
   ```bash
   ./gradlew build
   ```

5. **Run on device/emulator**
   - Connect Android device (API 24+) or start emulator
   - Click "Run" button or use:
   ```bash
   ./gradlew installDebug
   ```

## 📋 Configuration

### Firebase Setup (Required)

See `FIREBASE_SETUP.md` for detailed Firebase configuration instructions.

### Minimum Requirements

- **Android Version**: 7.0 (Nougat) - API Level 24
- **Target SDK**: 36
- **Compile SDK**: 36
- **Kotlin**: 2.0.21

## 🔧 Dependencies

### Core Dependencies
```kotlin
- androidx.core:core-ktx:1.10.1
- androidx.lifecycle:lifecycle-runtime-ktx:2.6.1
- androidx.activity:activity-compose:1.8.0
```

### Compose UI
```kotlin
- androidx.compose:compose-bom:2024.09.00
- androidx.compose.material3
- androidx.compose.ui
```

### Firebase
```kotlin
- com.google.firebase:firebase-bom:33.5.1
- com.google.firebase:firebase-auth-ktx
- com.google.firebase:firebase-database-ktx
- com.google.firebase:firebase-analytics-ktx
```

### Navigation & ViewModel
```kotlin
- androidx.navigation:navigation-compose:2.8.0
- androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5
```

### Local Database
```kotlin
- androidx.room:room-runtime:2.6.1
- androidx.room:room-ktx:2.6.1
```

### Charts
```kotlin
- com.github.PhilJay:MPAndroidChart:v3.1.0
```

## 📱 Usage

### First Time Setup

1. **Launch the app**
2. **Create an account** or **Sign in**
3. **Pair your ESP32 device**:
   - Power on Rice Dryer
   - Hold BUTTON 1 for 3 seconds on ESP32
   - Enter Device ID and 6-digit pairing code shown on LCD
   - Tap "Pair Device"

### Daily Usage

1. **View Dashboard**: See real-time temperature and humidity
2. **Control Drying**:
   - Adjust temperature setpoint (30-80°C)
   - Adjust humidity target (10-50%)
   - Tap START to begin drying
   - Tap STOP to end process
3. **View Charts**: Analyze historical data and trends
4. **Monitor Status**: Check connection and device status

## 🔐 Security

- ✅ Firebase Authentication with email/password
- ✅ Secure data transmission over HTTPS
- ✅ User-specific device pairing
- ✅ Input validation and error handling
- ⚠️ **Never commit `google-services.json` to version control**
- ⚠️ Use production Firebase rules for deployed apps

## 🐛 Troubleshooting

### App won't build
- Sync Gradle files: `File > Sync Project with Gradle Files`
- Clean build: `Build > Clean Project` then `Build > Rebuild Project`
- Check `google-services.json` is present in `app/` directory

### Cannot connect to Firebase
- Verify internet connection
- Check Firebase project settings
- Ensure `google-services.json` package name matches
- Review Firebase Console for service status

### Device pairing fails
- Verify ESP32 is in pairing mode (LCD shows code)
- Check Device ID matches MAC address
- Ensure pairing code hasn't expired (10 min validity)
- Confirm ESP32 has internet connection

### Charts not loading
- Ensure device has historical data
- Check time range selection
- Verify Firebase database permissions
- Try refreshing the screen

## 📊 Performance

- **App Size**: ~15-20 MB (release build)
- **RAM Usage**: ~150-200 MB
- **Network**: ~5KB per device update
- **Battery**: Minimal impact (background sync disabled)

## 🛠️ Development

### Building Release APK

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release-unsigned.apk`

### Running Tests

```bash
./gradlew test           # Unit tests
./gradlew connectedTest  # Instrumented tests
```

### Code Quality

```bash
./gradlew lint          # Run lint checks
./gradlew ktlintCheck   # Kotlin code style
```

## 📄 License

This project is part of the Rice Dryer IoT system.

## 👥 Contributors

- QPPD Development Team

## 📞 Support

For issues or questions:
1. Check the troubleshooting section
2. Review `FIREBASE_SETUP.md`
3. Check ESP32 firmware compatibility
4. Review Firebase Console logs

## 🔄 Version History

### v1.0.0 (Current)
- ✅ Authentication system
- ✅ Real-time dashboard
- ✅ Device pairing
- ✅ Charts and analytics
- ✅ Offline support

### Planned Updates
- v1.1.0: Device list and management
- v1.2.0: Push notifications
- v1.3.0: Data export and sharing
- v1.4.0: Advanced analytics
