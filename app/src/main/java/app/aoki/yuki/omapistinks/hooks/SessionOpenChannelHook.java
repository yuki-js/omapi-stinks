package app.aoki.yuki.omapistinks.hooks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import app.aoki.yuki.omapistinks.CallLogEntry;
import app.aoki.yuki.omapistinks.Constants;
import app.aoki.yuki.omapistinks.LogBroadcaster;

/**
 * Hooks Session.openBasicChannel() and Session.openLogicalChannel() methods
 */
public class SessionOpenChannelHook {
    
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
                private long startTime;
                
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    startTime = System.currentTimeMillis();
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        long executionTime = System.currentTimeMillis() - startTime;
                        byte[] aid = (byte[]) param.args[0];
                        String aidHex = LogBroadcaster.bytesToHex(aid);
                        Object channel = param.getResult();
                        
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
                        // Log error if something went wrong
                        CallLogEntry errorEntry = CallLogEntry.createErrorEntry(
                            lpparam.packageName,
                            "Session." + methodName,
                            Constants.TYPE_OPEN_CHANNEL,
                            "Error logging open channel: " + t.getMessage()
                        );
                        broadcaster.logMessage(errorEntry);
                    }
                }
            });
            
            // Hook version with byte[] aid and byte P2
            XposedHelpers.findAndHookMethod(clazz, methodName, byte[].class, byte.class, new XC_MethodHook() {
                private long startTime;
                
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    startTime = System.currentTimeMillis();
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        long executionTime = System.currentTimeMillis() - startTime;
                        byte[] aid = (byte[]) param.args[0];
                        byte p2 = (byte) param.args[1];
                        String aidHex = LogBroadcaster.bytesToHex(aid);
                        Object channel = param.getResult();
                        
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
                        // Log error if something went wrong
                        CallLogEntry errorEntry = CallLogEntry.createErrorEntry(
                            lpparam.packageName,
                            "Session." + methodName,
                            Constants.TYPE_OPEN_CHANNEL,
                            "Error logging open channel: " + t.getMessage()
                        );
                        broadcaster.logMessage(errorEntry);
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
