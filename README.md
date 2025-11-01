# OMAPI Sniff

An Xposed Module to sniff OMAPI (Open Mobile API) calls on Android devices.

## Overview

This Xposed module intercepts and logs all OMAPI-related calls made by applications on your Android device. OMAPI provides standardized access to Secure Elements (SE) such as SIM cards, embedded Secure Elements, and SD cards with security features.

## Features

- **Comprehensive Hooking**: Hooks all major OMAPI classes and methods:
  - `SEService` - Service connection and reader management
  - `Reader` - Secure Element reader operations
  - `Session` - Session management and channel opening
  - `Channel` - APDU transmission and channel operations

- **Visual Log Viewer**: Built-in Activity to view captured OMAPI calls in real-time

- **Dual Package Support**: Supports both legacy (`org.simalliance.openmobileapi`) and modern (`android.se.omapi`) OMAPI packages

- **Detailed Logging**: Captures:
  - Method calls with parameters
  - Return values
  - APDU commands and responses in hexadecimal format
  - Timestamps for all calls

## Requirements

- Android 9.0 (API 28) or higher
- Xposed Framework (LSPosed, EdXposed, or original Xposed)
- Root access

## Installation

1. Build the APK using Android Studio or Gradle:
   ```bash
   ./gradlew assembleDebug
   ```

2. Install the APK on your device:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

3. Activate the module in your Xposed framework (LSPosed, EdXposed, etc.)

4. Reboot your device or restart the target app

5. Launch "OMAPI Sniff" to view captured logs

## Hooked Methods

### SEService
- `getReaders()` - Get available SE readers
- `isConnected()` - Check service connection status
- `shutdown()` - Shutdown the service
- `getVersion()` - Get OMAPI version

### Reader
- `getName()` - Get reader name
- `isSecureElementPresent()` - Check if SE is present
- `openSession()` - Open a session to the SE
- `closeSessions()` - Close all sessions

### Session
- `getReader()` - Get the associated reader
- `getATR()` - Get Answer To Reset
- `close()` - Close the session
- `isClosed()` - Check if session is closed
- `closeChannels()` - Close all channels
- `openBasicChannel(byte[] aid)` - Open basic channel with AID
- `openBasicChannel(byte[] aid, byte P2)` - Open basic channel with AID and P2
- `openLogicalChannel(byte[] aid)` - Open logical channel with AID
- `openLogicalChannel(byte[] aid, byte P2)` - Open logical channel with AID and P2

### Channel
- `close()` - Close the channel
- `isBasicChannel()` - Check if it's a basic channel
- `isClosed()` - Check if channel is closed
- `getSelectResponse()` - Get SELECT response
- `getSession()` - Get the associated session
- `transmit(byte[] command)` - Transmit APDU command (most important!)
- `selectNext()` - Select next applet

## Usage

1. Open the OMAPI Sniff app
2. Use any app that makes OMAPI calls (e.g., mobile payment apps, SIM toolkit apps)
3. View the captured calls in real-time in the OMAPI Sniff app
4. Use the refresh button to update the log view
5. Use the clear button to clear all logs

## Technical Details

This module is based on the GlobalPlatform Open Mobile API Specification v3.2.0.13, which defines the standard API for accessing Secure Elements on mobile devices.

### Supported Packages
- `org.simalliance.openmobileapi.*` (Legacy)
- `android.se.omapi.*` (Android 9+)

## Building from Source

```bash
git clone https://github.com/yuki-js/omapi-sniff.git
cd omapi-sniff
./gradlew assembleDebug
```

## License

This project is provided as-is for educational and debugging purposes.

## References

- [GlobalPlatform Open Mobile API Specification](https://globalplatform.org/wp-content/uploads/2018/04/GPD_Open_Mobile_API_Spec_v3.2.0.13_PublicReview.pdf)
- [Android OMAPI Documentation](https://developer.android.com/reference/android/se/omapi/package-summary)
