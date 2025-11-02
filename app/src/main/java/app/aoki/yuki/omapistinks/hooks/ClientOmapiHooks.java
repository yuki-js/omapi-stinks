package app.aoki.yuki.omapistinks.hooks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * Hooks for client-side OMAPI classes (android.se.omapi.* or org.simalliance.openmobileapi.*)
 * These classes are used by apps to communicate with the Secure Element
 */
public class ClientOmapiHooks extends OmapiHooks {

    public ClientOmapiHooks(LogDispatcher logDispatcher) {
        super(logDispatcher);
    }

    @Override
    public boolean hookPackage(LoadPackageParam lpparam, String packagePrefix) {
        boolean success = false;
        try {
            // Try to find at least one OMAPI class to verify the package exists
            try {
                XposedHelpers.findClass(packagePrefix + ".SEService", lpparam.classLoader);
            } catch (Throwable t) {
                // Package doesn't exist in this app, skip silently
                return false;
            }
            
            logDispatcher.dispatchLog("Found OMAPI package: " + packagePrefix + " in " + lpparam.packageName);
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
                logDispatcher.dispatchLog("Successfully hooked " + packagePrefix);
            }
            
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook " + packagePrefix + ": " + t.getMessage());
            logDispatcher.dispatchLog("Failed to hook " + packagePrefix + ": " + t.getMessage());
        }
        return success;
    }

    private boolean hookSEServiceClass(LoadPackageParam lpparam, String packagePrefix) {
        try {
            Class<?> seServiceClass = XposedHelpers.findClass(packagePrefix + ".SEService", lpparam.classLoader);
            logDispatcher.dispatchLog("Hooking SEService class: " + seServiceClass.getName());
            
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
            logDispatcher.dispatchLog("Successfully hooked SEService in " + packagePrefix);
            return true;
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook SEService: " + t.getMessage());
            logDispatcher.dispatchLog("Failed to hook SEService: " + t.getMessage());
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
}
