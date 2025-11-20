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
        
        // Provide a ContextProvider that resolves context lazily
        // This allows the broadcaster to work even when context is initially null
        ContextProvider provider = new ContextProvider() {
            @Override
            public Context getContext() {
                // Return the hooked context (may be null initially, but will be set later)
                return appContext;
            }
        };
        
        // Initialize broadcaster with provider (will resolve context lazily)
        broadcaster = new LogBroadcaster(provider, lpparam.packageName);
        
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
                    // Wrap in try/catch to ensure no exceptions propagate to hooked method
                    try {
                        appContext = (Context) param.args[0];
                        // No need to recreate broadcaster; provider reads appContext lazily
                        
                        // Log the hook notification (broadcaster will use the updated appContext)
                        if (broadcaster != null) {
                            CallLogEntry hookEntry = CallLogEntry.createHookEntry(
                                lpparam.packageName,
                                "Application.attach",
                                "OMAPI hooks installed for package: " + lpparam.packageName
                            );
                            broadcaster.logMessage(hookEntry);
                        }
                    } catch (Throwable t) {
                        // Absorb all exceptions
                        try {
                            if (broadcaster != null) {
                                CallLogEntry errorEntry = CallLogEntry.createErrorEntry(
                                    lpparam.packageName,
                                    "Application.attach",
                                    Constants.TYPE_OTHER,
                                    "Error in attach hook: " + t.getMessage()
                                );
                                broadcaster.logMessage(errorEntry);
                            }
                        } catch (Throwable ignored) {
                            // If even error logging fails, silently ignore
                        }
                    }
                }
            });
        } catch (Throwable t) {
            // Context hook failed, broadcaster will work without context (Xposed log only)
            // Note: This error is logged to Xposed log only (not via broadcaster)
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
        // Hook Channel.close - drop Channel->AID mapping on close
        ChannelTransmitHook.hookClose(lpparam, packagePrefix + ".Channel", broadcaster);
        
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
