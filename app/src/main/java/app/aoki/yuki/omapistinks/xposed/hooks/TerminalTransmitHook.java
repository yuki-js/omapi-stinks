package app.aoki.yuki.omapistinks.xposed.hooks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import app.aoki.yuki.omapistinks.core.CallLogEntry;
import app.aoki.yuki.omapistinks.core.Constants;
import app.aoki.yuki.omapistinks.xposed.LogBroadcaster;

/**
 * Hooks com.android.se.Terminal.transmit() for system-level APDU monitoring
 */
public class TerminalTransmitHook {
    
    public static void hook(LoadPackageParam lpparam, LogBroadcaster broadcaster) {
        try {
            Class<?> terminalClass = XposedHelpers.findClass("com.android.se.Terminal", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(terminalClass, "transmit", byte[].class, new XC_MethodHook() {
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
                            .functionName("[SYSTEM] Terminal.transmit")
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
                        CallLogEntry.Builder errorBuilder = new CallLogEntry.Builder()
                            .packageName(lpparam.packageName)
                            .functionName("[SYSTEM] Terminal.transmit")
                            .type(Constants.TYPE_TRANSMIT)
                            .error("Error logging transmit: " + t.getMessage());
                        if (callStack != null) {
                            errorBuilder.stackTrace(callStack);
                        }
                        broadcaster.logMessage(errorBuilder.build());
                    }
                }
            });
        } catch (Throwable t) {
            // Terminal class might not exist
        }
    }
}
