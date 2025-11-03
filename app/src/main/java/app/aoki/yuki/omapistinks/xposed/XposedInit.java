package app.aoki.yuki.omapistinks.xposed;

import android.content.Context;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import app.aoki.yuki.omapistinks.core.CallLogEntry;
import app.aoki.yuki.omapistinks.core.Constants;
import app.aoki.yuki.omapistinks.xposed.hooks.ChannelTransmitHook;
import app.aoki.yuki.omapistinks.xposed.hooks.SessionOpenChannelHook;
import app.aoki.yuki.omapistinks.xposed.hooks.TerminalTransmitHook;

/**
 * Xposed Module entry point for hooking OMAPI calls
 * Hooks are organized by functionality in separate classes
 */
public class XposedInit implements IXposedHookLoadPackage {

    private Context appContext;
    private LogBroadcaster broadcaster;

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        // Hook Application.attach to get context for broadcasting
        hookApplicationContext(lpparam);
        
        // Initialize broadcaster (will be set after context is available)
        broadcaster = new LogBroadcaster(appContext, lpparam.packageName);
        
        // Hook system SecureElement service
        if (lpparam.packageName.equals("com.android.se")) {
            hookSystemService(lpparam);
        }
        
        // Hook client-side OMAPI for ALL packages (no whitelist)
        // This allows hooking any app that uses OMAPI
        hookClientOmapi(lpparam);
    }
    
    private void hookApplicationContext(LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod("android.app.Application", lpparam.classLoader,
                    "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        appContext = (Context) param.args[0];
                        // Update broadcaster with context
                        broadcaster = new LogBroadcaster(appContext, lpparam.packageName);
                        
                        // Log the hook notification
                        CallLogEntry hookEntry = CallLogEntry.createHookEntry(
                            lpparam.packageName,
                            "Application.attach",
                            "OMAPI hooks installed for package: " + lpparam.packageName
                        );
                        broadcaster.logMessage(hookEntry);
                    } catch (Throwable t) {
                        // Log error if something went wrong in the hook
                        if (broadcaster != null) {
                            CallLogEntry errorEntry = CallLogEntry.createErrorEntry(
                                lpparam.packageName,
                                "Application.attach",
                                Constants.TYPE_OTHER,
                                "Error in attach hook: " + t.getMessage()
                            );
                            broadcaster.logMessage(errorEntry);
                        }
                    }
                }
            });
        } catch (Throwable t) {
            // Context hook failed, broadcaster will work without context (Xposed log only)
        }
    }
    
    private void hookClientOmapi(LoadPackageParam lpparam) {
        // Hook modern android.se.omapi package (Android 9+)
        hookOmapiPackage(lpparam, "android.se.omapi");
        
        // Hook legacy org.simalliance.openmobileapi package
        hookOmapiPackage(lpparam, "org.simalliance.openmobileapi");
    }
    
    private void hookOmapiPackage(LoadPackageParam lpparam, String packagePrefix) {
        // Hook Channel.transmit - captures APDU command and response
        ChannelTransmitHook.hook(lpparam, packagePrefix + ".Channel", broadcaster);
        
        // Hook Session.openBasicChannel - captures AID and select response
        SessionOpenChannelHook.hookBasicChannel(lpparam, packagePrefix + ".Session", broadcaster);
        
        // Hook Session.openLogicalChannel - captures AID and select response
        SessionOpenChannelHook.hookLogicalChannel(lpparam, packagePrefix + ".Session", broadcaster);
    }
    
    private void hookSystemService(LoadPackageParam lpparam) {
        // Hook com.android.se.Terminal.transmit() - system-wide APDU monitoring
        TerminalTransmitHook.hook(lpparam, broadcaster);
    }
}
