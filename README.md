# OMAPI Stinks

An Xposed Module to intercept and log OMAPI (Open Mobile API) calls on Android devices.

## Overview

This Xposed module hooks into OMAPI calls made by applications and system services, providing real-time visibility into Secure Element (SE) communication. Perfect for debugging mobile payment apps, SIM toolkit applications, and any app that uses Secure Elements.

## Features

- **Comprehensive Hooking**: Intercepts all major OMAPI classes:
  - `SEService` - Service connection and reader management
  - `Reader` - Secure Element reader operations
  - `Session` - Session management and channel opening
  - `Channel` - APDU transmission (the critical part!)
  - `Terminal` (System) - System-level APDU transmission

- **Real-Time Logging**: Logs appear instantly in the UI app via broadcast IPC

- **Dual Package Support**: Hooks both legacy (`org.simalliance.openmobileapi`) and modern (`android.se.omapi`) OMAPI packages

- **System Service Hooking**: Monitors `com.android.se.Terminal` for system-wide SE activity

- **Modern Material Design UI**: Clean, card-based interface

- **No Special Permissions**: Works without file access or special permissions

## Requirements

- Android 9.0 (API 28) or higher
- Xposed Framework (LSPosed recommended)
- Root access

## Installation & Setup

### 1. Install the Module

```bash
# Build from source
./gradlew assembleDebug

# Or download from releases
# Then install:
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. Configure LSPosed

1. Open **LSPosed Manager**
2. Enable **"OMAPI Stinks"** module
3. **Add to scope** (CRITICAL):
   - ✅ **android** (system framework - REQUIRED)
   - ✅ **com.android.se** (SecureElement service - REQUIRED)
   - ✅ Your target apps (Google Pay, Samsung Pay, etc.)
4. **Reboot device**

### 3. Use It

1. Launch "OMAPI Stinks" app
2. Use an app that makes OMAPI calls (e.g., Google Pay, mobile banking)
3. Watch logs appear in real-time! 🎉

## How It Works

```
App calls OMAPI → Xposed Hook intercepts → Broadcast to UI → Display in app
```

The module:
1. Hooks OMAPI methods in target processes
2. Captures method calls, parameters, and return values
3. Sends logs via Android broadcast to the UI app
4. UI displays logs in real-time with Material Design cards

## Hooked Methods

### Client-Side (App Level)

**SEService**
- `getReaders()`, `isConnected()`, `shutdown()`, `getVersion()`

**Reader**
- `getName()`, `isSecureElementPresent()`, `openSession()`, `closeSessions()`

**Session**
- `getReader()`, `getATR()`, `close()`, `isClosed()`, `closeChannels()`
- `openBasicChannel(byte[] aid)`, `openBasicChannel(byte[] aid, byte P2)`
- `openLogicalChannel(byte[] aid)`, `openLogicalChannel(byte[] aid, byte P2)`

**Channel**
- `close()`, `isBasicChannel()`, `isClosed()`, `getSelectResponse()`, `getSession()`
- **`transmit(byte[] command)`** - Captures ALL APDU commands and responses!
- `selectNext()`

### System-Side (System Service)

**com.android.se.Terminal**
- **`transmit(byte[] command)`** - ALL APDUs from ALL apps pass through here!

## Supported Packages

The module automatically hooks these OMAPI packages:
- `android.se.omapi.*` (Android 9+)
- `org.simalliance.openmobileapi.*` (Legacy)

Pre-configured scope suggestions include:
- `android` (system framework)
- `com.android.se` (SecureElement service)
- `com.google.android.gms` (Google Play Services)
- `com.google.android.apps.walletnfcrel` (Google Wallet)
- `com.samsung.android.spay` (Samsung Pay)
- `com.android.stk` (SIM Toolkit)
- `com.felicanetworks.mfm.main` (おサイフケータイ - Osaifu-Keitai)
- `com.felicanetworks.mfc` (Mobile Felica Client)
- `com.felicanetworks.mfw.a.main` (Mobile Felica Web Plugin)
- `com.felicanetworks.mfw.a.boot` (Mobile Felica Web Boot)
- `com.felicanetworks.mfs` (Mobile Felica Service)

## Verification & Troubleshooting

### Verify Module is Working

Use `adb logcat` to see real-time diagnostic logs:

```bash
# Terminal 1: Watch for hook activity
adb logcat -s OmapiStinks:* OmapiStinks.LogReceiver:*

# Terminal 2: Watch for broadcast delivery
adb logcat | grep "LOG_ENTRY"
```

**What you should see:**
- `OmapiStinks: Channel.transmit(command=...)` - Hook is firing
- `LogReceiver: Received log: ...` - Broadcast is being received
- `LogReceiver: Log stored successfully. Total logs: N` - Logs are stored

### No logs appearing?

1. **Check LSPosed Manager → Logs** for "OmapiStinks" entries
2. Ensure **"android"** is in scope (CRITICAL!)
3. Ensure **"com.android.se"** is in scope
4. Reboot after enabling module
5. Check module is enabled in LSPosed
6. **Run logcat** to see if hooks are firing but broadcasts failing

### Logs in LSPosed but not in app?

**This means hooks work but broadcast delivery failed.**

Debug steps:
```bash
# Check if LogReceiver is registered
adb shell dumpsys package app.aoki.yuki.omapistinks | grep -A 5 "Receiver"

# Test broadcast manually
adb shell am broadcast -a app.aoki.yuki.omapistinks.LOG_ENTRY \
  --es message "Test message" \
  --es timestamp "2024-01-01 00:00:00.000" \
  -n app.aoki.yuki.omapistinks/.LogReceiver
```

If manual broadcast works, check:
1. Target app is using OMAPI (try Google Pay tap)
2. Hook is in correct process (check `adb logcat`)
3. Broadcast permission issues (shouldn't happen with `exported=true`)

### App crashes?

1. Clear app data and reinstall
2. Check LSPosed logs for errors
3. Ensure you're using LSPosed (not EdXposed)
4. Check logcat for Java exceptions

## Technical Details

### Architecture

- **Single-file Xposed hook**: `XposedInit.java` handles all hooking
- **Broadcast IPC**: Cross-process communication via Android broadcasts
- **In-memory logging**: Fast, no file I/O
- **Material Design 3 UI**: Modern card-based interface

### Why No File Logging?

Previous versions used `/data/local/tmp` for file logging, but unprivileged apps cannot access this directory. The new architecture uses broadcast IPC exclusively, which works perfectly across process boundaries without any permission issues.

### Broadcast Details

- **Action**: `app.aoki.yuki.omapistinks.LOG_ENTRY`
- **Extras**: `message`, `timestamp`
- **Security**: `RECEIVER_NOT_EXPORTED` (Android 13+)

## Building from Source

```bash
git clone https://github.com/yuki-js/omapi-sniff.git
cd omapi-sniff
./gradlew assembleDebug
# APK will be in: app/build/outputs/apk/debug/app-debug.apk
```

## CI/CD

This project includes GitHub Actions workflows for:
- Automated debug and release builds
- Lint checking
- APK artifact uploads (30-day retention)

## References

- [GlobalPlatform Open Mobile API Specification v3.2.0.13](https://globalplatform.org/wp-content/uploads/2018/04/GPD_Open_Mobile_API_Spec_v3.2.0.13_PublicReview.pdf)
- [Android OMAPI Documentation](https://developer.android.com/reference/android/se/omapi/package-summary)
- [LSPosed Framework](https://github.com/LSPosed/LSPosed)

## License

This project is provided as-is for educational and debugging purposes.

## Contributing

Pull requests are welcome! Please ensure:
- Code follows existing style
- Both debug and release builds succeed
- Lint checks pass

---

**Note**: This module is for debugging and analysis purposes. Be aware that intercepting OMAPI calls may expose sensitive security information. Use responsibly and only on devices you own.
