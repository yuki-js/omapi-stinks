package app.aoki.yuki.omapistinks.hooks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import app.aoki.yuki.omapistinks.CallLogEntry;
import app.aoki.yuki.omapistinks.Constants;
import app.aoki.yuki.omapistinks.LogBroadcaster;

/**
 * Hooks com.android.se.Terminal.transmit() for system-level APDU monitoring
 */
public class TerminalTransmitHook {
    
    public static void hook(LoadPackageParam lpparam, LogBroadcaster broadcaster) {
        try {
            Class<?> terminalClass = XposedHelpers.findClass("com.android.se.Terminal", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(terminalClass, "transmit", byte[].class, new XC_MethodHook() {
                private String commandHex;
                private long startTime;
                
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    byte[] command = (byte[]) param.args[0];
                    commandHex = LogBroadcaster.bytesToHex(command);
                    startTime = System.currentTimeMillis();
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    long executionTime = System.currentTimeMillis() - startTime;
                    byte[] response = (byte[]) param.getResult();
                    String responseHex = response != null ? LogBroadcaster.bytesToHex(response) : null;
                    
                    // Get thread information
                    Thread currentThread = Thread.currentThread();
                    long threadId = currentThread.getId();
                    String threadName = currentThread.getName();
                    int processId = android.os.Process.myPid();
                    
                    // Create structured log entry
                    CallLogEntry entry = new CallLogEntry(
                        broadcaster.createTimestamp(),
                        broadcaster.createShortTimestamp(),
                        lpparam.packageName,
                        "[SYSTEM] Terminal.transmit",
                        Constants.TYPE_TRANSMIT,
                        commandHex,
                        responseHex,
                        null, // no AID for transmit
                        null, // no select response for transmit
                        null, // no details for transmit
                        threadId,
                        threadName,
                        processId,
                        executionTime
                    );
                    
                    broadcaster.logMessage(entry);
                }
            });
        } catch (Throwable t) {
            // Terminal class might not exist
        }
    }
}
