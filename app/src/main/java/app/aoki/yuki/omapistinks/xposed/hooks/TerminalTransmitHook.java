package app.aoki.yuki.omapistinks.xposed.hooks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import app.aoki.yuki.omapistinks.core.CallLogEntry;
import app.aoki.yuki.omapistinks.core.Constants;
import app.aoki.yuki.omapistinks.xposed.LogBroadcaster;

/**
 * Hooks com.android.se.Terminal.transmit() for system-level APDU monitoring
 * 
 * Thread-safety: Uses ThreadLocal variables to avoid races across concurrent calls.
 * Exception-safety: All exceptions are caught and never propagate to hooked methods.
 */
public class TerminalTransmitHook {
    
    // ThreadLocal variables to store per-call state (thread-safe for concurrent calls)
    private static final ThreadLocal<String> commandHexTL = new ThreadLocal<>();
    private static final ThreadLocal<Long> startTimeTL = new ThreadLocal<>();
    
    public static void hook(LoadPackageParam lpparam, LogBroadcaster broadcaster) {
        try {
            Class<?> terminalClass = XposedHelpers.findClass("com.android.se.Terminal", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(terminalClass, "transmit", byte[].class, new XC_MethodHook() {
                
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    // Wrap in try/catch to ensure no exceptions propagate to hooked method
                    try {
                        byte[] command = (byte[]) param.args[0];
                        commandHexTL.set(LogBroadcaster.bytesToHex(command));
                        startTimeTL.set(System.currentTimeMillis());
                    } catch (Throwable t) {
                        // Absorb all exceptions - hook should never interfere with app
                    }
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // Wrap in try/catch to ensure no exceptions propagate to hooked method
                    try {
                        // Safely retrieve ThreadLocal values with defaults
                        String commandHex = commandHexTL.get();
                        Long startTime = startTimeTL.get();
                        
                        // Calculate execution time (handle missing startTime gracefully)
                        long executionTime = (startTime != null) ? 
                            (System.currentTimeMillis() - startTime) : 0;
                        
                        // Gracefully handle missing getResult (method might have thrown)
                        byte[] response = null;
                        try {
                            response = (byte[]) param.getResult();
                        } catch (Throwable t) {
                            // Method threw exception - response remains null
                        }
                        String responseHex = response != null ? LogBroadcaster.bytesToHex(response) : null;
                        
                        // Create structured log entry using factory method
                        CallLogEntry entry = CallLogEntry.createTransmitEntry(
                            lpparam.packageName,
                            "[SYSTEM] Terminal.transmit",
                            commandHex,
                            responseHex,
                            executionTime
                        );
                        
                        broadcaster.logMessage(entry);
                    } catch (Throwable t) {
                        // Absorb all exceptions
                        try {
                            CallLogEntry errorEntry = CallLogEntry.createErrorEntry(
                                lpparam.packageName,
                                "[SYSTEM] Terminal.transmit",
                                Constants.TYPE_TRANSMIT,
                                "Error logging transmit: " + t.getMessage()
                            );
                            broadcaster.logMessage(errorEntry);
                        } catch (Throwable ignored) {
                            // If even error logging fails, silently ignore
                        }
                    } finally {
                        // Critical: Always clean up ThreadLocal to avoid leaks
                        commandHexTL.remove();
                        startTimeTL.remove();
                    }
                }
            });
        } catch (Throwable t) {
            // Terminal class might not exist - silently ignore
        }
    }
}
