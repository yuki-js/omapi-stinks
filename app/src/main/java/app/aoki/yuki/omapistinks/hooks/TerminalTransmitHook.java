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
                
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    byte[] command = (byte[]) param.args[0];
                    commandHex = LogBroadcaster.bytesToHex(command);
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    byte[] response = (byte[]) param.getResult();
                    String responseHex = response != null ? LogBroadcaster.bytesToHex(response) : null;
                    
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
                        null  // no details for transmit
                    );
                    
                    broadcaster.logMessage(entry);
                }
            });
        } catch (Throwable t) {
            // Terminal class might not exist
        }
    }
}
