package app.aoki.yuki.omapistinks;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class XposedInit implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static final String TAG = "OmapiStinks";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    private static File logFile;
    private static boolean fileLoggingAvailable = false;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        // Initialize file logging early in the zygote
        try {
            // Use /data/local/tmp which is accessible without special permissions
            File logDir = new File("/data/local/tmp/omapistinks");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            logFile = new File(logDir, "hooks.log");
            fileLoggingAvailable = true;
            
            logToFile("=== OMAPI Stinks Module Loaded in Zygote ===");
            XposedBridge.log(TAG + ": File logging initialized at " + logFile.getAbsolutePath());
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to initialize file logging: " + t.getMessage());
            fileLoggingAvailable = false;
        }
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        // Log when module is loaded into a process
        String packageName = lpparam.packageName;
        String processName = lpparam.processName;
        
        logToFile("=== Package Loaded: " + packageName + " (process: " + processName + ") ===");
        XposedBridge.log(TAG + ": Loaded into package: " + packageName + " (process: " + processName + ")");
        
        // Try to hook OMAPI packages
        boolean hooked = false;
        hooked |= hookOmapiPackage(lpparam, "org.simalliance.openmobileapi");
        hooked |= hookOmapiPackage(lpparam, "android.se.omapi");
        
        // Hook system SecureElement service (com.android.se)
        if ("com.android.se".equals(packageName) || processName.contains("com.android.se")) {
            logToFile("*** Detected SecureElement Service process ***");
            XposedBridge.log(TAG + ": Hooking SecureElement Service");
            hooked |= hookSecureElementService(lpparam);
        }
        
        // Hook system_server for SE management
        if ("android".equals(packageName) && "system_server".equals(processName)) {
            logToFile("*** Detected system_server process ***");
            XposedBridge.log(TAG + ": Hooking system_server");
            hooked |= hookSystemServer(lpparam);
        }
        
        if (hooked) {
            logToFile("Successfully hooked OMAPI in " + packageName);
            XposedBridge.log(TAG + ": Successfully hooked OMAPI in " + packageName);
        }
    }

    private boolean hookOmapiPackage(LoadPackageParam lpparam, String packagePrefix) {
        boolean success = false;
        try {
            // Try to find at least one OMAPI class to verify the package exists
            try {
                XposedHelpers.findClass(packagePrefix + ".SEService", lpparam.classLoader);
            } catch (Throwable t) {
                // Package doesn't exist in this app, skip silently
                return false;
            }
            
            logToFile("Found OMAPI package: " + packagePrefix + " in " + lpparam.packageName);
            XposedBridge.log(TAG + ": Found OMAPI package: " + packagePrefix);
            
            // Hook SEService
            success |= hookSEServiceClass(lpparam, packagePrefix);
            
            // Hook Reader
            success |= hookReaderClass(lpparam, packagePrefix);
            
            // Hook Session
            success |= hookSessionClass(lpparam, packagePrefix);
            
            // Hook Channel
            success |= hookChannelClass(lpparam, packagePrefix);
            
            if (success) {
                logToFile("Successfully hooked " + packagePrefix);
            }
            
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook " + packagePrefix + ": " + t.getMessage());
            logToFile("Failed to hook " + packagePrefix + ": " + t.getMessage());
        }
        return success;
    }

    private boolean hookSEServiceClass(LoadPackageParam lpparam, String packagePrefix) {
        try {
            Class<?> seServiceClass = XposedHelpers.findClass(packagePrefix + ".SEService", lpparam.classLoader);
            logToFile("Hooking SEService class: " + seServiceClass.getName());
            
            // Hook getReaders()
            XposedHelpers.findAndHookMethod(seServiceClass, "getReaders", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object[] readers = (Object[]) param.getResult();
                    String log = "SEService.getReaders() returned " + (readers != null ? readers.length : 0) + " readers";
                    logCall(log);
                }
            });

            // Hook isConnected()
            XposedHelpers.findAndHookMethod(seServiceClass, "isConnected", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    boolean result = (Boolean) param.getResult();
                    logCall("SEService.isConnected() = " + result);
                }
            });

            // Hook shutdown()
            XposedHelpers.findAndHookMethod(seServiceClass, "shutdown", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    logCall("SEService.shutdown()");
                }
            });

            // Hook getVersion()
            XposedHelpers.findAndHookMethod(seServiceClass, "getVersion", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String version = (String) param.getResult();
                    logCall("SEService.getVersion() = " + version);
                }
            });

            XposedBridge.log(TAG + ": Successfully hooked SEService in " + packagePrefix);
            logToFile("Successfully hooked SEService in " + packagePrefix);
            return true;
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook SEService: " + t.getMessage());
            logToFile("Failed to hook SEService: " + t.getMessage());
            return false;
        }
    }

    private boolean hookReaderClass(LoadPackageParam lpparam, String packagePrefix) {
        try {
            Class<?> readerClass = XposedHelpers.findClass(packagePrefix + ".Reader", lpparam.classLoader);

            // Hook getName()
            XposedHelpers.findAndHookMethod(readerClass, "getName", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String name = (String) param.getResult();
                    logCall("Reader.getName() = " + name);
                }
            });

            // Hook isSecureElementPresent()
            XposedHelpers.findAndHookMethod(readerClass, "isSecureElementPresent", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    boolean present = (Boolean) param.getResult();
                    logCall("Reader.isSecureElementPresent() = " + present);
                }
            });

            // Hook openSession()
            XposedHelpers.findAndHookMethod(readerClass, "openSession", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object session = param.getResult();
                    logCall("Reader.openSession() = " + (session != null ? "Session@" + Integer.toHexString(session.hashCode()) : "null"));
                }
            });

            // Hook closeSessions()
            XposedHelpers.findAndHookMethod(readerClass, "closeSessions", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    logCall("Reader.closeSessions()");
                }
            });

            XposedBridge.log(TAG + ": Successfully hooked Reader in " + packagePrefix);
            return true;
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook Reader: " + t.getMessage());
            return false;
        }
    }

    private boolean hookSessionClass(LoadPackageParam lpparam, String packagePrefix) {
        try {
            Class<?> sessionClass = XposedHelpers.findClass(packagePrefix + ".Session", lpparam.classLoader);

            // Hook getReader()
            XposedHelpers.findAndHookMethod(sessionClass, "getReader", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object reader = param.getResult();
                    logCall("Session.getReader() = " + (reader != null ? "Reader@" + Integer.toHexString(reader.hashCode()) : "null"));
                }
            });

            // Hook getATR()
            XposedHelpers.findAndHookMethod(sessionClass, "getATR", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    byte[] atr = (byte[]) param.getResult();
                    logCall("Session.getATR() = " + bytesToHex(atr));
                }
            });

            // Hook close()
            XposedHelpers.findAndHookMethod(sessionClass, "close", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    logCall("Session.close()");
                }
            });

            // Hook isClosed()
            XposedHelpers.findAndHookMethod(sessionClass, "isClosed", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    boolean closed = (Boolean) param.getResult();
                    logCall("Session.isClosed() = " + closed);
                }
            });

            // Hook closeChannels()
            XposedHelpers.findAndHookMethod(sessionClass, "closeChannels", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    logCall("Session.closeChannels()");
                }
            });

            // Hook openBasicChannel with byte[] aid, byte P2
            XposedHelpers.findAndHookMethod(sessionClass, "openBasicChannel", byte[].class, byte.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    byte[] aid = (byte[]) param.args[0];
                    byte p2 = (Byte) param.args[1];
                    logCall("Session.openBasicChannel(aid=" + bytesToHex(aid) + ", P2=" + String.format("0x%02X", p2) + ")");
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object channel = param.getResult();
                    logCall("Session.openBasicChannel() returned " + (channel != null ? "Channel@" + Integer.toHexString(channel.hashCode()) : "null"));
                }
            });

            // Hook openBasicChannel with byte[] aid only
            XposedHelpers.findAndHookMethod(sessionClass, "openBasicChannel", byte[].class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    byte[] aid = (byte[]) param.args[0];
                    logCall("Session.openBasicChannel(aid=" + bytesToHex(aid) + ")");
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object channel = param.getResult();
                    logCall("Session.openBasicChannel() returned " + (channel != null ? "Channel@" + Integer.toHexString(channel.hashCode()) : "null"));
                }
            });

            // Hook openLogicalChannel with byte[] aid, byte P2
            XposedHelpers.findAndHookMethod(sessionClass, "openLogicalChannel", byte[].class, byte.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    byte[] aid = (byte[]) param.args[0];
                    byte p2 = (Byte) param.args[1];
                    logCall("Session.openLogicalChannel(aid=" + bytesToHex(aid) + ", P2=" + String.format("0x%02X", p2) + ")");
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object channel = param.getResult();
                    logCall("Session.openLogicalChannel() returned " + (channel != null ? "Channel@" + Integer.toHexString(channel.hashCode()) : "null"));
                }
            });

            // Hook openLogicalChannel with byte[] aid only
            XposedHelpers.findAndHookMethod(sessionClass, "openLogicalChannel", byte[].class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    byte[] aid = (byte[]) param.args[0];
                    logCall("Session.openLogicalChannel(aid=" + bytesToHex(aid) + ")");
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object channel = param.getResult();
                    logCall("Session.openLogicalChannel() returned " + (channel != null ? "Channel@" + Integer.toHexString(channel.hashCode()) : "null"));
                }
            });

            XposedBridge.log(TAG + ": Successfully hooked Session in " + packagePrefix);
            return true;
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook Session: " + t.getMessage());
            return false;
        }
    }

    private boolean hookChannelClass(LoadPackageParam lpparam, String packagePrefix) {
        try {
            Class<?> channelClass = XposedHelpers.findClass(packagePrefix + ".Channel", lpparam.classLoader);

            // Hook close()
            XposedHelpers.findAndHookMethod(channelClass, "close", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    logCall("Channel.close()");
                }
            });

            // Hook isBasicChannel()
            XposedHelpers.findAndHookMethod(channelClass, "isBasicChannel", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    boolean isBasic = (Boolean) param.getResult();
                    logCall("Channel.isBasicChannel() = " + isBasic);
                }
            });

            // Hook isClosed()
            XposedHelpers.findAndHookMethod(channelClass, "isClosed", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    boolean closed = (Boolean) param.getResult();
                    logCall("Channel.isClosed() = " + closed);
                }
            });

            // Hook getSelectResponse()
            XposedHelpers.findAndHookMethod(channelClass, "getSelectResponse", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    byte[] response = (byte[]) param.getResult();
                    logCall("Channel.getSelectResponse() = " + bytesToHex(response));
                }
            });

            // Hook getSession()
            XposedHelpers.findAndHookMethod(channelClass, "getSession", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object session = param.getResult();
                    logCall("Channel.getSession() = " + (session != null ? "Session@" + Integer.toHexString(session.hashCode()) : "null"));
                }
            });

            // Hook transmit() - the most important method for APDU communication
            XposedHelpers.findAndHookMethod(channelClass, "transmit", byte[].class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    byte[] command = (byte[]) param.args[0];
                    logCall("Channel.transmit(command=" + bytesToHex(command) + ")");
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    byte[] response = (byte[]) param.getResult();
                    logCall("Channel.transmit() returned " + bytesToHex(response));
                }
            });

            // Hook selectNext()
            XposedHelpers.findAndHookMethod(channelClass, "selectNext", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    boolean result = (Boolean) param.getResult();
                    logCall("Channel.selectNext() = " + result);
                }
            });

            XposedBridge.log(TAG + ": Successfully hooked Channel in " + packagePrefix);
            return true;
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook Channel: " + t.getMessage());
            return false;
        }
    }

    private void logCall(String message) {
        // Log to Xposed log
        XposedBridge.log(TAG + ": " + message);
        
        // Log to file
        logToFile(message);
        
        // Store in call log for the Activity to display
        try {
            CallLogger.getInstance().addLog(message);
        } catch (Throwable t) {
            // CallLogger might not be available in system processes
        }
    }

    private static synchronized void logToFile(String message) {
        if (!fileLoggingAvailable || logFile == null) {
            return;
        }
        
        try {
            FileWriter fw = new FileWriter(logFile, true);
            PrintWriter pw = new PrintWriter(fw);
            String timestamp = DATE_FORMAT.format(new Date());
            pw.println(timestamp + " | " + message);
            pw.flush();
            pw.close();
        } catch (Throwable t) {
            // Silently fail to avoid breaking the app
            fileLoggingAvailable = false;
        }
    }

    /**
     * Hook the SecureElement system service (com.android.se)
     * This is the backend service that handles all OMAPI calls system-wide
     */
    private boolean hookSecureElementService(LoadPackageParam lpparam) {
        boolean success = false;
        try {
            // Hook ISecureElementService.Stub implementation
            logToFile("Attempting to hook SecureElement Service implementation...");
            
            // Try to find the service implementation class
            Class<?> serviceClass = null;
            try {
                // Try different possible class names based on AOSP
                String[] possibleClasses = {
                    "com.android.se.SecureElementService",
                    "com.android.se.internal.SecureElementService",
                    "android.se.SecureElementService"
                };
                
                for (String className : possibleClasses) {
                    try {
                        serviceClass = XposedHelpers.findClass(className, lpparam.classLoader);
                        logToFile("Found SecureElement service class: " + className);
                        break;
                    } catch (Throwable t) {
                        // Try next
                    }
                }
            } catch (Throwable t) {
                logToFile("Could not find SecureElementService class: " + t.getMessage());
            }
            
            // Hook Terminal class for low-level APDU
            try {
                Class<?> terminalClass = XposedHelpers.findClass("com.android.se.Terminal", lpparam.classLoader);
                logToFile("Found Terminal class, hooking transmit...");
                
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
                
                logToFile("Successfully hooked Terminal.transmit()");
                success = true;
            } catch (Throwable t) {
                logToFile("Failed to hook Terminal: " + t.getMessage());
            }
            
            // Hook ChannelAccess for access control info
            try {
                Class<?> channelAccessClass = XposedHelpers.findClass("com.android.se.security.ChannelAccess", lpparam.classLoader);
                logToFile("Found ChannelAccess class");
                
                XposedHelpers.findAndHookMethod(channelAccessClass, "setCallingPid", int.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        int pid = (Integer) param.args[0];
                        logCall("[SYSTEM/Access] Channel access from PID: " + pid);
                    }
                });
                
                success = true;
            } catch (Throwable t) {
                logToFile("Failed to hook ChannelAccess: " + t.getMessage());
            }
            
            // Try to hook the AIDL service methods
            try {
                // Look for ISecureElementService$Stub
                Class<?> stubClass = XposedHelpers.findClass("android.se.omapi.ISecureElementService$Stub", lpparam.classLoader);
                logToFile("Found ISecureElementService$Stub");
                
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
                
                success = true;
            } catch (Throwable t) {
                logToFile("Failed to hook ISecureElementService: " + t.getMessage());
            }
            
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook SecureElement service: " + t.getMessage());
            logToFile("Failed to hook SecureElement service: " + t.getMessage());
            t.printStackTrace();
        }
        
        return success;
    }
    
    /**
     * Hook system_server for SE-related system services
     */
    private boolean hookSystemServer(LoadPackageParam lpparam) {
        boolean success = false;
        try {
            logToFile("Attempting to hook system_server SE components...");
            
            // Try to hook PackageManager for SE permission checks
            try {
                Class<?> pmClass = XposedHelpers.findClass("com.android.server.pm.PackageManagerService", 
                                                          lpparam.classLoader);
                logToFile("Found PackageManagerService in system_server");
                
                // This is just logging that we're in system_server
                // Actual SE permission checks happen in SecureElement service
                success = true;
            } catch (Throwable t) {
                logToFile("Could not hook PackageManagerService: " + t.getMessage());
            }
            
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook system_server: " + t.getMessage());
            logToFile("Failed to hook system_server: " + t.getMessage());
        }
        
        return success;
    }

    private static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
