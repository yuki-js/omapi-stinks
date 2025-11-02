# Hook Troubleshooting Guide

## Overview

This document helps you troubleshoot why OMAPI Stinks might not be capturing calls.

## Verifying Module is Working

### 1. Check LSPosed Module Status

1. Open LSPosed Manager
2. Go to Modules
3. Verify "OMAPI Stinks" is enabled (checkbox checked)
4. Verify it has a green checkmark (active)

### 2. Check Module Scope

The module MUST be enabled for the apps you want to monitor. OMAPI Stinks suggests these packages:

**System Packages** (required for system-level OMAPI):
- `android` - Android system framework (REQUIRED for android.se.omapi)
- `com.android.se` - SE Service (if exists on your device)

**Common Apps**:
- `com.google.android.gms` - Google Play Services
- `com.google.android.apps.walletnfcrel` - Google Wallet
- `com.samsung.android.spay` - Samsung Pay

**How to Add to Scope**:
1. In LSPosed, tap on "OMAPI Stinks" module
2. Tap "Application scope" or "Recommended"
3. Enable checkmarks for:
   - Android System (android)
   - Any apps using OMAPI
4. Reboot device or force stop the apps

### 3. Check File Log

The module writes detailed logs to:
```
/data/local/tmp/omapistinks/hooks.log
```

**View from device**:
```bash
adb shell cat /data/local/tmp/omapistinks/hooks.log
```

**From the app**:
- Open OMAPI Stinks app
- Menu → "View File Log"

**What to look for**:
- `=== OMAPI Stinks Module Loaded in Zygote ===` - Module initialized
- `=== Package Loaded: android ===` - Module loaded into system
- `Found OMAPI package: android.se.omapi` - OMAPI classes found
- `Successfully hooked SEService` - Hooks installed

### 4. Check LSPosed Logs

1. Open LSPosed Manager
2. Go to "Logs" tab
3. Filter by "OmapiStinks"
4. Look for:
   - "Loaded into package: android"
   - "Successfully hooked OMAPI"
   - Any error messages

## Common Issues

### Issue: No logs at all

**Cause**: Module not in scope for the app

**Solution**:
1. Add `android` to scope (REQUIRED for android.se.omapi)
2. Add the specific app using OMAPI
3. Reboot device

### Issue: "android.se.omapi" calls not captured

**Cause**: android.se.omapi is in the system framework, not in apps

**Solution**:
1. Enable scope for `android` package (not just apps)
2. Reboot device (system framework needs restart)
3. Check file log: `/data/local/tmp/omapistinks/hooks.log`

### Issue: Hooks work but no UI logs

**Cause**: CallLogger is in app process, system hooks can't access it

**Solution**:
- System process logs go to file log only
- Check: `adb shell cat /data/local/tmp/omapistinks/hooks.log`
- Or use "View File Log" in the app menu

### Issue: File log not created

**Permissions issue with /data/local/tmp**

**Solution**:
```bash
adb shell
su
mkdir -p /data/local/tmp/omapistinks
chmod 777 /data/local/tmp/omapistinks
```

### Issue: Old module behavior after update

**Cached module in memory**

**Solution**:
1. Force stop all apps in scope
2. Reboot device
3. Clear LSPosed cache if available

## Testing OMAPI Calls

### Using ADB

You can test if your device has OMAPI:

```bash
# Check if android.se.omapi exists
adb shell pm list packages | grep se

# Check for SE readers (requires root)
adb shell
su
dumpsys secure_element
```

### Using Test Apps

Apps that typically use OMAPI:
- Google Wallet / Google Pay
- Samsung Pay
- Banking apps with NFC
- Transit payment apps
- SIM Toolkit apps

## Understanding android.se.omapi vs org.simalliance

### android.se.omapi (Modern - Android 9+)
- Built into Android framework
- Located in system process
- **Requires "android" in scope**
- Most new apps use this

### org.simalliance.openmobileapi (Legacy)
- Older standalone library
- Bundled with apps
- **Automatically hooked in apps**
- Rare in modern apps

## Log File Format

```
2024-11-02 12:34:56.789 | === OMAPI Stinks Module Loaded in Zygote ===
2024-11-02 12:34:57.123 | === Package Loaded: android (process: system_server) ===
2024-11-02 12:34:57.456 | Found OMAPI package: android.se.omapi in android
2024-11-02 12:34:57.789 | Successfully hooked android.se.omapi
2024-11-02 12:35:01.234 | SEService.getReaders() returned 2 readers
```

## Expected Behavior

### On Device Boot
1. Module loads in zygote
2. File log created: `/data/local/tmp/omapistinks/hooks.log`
3. "Module Loaded in Zygote" message logged

### When App Starts
1. Module loads into app process
2. "Package Loaded: [appname]" logged
3. If OMAPI found: "Found OMAPI package" logged
4. Hooks installed

### When OMAPI Called
1. Method call logged to:
   - XposedBridge log
   - File log (always)
   - In-memory log (if in app process)

## Advanced: Hooking System Services

To monitor system components (com.android.se):

1. **Enable android scope** - Most important!
2. **Optional**: Add com.android.se if it exists
3. **Reboot** - System services need full restart
4. **Check file log** - System logs don't show in UI

The android.se.omapi API lives in the `android` package (system framework), not in a separate process. That's why "android" scope is essential.

## Getting Help

If still having issues, collect this information:

1. LSPosed version
2. Android version
3. Device model
4. Contents of `/data/local/tmp/omapistinks/hooks.log`
5. LSPosed logs (filtered by "OmapiStinks")
6. App you're trying to monitor
7. Output of: `adb shell pm list packages | grep -i se`

## Quick Checklist

- [ ] Module enabled in LSPosed
- [ ] "android" package in scope (for android.se.omapi)
- [ ] Device rebooted after enabling scope
- [ ] File log exists at /data/local/tmp/omapistinks/hooks.log
- [ ] File log shows "Module Loaded in Zygote"
- [ ] File log shows "Package Loaded: android"
- [ ] File log shows "Found OMAPI package"
- [ ] App using OMAPI has been launched
- [ ] Tried viewing file log from app menu
