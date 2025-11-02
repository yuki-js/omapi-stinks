package app.aoki.yuki.omapistinks.hooks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * Base class for OMAPI hooking functionality
 */
public abstract class OmapiHooks {
    protected static final String TAG = "OmapiStinks";
    protected final LogDispatcher logDispatcher;

    public OmapiHooks(LogDispatcher logDispatcher) {
        this.logDispatcher = logDispatcher;
    }

    /**
     * Hook OMAPI classes in the target package
     * @param lpparam Package parameters
     * @param packagePrefix OMAPI package prefix (e.g., "android.se.omapi")
     * @return true if hooks were successfully installed
     */
    public abstract boolean hookPackage(LoadPackageParam lpparam, String packagePrefix);

    protected void logCall(String message) {
        XposedBridge.log(TAG + ": " + message);
        logDispatcher.dispatchLog(message);
    }

    protected static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
