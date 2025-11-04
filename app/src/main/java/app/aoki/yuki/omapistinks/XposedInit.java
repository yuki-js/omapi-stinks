package app.aoki.yuki.omapistinks;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.AndroidAppHelper;

public class XposedInit implements de.robv.android.xposed.IXposedHookLoadPackage {
    private Context appContext;
    private LogBroadcaster broadcaster;

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        // Hook Application.attach to get context for broadcasting
        hookApplicationContext(lpparam);

        // Provide a ContextProvider that prefers the hooked appContext, falls back to AndroidAppHelper
        ContextProvider provider = new ContextProvider() {
            @Override
            public Context getContext() {
                // Prefer the hooked context if available
                if (appContext != null) {
                    return appContext;
                }
                // Fallback to Xposed API to get current application
                try {
                    Context fallback = AndroidAppHelper.currentApplication();
                    if (fallback != null) return fallback;
                } catch (Throwable t) {
                    XposedBridge.log("OmapiStinks: AndroidAppHelper.currentApplication() failed: " + t);
                }
                return null;
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
                    appContext = (Context) param.args[0];
                    // No need to recreate broadcaster; provider reads appContext lazily
                }
            });
        } catch (Throwable t) {
            // Context hook failed, broadcaster will work using fallback (AndroidAppHelper) or Xposed log only
        }
    }

    // rest of original file unchanged (hookClientOmapi, hookOmapiPackage, hookSystemService etc.)
}