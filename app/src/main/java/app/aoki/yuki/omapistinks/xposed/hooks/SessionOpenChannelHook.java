package app.aoki.yuki.omapistinks.xposed.hooks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import app.aoki.yuki.omapistinks.core.CallLogEntry;
import app.aoki.yuki.omapistinks.core.Constants;
import app.aoki.yuki.omapistinks.xposed.LogBroadcaster;

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
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    long startTime = System.currentTimeMillis();
                    String callStack = CallLogEntry.captureCallStack();
                    
                    // Store values in param extras to avoid race conditions
                    param.setObjectExtra("startTime", startTime);
                    param.setObjectExtra("callStack", callStack);
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        // Retrieve values from param extras (thread-safe)
                        long startTime = (Long) param.getObjectExtra("startTime");
                        String callStack = (String) param.getObjectExtra("callStack");
                        
                        long executionTime = System.currentTimeMillis() - startTime;
                        byte[] aid = (byte[]) param.args[0];
                        String aidHex = LogBroadcaster.bytesToHex(aid);
                        Object channel = param.getResult();
                        
                        // Get select response from channel
                        String selectResponse = extractSelectResponse(channel);
                        
                        CallLogEntry entry = new CallLogEntry.Builder()
                            .packageName(lpparam.packageName)
                            .functionName("Session." + methodName)
                            .type(Constants.TYPE_OPEN_CHANNEL)
                            .aid(aidHex)
                            .selectResponse(selectResponse)
                            .executionTimeMs(executionTime)
                            .stackTrace(callStack)
                            .build();
                        
                        broadcaster.logMessage(entry);
                    } catch (Throwable t) {
                        // Log error if something went wrong
                        String callStack = (String) param.getObjectExtra("callStack");
                        CallLogEntry.Builder errorBuilder = new CallLogEntry.Builder()
                            .packageName(lpparam.packageName)
                            .functionName("Session." + methodName)
                            .type(Constants.TYPE_OPEN_CHANNEL)
                            .error("Error logging open channel: " + t.getMessage());
                        if (callStack != null) {
                            errorBuilder.stackTrace(callStack);
                        }
                        broadcaster.logMessage(errorBuilder.build());
                    }
                }
            });
            
            // Hook version with byte[] aid and byte P2
            XposedHelpers.findAndHookMethod(clazz, methodName, byte[].class, byte.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    long startTime = System.currentTimeMillis();
                    String callStack = CallLogEntry.captureCallStack();
                    
                    // Store values in param extras to avoid race conditions
                    param.setObjectExtra("startTime", startTime);
                    param.setObjectExtra("callStack", callStack);
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        // Retrieve values from param extras (thread-safe)
                        long startTime = (Long) param.getObjectExtra("startTime");
                        String callStack = (String) param.getObjectExtra("callStack");
                        
                        long executionTime = System.currentTimeMillis() - startTime;
                        byte[] aid = (byte[]) param.args[0];
                        byte p2 = (byte) param.args[1];
                        String aidHex = LogBroadcaster.bytesToHex(aid);
                        Object channel = param.getResult();
                        
                        // Get select response from channel
                        String selectResponse = extractSelectResponse(channel);
                        
                        CallLogEntry entry = new CallLogEntry.Builder()
                            .packageName(lpparam.packageName)
                            .functionName("Session." + methodName + "(P2=0x" + String.format("%02X", p2) + ")")
                            .type(Constants.TYPE_OPEN_CHANNEL)
                            .aid(aidHex)
                            .selectResponse(selectResponse)
                            .executionTimeMs(executionTime)
                            .stackTrace(callStack)
                            .build();
                        
                        broadcaster.logMessage(entry);
                    } catch (Throwable t) {
                        // Log error if something went wrong
                        String callStack = (String) param.getObjectExtra("callStack");
                        CallLogEntry.Builder errorBuilder = new CallLogEntry.Builder()
                            .packageName(lpparam.packageName)
                            .functionName("Session." + methodName)
                            .type(Constants.TYPE_OPEN_CHANNEL)
                            .error("Error logging open channel: " + t.getMessage());
                        if (callStack != null) {
                            errorBuilder.stackTrace(callStack);
                        }
                        broadcaster.logMessage(errorBuilder.build());
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
