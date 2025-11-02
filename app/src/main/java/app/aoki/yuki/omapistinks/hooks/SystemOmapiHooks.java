package app.aoki.yuki.omapistinks.hooks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * Hooks for system-level OMAPI components (com.android.se service)
 * These hooks capture ALL OMAPI activity at the system service level
 */
public class SystemOmapiHooks extends OmapiHooks {

    public SystemOmapiHooks(LogDispatcher logDispatcher) {
        super(logDispatcher);
    }

    @Override
    public boolean hookPackage(LoadPackageParam lpparam, String packagePrefix) {
        // This method is not used for system hooks
        return false;
    }

    /**
     * Hook the SecureElement system service (com.android.se)
     * This is the backend service that handles all OMAPI calls system-wide
     */
    public boolean hookSecureElementService(LoadPackageParam lpparam) {
        boolean success = false;
        try {
            logDispatcher.dispatchLog("Attempting to hook SecureElement Service implementation...");
            
            // Hook Terminal class for low-level APDU
            success |= hookTerminalClass(lpparam);
            
            // Hook ChannelAccess for access control info
            success |= hookChannelAccessClass(lpparam);
            
            // Try to hook the AIDL service methods
            success |= hookSecureElementServiceStub(lpparam);
            
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook SecureElement service: " + t.getMessage());
            logDispatcher.dispatchLog("Failed to hook SecureElement service: " + t.getMessage());
            t.printStackTrace();
        }
        
        return success;
    }

    private boolean hookTerminalClass(LoadPackageParam lpparam) {
        try {
            Class<?> terminalClass = XposedHelpers.findClass("com.android.se.Terminal", lpparam.classLoader);
            logDispatcher.dispatchLog("Found Terminal class, hooking transmit...");
            
            // Hook transmit method - this is where ALL APDUs go through
            XposedHelpers.findAndHookMethod(terminalClass, "transmit", byte[].class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    byte[] command = (byte[]) param.args[0];
                    String terminalName = "Unknown";
                    try {
                        Object terminal = param.thisObject;
                        terminalName = (String) XposedHelpers.getObjectField(terminal, "mName");
                    } catch (Throwable t) {
                        // Ignore
                    }
                    logCall("[SYSTEM/Terminal:" + terminalName + "] transmit(command=" + bytesToHex(command) + ")");
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    byte[] response = (byte[]) param.getResult();
                    String terminalName = "Unknown";
                    try {
                        Object terminal = param.thisObject;
                        terminalName = (String) XposedHelpers.getObjectField(terminal, "mName");
                    } catch (Throwable t) {
                        // Ignore
                    }
                    logCall("[SYSTEM/Terminal:" + terminalName + "] transmit() returned " + bytesToHex(response));
                }
            });
            
            logDispatcher.dispatchLog("Successfully hooked Terminal.transmit()");
            return true;
        } catch (Throwable t) {
            logDispatcher.dispatchLog("Failed to hook Terminal: " + t.getMessage());
            return false;
        }
    }

    private boolean hookChannelAccessClass(LoadPackageParam lpparam) {
        try {
            Class<?> channelAccessClass = XposedHelpers.findClass("com.android.se.security.ChannelAccess", lpparam.classLoader);
            logDispatcher.dispatchLog("Found ChannelAccess class");
            
            XposedHelpers.findAndHookMethod(channelAccessClass, "setCallingPid", int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    int pid = (Integer) param.args[0];
                    logCall("[SYSTEM/Access] Channel access from PID: " + pid);
                }
            });
            
            return true;
        } catch (Throwable t) {
            logDispatcher.dispatchLog("Failed to hook ChannelAccess: " + t.getMessage());
            return false;
        }
    }

    private boolean hookSecureElementServiceStub(LoadPackageParam lpparam) {
        try {
            // Look for ISecureElementService$Stub
            Class<?> stubClass = XposedHelpers.findClass("android.se.omapi.ISecureElementService$Stub", lpparam.classLoader);
            logDispatcher.dispatchLog("Found ISecureElementService$Stub");
            
            // Hook onTransact to see all Binder calls
            XposedHelpers.findAndHookMethod(stubClass, "onTransact", 
                int.class, android.os.Parcel.class, android.os.Parcel.class, int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        int code = (Integer) param.args[0];
                        int callingUid = android.os.Binder.getCallingUid();
                        int callingPid = android.os.Binder.getCallingPid();
                        logCall("[SYSTEM/Binder] ISecureElementService call: code=" + code + 
                               " from UID=" + callingUid + " PID=" + callingPid);
                    }
                });
            
            return true;
        } catch (Throwable t) {
            logDispatcher.dispatchLog("Failed to hook ISecureElementService: " + t.getMessage());
            return false;
        }
    }
}
