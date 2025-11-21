package app.aoki.yuki.omapistinks.xposed.hooks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import app.aoki.yuki.omapistinks.core.CallLogEntry;
import app.aoki.yuki.omapistinks.core.Constants;
import app.aoki.yuki.omapistinks.xposed.LogBroadcaster;

/**
 * Hooks Session.openBasicChannel() and Session.openLogicalChannel() methods
 * 
 * Thread-safety: Uses ThreadLocal variables to avoid races across concurrent calls.
 * Exception-safety: All exceptions are caught and never propagate to hooked methods.
 */
public class SessionOpenChannelHook {
    
    // Map Channel instance to associated AID (hex). Use WeakHashMap to avoid leaks across GC.
    private static final java.util.Map<Object, String> CHANNEL_AID_MAP = new java.util.WeakHashMap<>();
    
    // ThreadLocal variable to store per-call start time (thread-safe for concurrent calls)
    private static final ThreadLocal<Long> startTimeTL = new ThreadLocal<>();
    
    public static void setAidForChannel(Object channel, String aidHex) {
        if (channel != null && aidHex != null && !aidHex.isEmpty()) {
            CHANNEL_AID_MAP.put(channel, aidHex);
        }
    }
    
    public static String getAidForChannel(Object channel) {
        return channel != null ? CHANNEL_AID_MAP.get(channel) : null;
    }
    
    public static void removeChannel(Object channel) {
        if (channel != null) {
            CHANNEL_AID_MAP.remove(channel);
        }
    }
    
    public static void hookBasicChannel(LoadPackageParam lpparam, String className, LogBroadcaster broadcaster) {
        hookOpenChannel(lpparam, className, "openBasicChannel", broadcaster);
    }
    
    public static void hookLogicalChannel(LoadPackageParam lpparam, String className, LogBroadcaster broadcaster) {
        hookOpenChannel(lpparam, className, "openLogicalChannel", broadcaster);
    }
    
    private static void hookOpenChannel(LoadPackageParam lpparam, String className, String methodName, LogBroadcaster broadcaster) {
        try {
            Class<?> clazz = XposedHelpers.findClass(className, lpparam.classLoader);
            
            // Hook version with byte[] aid
            XposedHelpers.findAndHookMethod(clazz, methodName, byte[].class, new XC_MethodHook() {
                
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    // Wrap in try/catch to ensure no exceptions propagate to hooked method
                    try {
                        startTimeTL.set(System.currentTimeMillis());
                    } catch (Throwable t) {
                        // Absorb all exceptions - hook should never interfere with app
                    }
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // Wrap in try/catch to ensure no exceptions propagate to hooked method
                    try {
                        // Safely retrieve ThreadLocal value with default
                        Long startTime = startTimeTL.get();
                        long executionTime = (startTime != null) ? 
                            (System.currentTimeMillis() - startTime) : 0;
                        
                        byte[] aid = (byte[]) param.args[0];
                        String aidHex = LogBroadcaster.bytesToHex(aid);
                        Object channel = param.getResult();
                        
                        // Map AID to Channel for future transmit lookups
                        setAidForChannel(channel, aidHex);
                        
                        // Get select response from channel
                        String selectResponse = extractSelectResponse(channel);
                        
                        CallLogEntry entry = CallLogEntry.createOpenChannelEntry(
                            lpparam.packageName,
                            "Session." + methodName,
                            aidHex,
                            selectResponse,
                            executionTime
                        );
                        
                        broadcaster.logMessage(entry);
                    } catch (Throwable t) {
                        // Absorb all exceptions
                        try {
                            CallLogEntry errorEntry = CallLogEntry.createErrorEntry(
                                lpparam.packageName,
                                "Session." + methodName,
                                Constants.TYPE_OPEN_CHANNEL,
                                "Error logging open channel: " + t.getMessage()
                            );
                            broadcaster.logMessage(errorEntry);
                        } catch (Throwable ignored) {
                            // If even error logging fails, silently ignore
                        }
                    } finally {
                        // Critical: Always clean up ThreadLocal to avoid leaks
                        startTimeTL.remove();
                    }
                }
            });
            
            // Hook version with byte[] aid and byte P2
            XposedHelpers.findAndHookMethod(clazz, methodName, byte[].class, byte.class, new XC_MethodHook() {
                
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    // Wrap in try/catch to ensure no exceptions propagate to hooked method
                    try {
                        startTimeTL.set(System.currentTimeMillis());
                    } catch (Throwable t) {
                        // Absorb all exceptions - hook should never interfere with app
                    }
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // Wrap in try/catch to ensure no exceptions propagate to hooked method
                    try {
                        // Safely retrieve ThreadLocal value with default
                        Long startTime = startTimeTL.get();
                        long executionTime = (startTime != null) ? 
                            (System.currentTimeMillis() - startTime) : 0;
                        
                        byte[] aid = (byte[]) param.args[0];
                        byte p2 = (byte) param.args[1];
                        String aidHex = LogBroadcaster.bytesToHex(aid);
                        Object channel = param.getResult();
                        
                        // Map AID to Channel for future transmit lookups
                        setAidForChannel(channel, aidHex);
                        
                        // Get select response from channel
                        String selectResponse = extractSelectResponse(channel);
                        
                        CallLogEntry entry = CallLogEntry.createOpenChannelEntry(
                            lpparam.packageName,
                            "Session." + methodName + "(P2=0x" + String.format("%02X", p2) + ")",
                            aidHex,
                            selectResponse,
                            executionTime
                        );
                        
                        broadcaster.logMessage(entry);
                    } catch (Throwable t) {
                        // Absorb all exceptions
                        try {
                            CallLogEntry errorEntry = CallLogEntry.createErrorEntry(
                                lpparam.packageName,
                                "Session." + methodName,
                                Constants.TYPE_OPEN_CHANNEL,
                                "Error logging open channel: " + t.getMessage()
                            );
                            broadcaster.logMessage(errorEntry);
                        } catch (Throwable ignored) {
                            // If even error logging fails, silently ignore
                        }
                    } finally {
                        // Critical: Always clean up ThreadLocal to avoid leaks
                        startTimeTL.remove();
                    }
                }
            });
        } catch (Throwable t) {
            // Method might not exist
        }
    }
    
    private static String extractSelectResponse(Object channel) {
        if (channel == null) {
            return null;
        }
        
        try {
            byte[] selectResp = (byte[]) XposedHelpers.callMethod(channel, "getSelectResponse");
            if (selectResp != null) {
                return LogBroadcaster.bytesToHex(selectResp);
            }
        } catch (Throwable t) {
            // getSelectResponse might not be available
        }
        
        return null;
    }
}
