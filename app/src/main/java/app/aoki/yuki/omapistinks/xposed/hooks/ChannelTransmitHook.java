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
                private String commandHex;
                private long startTime;
                private String callStack;
                
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    byte[] command = (byte[]) param.args[0];
                    commandHex = LogBroadcaster.bytesToHex(command);
                    startTime = System.currentTimeMillis();
                    // Capture call stack from the hooked method context
                    callStack = CallLogEntry.captureCallStack();
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
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
                        CallLogEntry errorEntry = CallLogEntry.createErrorEntry(
                            lpparam.packageName,
                            "Channel.transmit",
                            Constants.TYPE_TRANSMIT,
                            "Error logging transmit: " + t.getMessage()
                        );
                        broadcaster.logMessage(errorEntry);
                    }
                }
            });
        } catch (Throwable t) {
            // Method might not exist in this package
        }
    }
}
