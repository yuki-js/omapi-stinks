# Root Cause Analysis: Logs Not Showing in UI

## Problem Statement

Logs were appearing in LSPosed raw logs and the file log (`/data/local/tmp/omapistinks/hooks.log`), but NOT in the app UI. The status bar always showed "⚠ Waiting for hooks..." even though hooks were clearly working.

## Root Cause

### The Core Issue: Process Isolation

The root cause was **process isolation** in Android's Xposed framework. When Xposed hooks code, it runs in the **target process**, not in the app's UI process.

```
┌─────────────────────┐         ┌──────────────────────┐
│  App UI Process     │         │ Hooked App Process   │
│  (omapistinks)      │         │  (android, com.*)    │
│                     │         │                      │
│  CallLogger         │    X    │  CallLogger          │
│  Instance #1        │ ─────── │  Instance #2         │
│  (empty)            │  No IPC │  (has logs)          │
└─────────────────────┘         └──────────────────────┘
```

### Why This Happened

1. **Singleton Pattern Limitation**: `CallLogger.getInstance()` creates a singleton **per process**
2. **Separate Memory Spaces**: Each process has its own memory space with separate object instances
3. **No Communication**: There was no inter-process communication (IPC) mechanism to transfer logs from hooked processes to the UI process

### Where Hooks Actually Run

- **android.se.omapi.* hooks**: Run in the **app process** using OMAPI (e.g., Google Pay)
- **com.android.se.Terminal hooks**: Run in the **system service process** (com.android.se)
- **UI app (MainActivity)**: Runs in its **own process** (app.aoki.yuki.omapistinks)

All three are DIFFERENT processes with DIFFERENT CallLogger instances!

## Evidence

From LSPosed logs, we could see:
```
OmapiStinks: Channel.transmit(command=00A4040007A0000000041010)
OmapiStinks: Channel.transmit() returned 6F2A...9000
```

But the UI showed nothing because:
- Hooks ran in Google Pay's process → CallLogger instance in Google Pay
- UI queried CallLogger in its own process → Different CallLogger instance (empty)

## Solution Implemented

### Multi-Level Logging Architecture

Created a `LogDispatcher` class that sends logs to THREE destinations simultaneously:

```
Hooked Process                 UI Process
┌──────────────┐              ┌─────────────┐
│ Hook Fires   │              │  MainActivity│
│      ↓       │              │      ↑       │
│ LogDispatcher│              │      │       │
│   ├─ File ──────────────────┼──→ Read     │
│   ├─ Broadcast ─────────────┼──→ Receive  │
│   └─ Memory  │   (no cross) │              │
└──────────────┘              └─────────────┘
```

#### 1. File Logging (Universal)
- **Location**: `/data/local/tmp/omapistinks/hooks.log`
- **Works**: Always, across all processes
- **Purpose**: Persistent storage and fallback

#### 2. Broadcast IPC (UI Updates)
- **Mechanism**: Android Broadcast with explicit package
- **Intent Action**: `app.aoki.yuki.omapistinks.LOG_ENTRY`
- **Purpose**: Real-time UI updates from any process

#### 3. In-Memory (Same Process)
- **Mechanism**: Direct CallLogger.getInstance().addLog()
- **Works**: Only if hook runs in UI process
- **Purpose**: Fast access when possible

### Code Refactoring

#### Before (Monolithic)
```
XposedInit.java (27,641 bytes)
- handleLoadPackage()
- hookSEServiceClass()
- hookReaderClass()
- hookSessionClass()
- hookChannelClass()
- hookSecureElementService()
- hookSystemServer()
- logCall()
- logToFile()
- bytesToHex()
```

#### After (Modular)
```
XposedInit.java (3,005 bytes) - Minimal coordinator
hooks/LogDispatcher.java (4,227 bytes) - IPC handler
hooks/OmapiHooks.java (1,293 bytes) - Base class
hooks/ClientOmapiHooks.java (16,316 bytes) - Client hooks
hooks/SystemOmapiHooks.java (6,144 bytes) - System hooks
```

**Benefits**:
- 90% code reduction in main file
- Clear separation of concerns
- Easier to maintain and test
- Reusable components

### MainActivity Changes

Added `BroadcastReceiver` to receive logs from hooked processes:

```java
logReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (LogDispatcher.BROADCAST_ACTION.equals(intent.getAction())) {
            String message = intent.getStringExtra(LogDispatcher.EXTRA_MESSAGE);
            if (message != null) {
                CallLogger.getInstance().addLog(message);
                refreshLogs();
            }
        }
    }
};
```

Registered in `onResume()`, unregistered in `onPause()`.

## Testing the Fix

### Verification Steps

1. **Install updated APK**
2. **Enable in LSPosed** with scope: `android`, `com.android.se`, and target app
3. **Reboot device**
4. **Open OMAPI Stinks app**
   - Status should show: "✓ Module active"
5. **Use an app that calls OMAPI** (e.g., Google Pay)
6. **Check UI**: Logs should appear in real-time

### Expected Behavior

**Before Fix**:
- LSPosed logs: ✅ Shows hooks
- File log: ✅ Shows hooks  
- UI: ❌ Empty (waiting for hooks)

**After Fix**:
- LSPosed logs: ✅ Shows hooks
- File log: ✅ Shows hooks
- UI: ✅ Shows hooks in real-time

## Technical Details

### Why Broadcasts Work

Broadcasts are Android's IPC mechanism that:
1. Can cross process boundaries
2. Don't require shared memory
3. Work even if sender and receiver are in different apps
4. Are reliable for non-critical data

### Why File Logging Remains Important

1. **Reliability**: Works even if broadcasts fail
2. **Persistence**: Survives app crashes
3. **Debugging**: Can check logs via ADB even if UI broken
4. **Audit Trail**: Complete history of all hooks

### Performance Considerations

- **File I/O**: Asynchronous, doesn't block hooks
- **Broadcasts**: Lightweight, uses Android's optimized IPC
- **Memory**: Each log entry ~200 bytes, capped at 1000 entries

## Lessons Learned

1. **Always Consider Process Boundaries**: In Xposed, hooks run in target processes, not your app
2. **Singleton ≠ Global**: Singletons are per-process, not system-wide
3. **Use IPC for Cross-Process**: Broadcasts, ContentProviders, or AIDL needed
4. **Multiple Logging Destinations**: Redundancy ensures reliability
5. **Test in Real Environment**: Emulators may hide process isolation issues

## Future Improvements

1. **Batch Broadcasts**: Send multiple logs in one broadcast to reduce overhead
2. **Persistent Storage**: Use SQLite database instead of text file
3. **Filtering**: Allow UI to filter logs by process/package
4. **Export**: Add export functionality for logs
5. **Search**: Add search capability in UI

## References

- Android IPC: https://developer.android.com/guide/components/processes-and-threads
- Xposed Process Handling: https://api.xposed.info/reference/packages.html
- Broadcast Best Practices: https://developer.android.com/guide/components/broadcasts
