package app.aoki.yuki.omapistinks;

import app.aoki.yuki.omapistinks.hooks.ClientOmapiHooks;
import app.aoki.yuki.omapistinks.hooks.LogDispatcher;
import app.aoki.yuki.omapistinks.hooks.SystemOmapiHooks;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * Main Xposed initialization class for OMAPI Stinks
 * This class is kept minimal and delegates to specialized hook classes
 */
public class XposedInit implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static final String TAG = "OmapiStinks";

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        // Initialize file logging early in the zygote
        XposedBridge.log(TAG + ": Module loaded in zygote");
        LogDispatcher zygoteDispatcher = new LogDispatcher("zygote");
        zygoteDispatcher.dispatchLog("=== OMAPI Stinks Module Loaded in Zygote ===");
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        String packageName = lpparam.packageName;
        String processName = lpparam.processName;
        
        // Create log dispatcher for this process
        LogDispatcher logDispatcher = new LogDispatcher(processName);
        logDispatcher.dispatchLog("=== Package Loaded: " + packageName + " (process: " + processName + ") ===");
        XposedBridge.log(TAG + ": Loaded into package: " + packageName + " (process: " + processName + ")");
        
        boolean hooked = false;
        
        // Hook client-side OMAPI packages
        ClientOmapiHooks clientHooks = new ClientOmapiHooks(logDispatcher);
        hooked |= clientHooks.hookPackage(lpparam, "org.simalliance.openmobileapi");
        hooked |= clientHooks.hookPackage(lpparam, "android.se.omapi");
        
        // Hook system SecureElement service (com.android.se)
        if ("com.android.se".equals(packageName) || processName.contains("com.android.se")) {
            logDispatcher.dispatchLog("*** Detected SecureElement Service process ***");
            XposedBridge.log(TAG + ": Hooking SecureElement Service");
            SystemOmapiHooks systemHooks = new SystemOmapiHooks(logDispatcher);
            hooked |= systemHooks.hookSecureElementService(lpparam);
        }
        
        // Hook system_server for SE management
        if ("android".equals(packageName) && "system_server".equals(processName)) {
            logDispatcher.dispatchLog("*** Detected system_server process ***");
            XposedBridge.log(TAG + ": In system_server process");
            // Additional system hooks can be added here if needed
        }
        
        if (hooked) {
            logDispatcher.dispatchLog("Successfully hooked OMAPI in " + packageName);
            XposedBridge.log(TAG + ": Successfully hooked OMAPI in " + packageName);
        }
    }
}
