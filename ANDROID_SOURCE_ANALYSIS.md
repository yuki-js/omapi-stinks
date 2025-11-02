# Android Source Code Analysis for OMAPI

## Overview

Based on Android Open Source Project (AOSP) analysis, here's the OMAPI implementation structure:

## Key Source Locations in AOSP

### 1. Framework API (android.se.omapi)
**Location**: `frameworks/base/core/java/android/se/omapi/`

**Key Classes**:
- `SEService.java` - Main entry point for OMAPI
- `Reader.java` - Represents a Secure Element reader
- `Session.java` - Communication session with SE
- `Channel.java` - Logical communication channel

**Implementation**: These are client-side API classes that applications use.

### 2. System Service (SecureElement)
**Location**: `packages/apps/SecureElement/`

**Key Components**:
- Service runs as system app (com.android.se)
- Implements ISecureElementService AIDL
- Manages hardware access to SE

**Process**: Runs in separate process `com.android.se`

### 3. Hardware Abstraction Layer (HAL)
**Location**: `hardware/interfaces/secure_element/`

**Versions**:
- V1.0, V1.1, V1.2 (HIDL)
- AIDL version (newer)

**Implementation**: Vendor-specific drivers communicate with actual SE hardware

## Architecture Flow

```
App Process                      System Process                Hardware
┌─────────────────┐             ┌──────────────────┐         ┌─────────┐
│ android.se.     │   Binder    │ SecureElement    │  HAL    │ SE      │
│ omapi.SEService │◄───────────►│ Service          │◄───────►│ Hardware│
│                 │   IPC       │ (com.android.se) │         │         │
└─────────────────┘             └──────────────────┘         └─────────┘
```

## Important Discovery for Hooking

### Where OMAPI Actually Lives

1. **android.se.omapi.* classes** are in `android` package (system framework)
   - Loaded in every app process that uses them
   - **Hook Location**: App processes using OMAPI
   - **Scope Required**: The app package name

2. **SecureElement Service (com.android.se)** is the backend
   - Runs in separate system process
   - **Hook Location**: `com.android.se` process
   - **Scope Required**: `com.android.se` package

3. **system_server** hosts some SE-related services
   - **Hook Location**: `system_server` process
   - **Scope Required**: `android` package

## Key Files from AOSP

### SEService.java Structure
```java
package android.se.omapi;

public final class SEService {
    private ISecureElementService mSecureElementService;
    
    // Constructor binds to system service
    public SEService(Context context, OnConnectedListener listener) {
        mContext = context;
        mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder service) {
                mSecureElementService = ISecureElementService.Stub.asInterface(service);
                // ...
            }
        };
        Intent intent = new Intent(ISecureElementService.class.getName());
        intent.setClassName("com.android.se", "com.android.se.SecureElementService");
        context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }
    
    public Reader[] getReaders() {
        // Calls through Binder to system service
        String[] readerNames = mSecureElementService.getReaders();
        // ...
    }
}
```

### Reader.java
```java
package android.se.omapi;

public final class Reader {
    private final ISecureElementReader mReader;
    
    public Session openSession() throws IOException {
        ISecureElementSession session = mReader.openSession();
        return new Session(mService, session, this);
    }
}
```

### Channel.java (Critical for APDU)
```java
package android.se.omapi;

public final class Channel implements Closeable {
    public byte[] transmit(byte[] command) throws IOException {
        // This is the money shot - all APDU commands go through here
        return mChannel.transmit(command);
    }
}
```

## System Service Implementation

### SecureElementService (com.android.se)

**Location**: `packages/apps/SecureElement/src/com/android/se/`

**Key Files**:
- `SecureElementService.java` - Main service implementation
- `Terminal.java` - Represents physical SE terminal
- `ChannelAccess.java` - Access control

**Service Declaration**: AndroidManifest.xml
```xml
<service android:name=".SecureElementService"
         android:permission="android.permission.BIND_SECURE_ELEMENT_SERVICE"
         android:exported="true">
    <intent-filter>
        <action android:name="android.se.omapi.ISecureElementService"/>
    </intent-filter>
</service>
```

## Hooking Strategy Based on Source Analysis

### Strategy 1: Hook Client-Side API (Current Implementation)
**Location**: `android.se.omapi.*` in app processes
**Pros**: 
- Sees all calls from specific app
- Can access app context
**Cons**:
- Must hook each app separately
- Doesn't see system-level activity

### Strategy 2: Hook System Service (NEW - Recommended)
**Location**: `com.android.se` service process
**Pros**:
- Sees ALL OMAPI calls from all apps
- Single point of monitoring
- Sees system-level SE access
**Cons**:
- Need to hook AIDL implementation
- Different hooking approach

### Strategy 3: Hook Both (Comprehensive)
Hook both client API and service for complete coverage

## Implementation for System Service Hooking

Based on source analysis, we need to hook:

### In com.android.se process:
```java
// SecureElementService.java
private final class SecureElementServiceImpl extends ISecureElementService.Stub {
    @Override
    public String[] getReaders() {
        // Hook here to see all reader requests
    }
    
    @Override
    public ISecureElementReader getReader(String reader) {
        // Hook here to see all reader access
    }
}

// Terminal.java
public class Terminal {
    public byte[] transmit(byte[] command) {
        // Hook here - all APDU at hardware level
    }
}
```

## SELinux and Permissions

From AOSP sepolicy:

```
# SecureElement service needs these permissions:
type secure_element, domain;
type secure_element_exec, exec_type, file_type;

# Can access SE HAL
allow secure_element hal_secure_element_hwservice:hwservice_manager find;
```

## Hardware Abstraction Layer (HAL)

### AIDL Interface (Modern)
**Location**: `hardware/interfaces/secure_element/aidl/`

```java
interface ISecureElement {
    void init(in ISecureElementCallback clientCallback);
    byte[] transmit(in byte[] data);
    boolean openLogicalChannel(in byte[] aid, byte p2);
    boolean openBasicChannel(in byte[] aid, byte p2);
}
```

### SE Types
1. **eSE** (Embedded SE) - Built into device
2. **UICC** (SIM card SE) - In SIM card
3. **SD** (SD card SE) - Rare

## Key Insights for OMAPI Stinks

### Where Calls Actually Happen

1. **App makes call**: `channel.transmit(apdu)`
2. **Local object**: `android.se.omapi.Channel` (in app process)
3. **Binder call**: To `ISecureElementChannel.Stub` 
4. **Service receives**: `com.android.se` process
5. **Service calls**: Terminal.transmit()
6. **HAL interface**: hardware/interfaces/secure_element
7. **Hardware**: Actual SE chip

### Current Hooking Location
We currently hook #2 (local object in app), which means:
- ✅ We see calls from apps we scope
- ❌ We miss calls if app not in scope
- ❌ We miss system-level SE access
- ❌ We don't see the aggregated view

### Recommended Additional Hooking
Also hook #4 (service in com.android.se):
- ✅ See ALL OMAPI activity system-wide
- ✅ See which app made each call
- ✅ See system SE access
- ✅ Single point of monitoring

## Source Code References

### AOSP Links
- Framework: https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/se/omapi/
- Service: https://cs.android.com/android/platform/superproject/+/master:packages/apps/SecureElement/
- HAL: https://cs.android.com/android/platform/superproject/+/master:hardware/interfaces/secure_element/

### Key Commits
- Initial OMAPI implementation: Android 9.0 (API 28)
- AIDL migration: Android 12+
- Recent security patches

## Implementation Notes for Module

### What We Need to Add

1. **Hook com.android.se service**:
   - Hook ISecureElementService.Stub methods
   - Hook Terminal.transmit() for raw APDU
   - Log calling UID/package for attribution

2. **Hook system_server** (optional):
   - Some SE management happens here
   - PackageManager SE permissions

3. **Improve logging**:
   - Add calling package info
   - Add SE terminal identification
   - Add APDU direction (to SE / from SE)

### Package Scope Requirements

Based on source analysis:
- `android` - For framework classes (android.se.omapi.*)
- `com.android.se` - For SecureElement service (CRITICAL!)
- App packages - For app-specific monitoring

## Testing Strategy

### Verify System Service Hook
```bash
# Check if com.android.se process exists
adb shell ps -A | grep com.android.se

# Check SecureElement service
adb shell dumpsys secure_element

# Force trigger OMAPI
adb shell am start -n com.android.se/.AccessControlEnforcer
```

### Verify Hooks Working
1. Hook com.android.se process
2. Launch any OMAPI app
3. Should see calls in com.android.se logs
4. Should see aggregated view of ALL SE access

## Conclusion

The Android Source investigation reveals that while our current implementation correctly hooks the client-side API (android.se.omapi.*), we should ALSO hook the system service (com.android.se) to get complete coverage of all OMAPI activity on the device. This is especially important because:

1. System components access SE through the service
2. Multiple apps may use SE simultaneously
3. The service is the single chokepoint for all SE access
4. We can attribute calls to specific apps via UID

Next steps: Implement service-side hooking in com.android.se process.
