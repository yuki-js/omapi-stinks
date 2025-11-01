# OMAPI Sniff - Implementation Details

## Overview

This Xposed Module implementation provides comprehensive hooking and logging of all OMAPI (Open Mobile API) calls on Android devices. The implementation is based on the GlobalPlatform Open Mobile API Specification v3.2.0.13.

## Architecture

### 1. Xposed Hook Module (`XposedInit.java`)

The main entry point that implements `IXposedHookLoadPackage`. It hooks into both legacy and modern OMAPI packages:

- **Legacy Package**: `org.simalliance.openmobileapi.*`
- **Modern Package**: `android.se.omapi.*` (Android 9+)

### 2. Hooked Classes and Methods

#### SEService Class
Service connection and reader management:
- `getReaders()` - Retrieves all available Secure Element readers
- `isConnected()` - Checks if service is connected
- `shutdown()` - Shuts down the service
- `getVersion()` - Gets OMAPI version string

#### Reader Class
Secure Element reader operations:
- `getName()` - Gets reader name (e.g., "eSE1", "SIM1")
- `isSecureElementPresent()` - Checks if SE is physically present
- `openSession()` - Opens a session to the Secure Element
- `closeSessions()` - Closes all open sessions

#### Session Class
Session management and channel operations:
- `getReader()` - Gets the associated Reader object
- `getATR()` - Gets Answer To Reset (card identification)
- `close()` - Closes the session
- `isClosed()` - Checks if session is closed
- `closeChannels()` - Closes all channels
- `openBasicChannel(byte[] aid)` - Opens basic channel (channel 0)
- `openBasicChannel(byte[] aid, byte P2)` - Opens basic channel with P2 parameter
- `openLogicalChannel(byte[] aid)` - Opens logical channel (1-19)
- `openLogicalChannel(byte[] aid, byte P2)` - Opens logical channel with P2 parameter

#### Channel Class
APDU transmission and channel management:
- `close()` - Closes the channel
- `isBasicChannel()` - Checks if it's the basic channel
- `isClosed()` - Checks if channel is closed
- `getSelectResponse()` - Gets the SELECT command response (FCI)
- `getSession()` - Gets the associated Session object
- `transmit(byte[] command)` - **Most important**: Transmits APDU commands
- `selectNext()` - Selects next applet instance

### 3. Call Logger (`CallLogger.java`)

Thread-safe singleton that stores log entries:
- Maintains up to 1000 log entries in memory
- Thread-safe operations for concurrent access
- Timestamp formatting with millisecond precision
- FIFO buffer management

### 4. MainActivity (`MainActivity.java`)

Activity for viewing logs:
- RecyclerView for efficient log display
- Auto-refresh every 1 second
- Menu actions: Refresh and Clear
- Auto-scroll to latest entries

### 5. LogAdapter (`LogAdapter.java`)

RecyclerView adapter for displaying log entries:
- Two-line layout: timestamp + message
- Monospace font for better readability of hex data
- Hover effect for better UX

## Data Flow

```
OMAPI Call
    ↓
Xposed Hook (XposedInit)
    ↓
Log to XposedBridge (system log)
    ↓
Store in CallLogger (in-memory)
    ↓
Display in MainActivity (UI)
```

## Key Features

### 1. Hex Data Formatting
All byte arrays (AIDs, APDUs, responses) are automatically converted to hexadecimal strings for easy analysis.

### 2. Object Tracking
Objects are tracked using their hash codes, making it easy to correlate operations on the same Session or Channel.

### 3. Dual Package Support
Automatically hooks both old and new OMAPI implementations, ensuring compatibility across Android versions.

### 4. Real-time Logging
Logs are captured in real-time and displayed with minimal latency in the UI.

## APDU Analysis

The most critical hooked method is `Channel.transmit()`, which captures:
- **Command APDU**: The command sent to the Secure Element
- **Response APDU**: The response from the Secure Element

Example log entries:
```
Channel.transmit(command=00A4040007A0000000041010)
Channel.transmit() returned 6F2A840EA0000000041010...9000
```

The response always ends with a status word (SW1-SW2), where `9000` indicates success.

## Security Considerations

This module is designed for debugging and analysis purposes. When analyzing OMAPI logs:

1. **Sensitive Data**: APDUs may contain sensitive data (PINs, keys, payment info)
2. **Privacy**: Log entries are stored in memory only (cleared on app termination)
3. **Access Control**: Requires Xposed framework and root access
4. **Scope**: Only logs calls made by apps on the same device

## Building

```bash
# Clone repository
git clone https://github.com/yuki-js/omapi-sniff.git
cd omapi-sniff

# Build APK
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

## Testing

To verify the module is working:

1. Install and activate in Xposed framework
2. Reboot or restart system services
3. Open OMAPI Sniff app
4. Use any app that accesses Secure Elements:
   - Mobile payment apps (Google Pay, Samsung Pay)
   - SIM toolkit applications
   - NFC card emulation apps
   - Banking apps with SE access
5. Check logs in OMAPI Sniff app

## Troubleshooting

### No logs appearing
- Verify module is activated in Xposed framework
- Check Xposed logs for hook errors
- Ensure target app actually uses OMAPI
- Try rebooting the device

### Hook failures
- Some ROMs may use custom OMAPI implementations
- Check Xposed logs for ClassNotFoundException
- Verify the package name in use on your device

### App crashes
- Some apps may have anti-hooking mechanisms
- Try disabling other Xposed modules
- Check logcat for detailed error messages

## Future Enhancements

Possible improvements:
1. Export logs to file
2. Filter logs by package name
3. Search functionality
4. APDU parsing and interpretation
5. Statistics and analytics
6. Log persistence across app restarts
7. Support for PKCS#11 operations (if exposed via OMAPI)

## References

- [GlobalPlatform OMAPI Specification v3.2.0.13](https://globalplatform.org/wp-content/uploads/2018/04/GPD_Open_Mobile_API_Spec_v3.2.0.13_PublicReview.pdf)
- [Android OMAPI Package](https://developer.android.com/reference/android/se/omapi/package-summary)
- [Xposed Framework API](https://api.xposed.info/)
