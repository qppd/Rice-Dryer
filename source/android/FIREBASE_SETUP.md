# Firebase Setup Guide for Rice Dryer Android App

## Prerequisites
- Firebase account (https://firebase.google.com/)
- Android Studio installed with the project opened

## Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project" or select existing project
3. Enter project name: `RiceDryer` (or your preferred name)
4. Enable/Disable Google Analytics (optional)
5. Click "Create project"

## Step 2: Add Android App to Firebase Project

1. In Firebase Console, click the Android icon to add Android app
2. Enter package name: `com.qppd.ricedryer`
3. Enter app nickname: `Rice Dryer Android` (optional)
4. Enter SHA-1 certificate (optional, but recommended for authentication)
   - To get SHA-1 in Android Studio:
     ```bash
     ./gradlew signingReport
     ```
   - Or use keytool:
     ```bash
     keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
     ```
5. Click "Register app"

## Step 3: Download google-services.json

1. Click "Download google-services.json"
2. Copy the file to: `source/android/RiceDryer/app/google-services.json`
3. **IMPORTANT**: This file contains sensitive data. Ensure it's in `.gitignore`

## Step 4: Enable Firebase Services

### Enable Authentication
1. In Firebase Console, go to "Authentication"
2. Click "Get started"
3. Enable "Email/Password" sign-in method
4. Click "Save"

### Enable Realtime Database
1. In Firebase Console, go to "Realtime Database"
2. Click "Create Database"
3. Choose location (e.g., us-central1)
4. Start in **Test mode** for development (or Production mode with rules)
5. Click "Enable"

### Set Database Rules (Development)
For development, use these rules (⚠️ Not secure for production):

```json
{
  "rules": {
    "devices": {
      "$deviceId": {
        ".read": true,
        ".write": true
      }
    },
    "users": {
      "$userId": {
        ".read": "auth != null && auth.uid == $userId",
        ".write": "auth != null && auth.uid == $userId"
      }
    }
  }
}
```

### Set Database Rules (Production - Recommended)
For production, use these secure rules:

```json
{
  "rules": {
    "devices": {
      "$deviceId": {
        ".read": "auth != null && (
          root.child('devices').child($deviceId).child('deviceInfo').child('pairedTo').val() == auth.uid ||
          root.child('devices').child($deviceId).child('deviceInfo').child('pairedTo').val() == null
        )",
        ".write": "auth != null"
      }
    },
    "users": {
      "$userId": {
        ".read": "auth != null && auth.uid == $userId",
        ".write": "auth != null && auth.uid == $userId"
      }
    }
  }
}
```

## Step 5: Verify Setup

The project is already configured with:
- ✅ Firebase dependencies in `build.gradle.kts`
- ✅ Google Services plugin
- ✅ Internet permissions in `AndroidManifest.xml`

## Step 6: Build and Run

1. Open the project in Android Studio
2. Sync Gradle files (File > Sync Project with Gradle Files)
3. Connect Android device or start emulator
4. Click "Run" button

## Database Structure

The app expects this Firebase Realtime Database structure:

```
firebase-root/
├── devices/
│   └── {deviceId}/
│       ├── deviceInfo/
│       │   ├── macAddress: String
│       │   ├── firmwareVersion: String
│       │   ├── hardwareVersion: String
│       │   ├── pairedTo: String (userId)
│       │   ├── deviceName: String
│       │   ├── pairingCode: String
│       │   └── pairingCodeExpiry: Number
│       ├── status/
│       │   ├── temperature: Number
│       │   ├── humidity: Number
│       │   ├── setpointTemp: Number
│       │   ├── setpointHumidity: Number
│       │   ├── dryingActive: Boolean
│       │   ├── heaterOn: Boolean
│       │   ├── fanOn: Boolean
│       │   ├── wifiConnected: Boolean
│       │   ├── firebaseConnected: Boolean
│       │   └── lastUpdate: Number
│       ├── settings/
│       │   ├── autoStop: Boolean
│       │   ├── maxTemp: Number
│       │   └── minTemp: Number
│       ├── commands/
│       │   ├── command: String (START, STOP, SET_TEMP, SET_HUMIDITY)
│       │   ├── value: Number
│       │   ├── timestamp: Number
│       │   └── processed: Boolean
│       └── history/
│           └── {timestamp}/
│               ├── temperature: Number
│               ├── humidity: Number
│               ├── setpointTemp: Number
│               ├── setpointHumidity: Number
│               ├── heaterOn: Boolean
│               ├── fanOn: Boolean
│               ├── dryingActive: Boolean
│               └── timestamp: Number
└── users/
    └── {userId}/
        ├── email: String
        ├── displayName: String
        ├── devices: Array<String>
        ├── notificationEnabled: Boolean
        ├── tempUnit: String
        ├── createdAt: Number
        └── lastLogin: Number
```

## Troubleshooting

### App crashes on startup
- Check if `google-services.json` is in the correct location
- Verify package name matches in Firebase Console and `build.gradle.kts`
- Check logcat for specific error messages

### Authentication fails
- Ensure Email/Password authentication is enabled in Firebase Console
- Check internet connection
- Verify Firebase rules allow authentication

### Cannot read/write to database
- Check Firebase Realtime Database rules
- Ensure user is authenticated
- Verify database URL in Firebase Config

### Device pairing fails
- Ensure ESP32 device is registered in Firebase
- Check pairing code hasn't expired (10 minutes validity)
- Verify device ID (MAC address) is correct

## Next Steps

1. **Add google-services.json** to your project
2. **Configure Firebase Database Rules** for security
3. **Test the app** with your ESP32 device
4. **Configure production signing** for release builds

## Security Notes

⚠️ **IMPORTANT SECURITY CONSIDERATIONS:**

1. **Never commit `google-services.json` to version control**
2. **Use secure Firebase Rules in production**
3. **Enable App Check** for additional security
4. **Implement proper user authentication flow**
5. **Validate all inputs on both client and server**
6. **Use HTTPS only** for all communications
7. **Regularly update dependencies** for security patches

## Support

For issues or questions:
- Check ESP32 firmware is properly configured
- Review Firebase Console for errors
- Check Android Logcat for detailed error messages
- Ensure all dependencies are up to date
