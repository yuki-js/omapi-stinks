package app.aoki.yuki.omapistinks.xposed.hooks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import app.aoki.yuki.omapistinks.core.CallLogEntry;
import app.aoki.yuki.omapistinks.core.Constants;
import app.aoki.yuki.omapistinks.xposed.LogBroadcaster;
import app.aoki.yuki.omapistinks.xposed.hooks.SessionOpenChannelHook;

/**
 * Hooks Channel.transmit() method to capture APDU command and response
 * 
 * Thread-safety: Uses ThreadLocal variables to avoid races across concurrent calls.
 * Exception-safety: All exceptions are caught and never propagate to hooked methods.
 */
public class ChannelTransmitHook {
    
    // ThreadLocal variables to store per-call state (thread-safe for concurrent calls)
    private static final ThreadLocal<String> commandHexTL = new ThreadLocal<>();
    private static final ThreadLocal<Long> startTimeTL = new ThreadLocal<>();
    
    public static void hook(LoadPackageParam lpparam, String className, LogBroadcaster broadcaster) {
        try {
            Class<?> clazz = XposedHelpers.findClass(className, lpparam.classLoader);
            XposedHelpers.findAndHookMethod(clazz, "transmit", byte[].class, new XC_MethodHook() {
                
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

                        // Resolve Channel instance and associated AID (if any)
                        Object channel = param.thisObject;
                        String aidHex = SessionOpenChannelHook.getAidForChannel(channel);
                        
                        // Create structured log entry using factory method (includes AID when available)
                        CallLogEntry entry = CallLogEntry.createTransmitEntry(
                            lpparam.packageName,
                            "Channel.transmit",
                            commandHex,
                            responseHex,
                            aidHex,
                            executionTime
                        );
                        
                        broadcaster.logMessage(entry);
                    } catch (Throwable t) {
                        // Absorb all exceptions - hook should never interfere with app
                        // Try to log error entry, but don't let it fail the hook
                        try {
                            CallLogEntry errorEntry = CallLogEntry.createErrorEntry(
                                lpparam.packageName,
                                "Channel.transmit",
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
            // Method might not exist in this package - silently ignore
        }
    }

    public static void hookClose(LoadPackageParam lpparam, String className, LogBroadcaster broadcaster) {
        try {
            Class<?> clazz = XposedHelpers.findClass(className, lpparam.classLoader);
            XposedHelpers.findAndHookMethod(clazz, "close", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // Wrap in try/catch to ensure no exceptions propagate to hooked method
                    try {
                        Object channel = param.thisObject;
                        SessionOpenChannelHook.removeChannel(channel);
                    } catch (Throwable t) {
                        // Absorb all exceptions
                        try {
                            CallLogEntry errorEntry = CallLogEntry.createErrorEntry(
                                lpparam.packageName,
                                "Channel.close",
                                Constants.TYPE_CLOSE,
                                "Error cleaning AID mapping: " + t.getMessage()
                            );
                            broadcaster.logMessage(errorEntry);
                        } catch (Throwable ignored) {
                            // If even error logging fails, silently ignore
                        }
                    }
                }
            });
        } catch (Throwable t) {
            // Method might not exist in this package - silently ignore
        }
    }
}
