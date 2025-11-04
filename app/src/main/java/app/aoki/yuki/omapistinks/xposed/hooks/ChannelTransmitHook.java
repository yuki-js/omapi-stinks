package app.aoki.yuki.omapistinks.xposed.hooks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import app.aoki.yuki.omapistinks.core.CallLogEntry;
import app.aoki.yuki.omapistinks.core.Constants;
import app.aoki.yuki.omapistinks.xposed.LogBroadcaster;

/**
 * Hooks Channel.transmit() method to capture APDU command and response
 */
public class ChannelTransmitHook {
    
    public static void hook(LoadPackageParam lpparam, String className, LogBroadcaster broadcaster) {
        try {
            Class<?> clazz = XposedHelpers.findClass(className, lpparam.classLoader);
            XposedHelpers.findAndHookMethod(clazz, "transmit", byte[].class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    byte[] command = (byte[]) param.args[0];
                    String commandHex = LogBroadcaster.bytesToHex(command);
                    long startTime = System.currentTimeMillis();
                    String callStack = CallLogEntry.captureCallStack();
                    
                    // Store values in param extras to avoid race conditions
                    param.setObjectExtra("commandHex", commandHex);
                    param.setObjectExtra("startTime", startTime);
                    param.setObjectExtra("callStack", callStack);
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        // Retrieve values from param extras (thread-safe)
                        String commandHex = (String) param.getObjectExtra("commandHex");
                        long startTime = (Long) param.getObjectExtra("startTime");
                        String callStack = (String) param.getObjectExtra("callStack");
                        
                        long executionTime = System.currentTimeMillis() - startTime;
                        byte[] response = (byte[]) param.getResult();
                        String responseHex = response != null ? LogBroadcaster.bytesToHex(response) : null;
                        
                        // Create structured log entry with call stack
                        CallLogEntry entry = new CallLogEntry.Builder()
                            .packageName(lpparam.packageName)
                            .functionName("Channel.transmit")
                            .type(Constants.TYPE_TRANSMIT)
                            .apduCommand(commandHex)
                            .apduResponse(responseHex)
                            .executionTimeMs(executionTime)
                            .stackTrace(callStack)
                            .build();
                        
                        broadcaster.logMessage(entry);
                    } catch (Throwable t) {
                        // Log error if something went wrong
                        String callStack = (String) param.getObjectExtra("callStack");
                        CallLogEntry errorEntry = new CallLogEntry.Builder()
                            .packageName(lpparam.packageName)
                            .functionName("Channel.transmit")
                            .type(Constants.TYPE_TRANSMIT)
                            .error("Error logging transmit: " + t.getMessage())
                            .stackTrace(callStack)
                            .build();
                        broadcaster.logMessage(errorEntry);
                    }
                }
            });
        } catch (Throwable t) {
            // Method might not exist in this package
        }
    }
}
