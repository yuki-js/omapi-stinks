package app.aoki.yuki.omapistinks;

import android.content.Context;
import android.content.Intent;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Xposed Module for hooking OMAPI calls
 * Sends all logs via broadcast to the UI app
 */
public class XposedInit implements IXposedHookLoadPackage {

    private static final String TAG = "OmapiStinks";
    
    private SimpleDateFormat dateFormat;
    private Context appContext;
    private String currentPackageName;

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        currentPackageName = lpparam.packageName;
        
        // Try to get application context for sending broadcasts
        try {
            XposedHelpers.findAndHookMethod("android.app.Application", lpparam.classLoader,
                    "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    appContext = (Context) param.args[0];
                }
            });
        } catch (Throwable t) {
            // Context hook failed, will use XposedBridge logging only
        }
        
        // Hook client-side OMAPI (apps using OMAPI)
        if (lpparam.packageName.equals("android") || isOmapiPackage(lpparam.packageName)) {
            hookClientOmapi(lpparam);
        }
        
        // Hook system SecureElement service
        if (lpparam.packageName.equals("com.android.se")) {
            hookSystemService(lpparam);
        }
    }
    
    private boolean isOmapiPackage(String packageName) {
        // Add common packages that use OMAPI
        return packageName.contains("wallet") || 
               packageName.contains("pay") || 
               packageName.contains("nfc") ||
               packageName.equals("com.google.android.gms") ||
               packageName.equals("com.google.android.apps.walletnfcrel") ||
               packageName.equals("com.samsung.android.spay") ||
               packageName.equals("com.android.stk");
    }
    
    private void hookClientOmapi(LoadPackageParam lpparam) {
        // Hook modern android.se.omapi package
        hookOmapiPackage(lpparam, "android.se.omapi");
        
        // Hook legacy org.simalliance.openmobileapi package
        hookOmapiPackage(lpparam, "org.simalliance.openmobileapi");
    }
    
    private void hookOmapiPackage(LoadPackageParam lpparam, String packagePrefix) {
        try {
            // Hook SEService
            hookClass(lpparam, packagePrefix + ".SEService", "getReaders", "SEService.getReaders()");
            hookClass(lpparam, packagePrefix + ".SEService", "isConnected", "SEService.isConnected()");
            hookClass(lpparam, packagePrefix + ".SEService", "shutdown", "SEService.shutdown()");
            
            // Hook Reader
            hookClass(lpparam, packagePrefix + ".Reader", "getName", "Reader.getName()");
            hookClass(lpparam, packagePrefix + ".Reader", "isSecureElementPresent", "Reader.isSecureElementPresent()");
            hookClass(lpparam, packagePrefix + ".Reader", "openSession", "Reader.openSession()");
            hookClass(lpparam, packagePrefix + ".Reader", "closeSessions", "Reader.closeSessions()");
            
            // Hook Session
            hookClass(lpparam, packagePrefix + ".Session", "getATR", "Session.getATR()");
            hookClass(lpparam, packagePrefix + ".Session", "close", "Session.close()");
            hookClass(lpparam, packagePrefix + ".Session", "isClosed", "Session.isClosed()");
            hookClass(lpparam, packagePrefix + ".Session", "closeChannels", "Session.closeChannels()");
            
            // Hook Session openBasicChannel
            hookClassWithByteArray(lpparam, packagePrefix + ".Session", "openBasicChannel", "Session.openBasicChannel");
            
            // Hook Session openLogicalChannel
            hookClassWithByteArray(lpparam, packagePrefix + ".Session", "openLogicalChannel", "Session.openLogicalChannel");
            
            // Hook Channel - most important for APDU capture
            hookClass(lpparam, packagePrefix + ".Channel", "close", "Channel.close()");
            hookClass(lpparam, packagePrefix + ".Channel", "isBasicChannel", "Channel.isBasicChannel()");
            hookClass(lpparam, packagePrefix + ".Channel", "isClosed", "Channel.isClosed()");
            hookClass(lpparam, packagePrefix + ".Channel", "getSelectResponse", "Channel.getSelectResponse()");
            
            // Hook Channel.transmit - captures all APDU commands and responses
            hookChannelTransmit(lpparam, packagePrefix + ".Channel");
            
            logMessage("Hooked " + packagePrefix + " in " + lpparam.packageName);
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Could not hook " + packagePrefix + ": " + t.getMessage());
        }
    }
    
    private void hookSystemService(LoadPackageParam lpparam) {
        try {
            // Hook com.android.se.Terminal.transmit() - ALL APDUs pass through here
            Class<?> terminalClass = XposedHelpers.findClass("com.android.se.Terminal", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(terminalClass, "transmit", byte[].class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    byte[] command = (byte[]) param.args[0];
                    String commandHex = bytesToHex(command);
                    logMessage("[SYSTEM] Terminal.transmit(command=" + commandHex + ")");
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    byte[] response = (byte[]) param.getResult();
                    if (response != null) {
                        String responseHex = bytesToHex(response);
                        logMessage("[SYSTEM] Terminal.transmit() returned " + responseHex);
                    }
                }
            });
            
            logMessage("Hooked com.android.se.Terminal in system service");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Could not hook system service: " + t.getMessage());
        }
    }
    
    private void hookClass(LoadPackageParam lpparam, String className, String methodName, String logPrefix) {
        try {
            Class<?> clazz = XposedHelpers.findClass(className, lpparam.classLoader);
            XposedHelpers.findAndHookMethod(clazz, methodName, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object result = param.getResult();
                    String resultStr = formatResult(result);
                    logMessage(logPrefix + " = " + resultStr);
                }
            });
        } catch (Throwable t) {
            // Method might not exist in this version, silently ignore
        }
    }
    
    private void hookClassWithByteArray(LoadPackageParam lpparam, String className, String methodName, String logPrefix) {
        try {
            Class<?> clazz = XposedHelpers.findClass(className, lpparam.classLoader);
            
            // Hook version with byte[] aid
            XposedHelpers.findAndHookMethod(clazz, methodName, byte[].class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    byte[] aid = (byte[]) param.args[0];
                    String aidHex = bytesToHex(aid);
                    logMessage(logPrefix + "(aid=" + aidHex + ")");
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object channel = param.getResult();
                    logMessage(logPrefix + "() returned " + channel);
                }
            });
            
            // Hook version with byte[] aid and byte P2
            XposedHelpers.findAndHookMethod(clazz, methodName, byte[].class, byte.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    byte[] aid = (byte[]) param.args[0];
                    byte p2 = (byte) param.args[1];
                    String aidHex = bytesToHex(aid);
                    logMessage(logPrefix + "(aid=" + aidHex + ", P2=0x" + String.format("%02X", p2) + ")");
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object channel = param.getResult();
                    logMessage(logPrefix + "() returned " + channel);
                }
            });
        } catch (Throwable t) {
            // Method might not exist, ignore
        }
    }
    
    private void hookChannelTransmit(LoadPackageParam lpparam, String className) {
        try {
            Class<?> clazz = XposedHelpers.findClass(className, lpparam.classLoader);
            XposedHelpers.findAndHookMethod(clazz, "transmit", byte[].class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    byte[] command = (byte[]) param.args[0];
                    String commandHex = bytesToHex(command);
                    logMessage("Channel.transmit(command=" + commandHex + ")");
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    byte[] response = (byte[]) param.getResult();
                    if (response != null) {
                        String responseHex = bytesToHex(response);
                        logMessage("Channel.transmit() returned " + responseHex);
                    }
                }
            });
        } catch (Throwable t) {
            // Method might not exist, ignore
        }
    }
    
    private void logMessage(String message) {
        try {
            // Always log to Xposed framework log
            XposedBridge.log(TAG + ": " + message);
            
            // Try to send broadcast to UI app if context is available
            if (appContext != null) {
                sendBroadcast(message);
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Error logging: " + t.getMessage());
        }
    }
    
    private void sendBroadcast(String message) {
        try {
            // Use explicit intent with ComponentName to ensure delivery
            Intent intent = new Intent(Constants.BROADCAST_ACTION);
            intent.setClassName(Constants.PACKAGE_NAME, Constants.PACKAGE_NAME + ".LogReceiver");
            intent.putExtra(Constants.EXTRA_MESSAGE, message);
            intent.putExtra(Constants.EXTRA_TIMESTAMP, dateFormat.format(new Date()));
            intent.putExtra(Constants.EXTRA_PACKAGE, currentPackageName);
            
            // Add FLAG_INCLUDE_STOPPED_PACKAGES to wake up the app if it's not running
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            
            appContext.sendBroadcast(intent);
        } catch (Throwable t) {
            // Log error for debugging
            XposedBridge.log(TAG + ": Failed to send broadcast: " + t.getMessage());
        }
    }
    
    private String formatResult(Object result) {
        if (result == null) {
            return "null";
        } else if (result instanceof byte[]) {
            return bytesToHex((byte[]) result);
        } else if (result instanceof Object[]) {
            Object[] arr = (Object[]) result;
            return arr.length + " items";
        } else {
            return result.toString();
        }
    }
    
    private String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
