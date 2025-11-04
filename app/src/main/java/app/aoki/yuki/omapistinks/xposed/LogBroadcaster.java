package app.aoki.yuki.omapistinks.xposed;

import app.aoki.yuki.omapistinks.core.ApduInfo;
import app.aoki.yuki.omapistinks.core.CallLogEntry;
import app.aoki.yuki.omapistinks.core.Constants;

import android.content.Context;
import android.content.Intent;

import de.robv.android.xposed.XposedBridge;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Handles broadcasting structured log data to the UI app
 * Uses ContextProvider for lazy context resolution with fallback support
 */
public class LogBroadcaster {
    private static final String TAG = "OmapiStinks";
    private final ContextProvider contextProvider;
    private final String packageName;
    private final SimpleDateFormat dateFormat;

    /**
     * Constructor using ContextProvider for lazy context resolution
     * @param contextProvider Provider that resolves context lazily with fallback strategies
     * @param packageName Package name for logging
     */
    public LogBroadcaster(ContextProvider contextProvider, String packageName) {
        this.contextProvider = contextProvider;
        this.packageName = packageName;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
    }

    /**
     * Send a structured log entry via broadcast
     * Context is resolved lazily each time to handle cases where context becomes available later
     */
    public void logMessage(CallLogEntry entry) {
        try {
            String logMsg = TAG + ": [" + packageName + "] " + entry.getFunctionName() + " (" + entry.getType() + ") [TID:" + entry.getThreadId() + ", PID:" + entry.getProcessId() + ", " + entry.getExecutionTimeMs() + "ms]";
            if (entry.hasError()) {
                logMsg += " ERROR: " + entry.getError();
            }
            XposedBridge.log(logMsg);
            
            // Resolve context lazily each time we send
            Context ctx = null;
            try {
                ctx = contextProvider.getContext();
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": Error obtaining context from ContextProvider: " + t);
            }
            
            if (ctx != null) {
                Intent intent = new Intent(Constants.BROADCAST_ACTION);
                intent.setClassName(Constants.PACKAGE_NAME, Constants.PACKAGE_NAME + ".core.LogReceiver");
                intent.putExtra(Constants.EXTRA_TIMESTAMP, entry.getTimestamp());
                intent.putExtra(Constants.EXTRA_PACKAGE, entry.getPackageName());
                intent.putExtra(Constants.EXTRA_FUNCTION, entry.getFunctionName());
                intent.putExtra(Constants.EXTRA_TYPE, entry.getType());
                
                if (entry.getApduInfo() != null) {
                    intent.putExtra(Constants.EXTRA_APDU_COMMAND, entry.getApduInfo().getCommand());
                    intent.putExtra(Constants.EXTRA_APDU_RESPONSE, entry.getApduInfo().getResponse());
                }
                
                intent.putExtra(Constants.EXTRA_AID, entry.getAid());
                intent.putExtra(Constants.EXTRA_SELECT_RESPONSE, entry.getSelectResponse());
                intent.putExtra(Constants.EXTRA_DETAILS, entry.getDetails());
                intent.putExtra(Constants.EXTRA_THREAD_ID, entry.getThreadId());
                intent.putExtra(Constants.EXTRA_THREAD_NAME, entry.getThreadName());
                intent.putExtra(Constants.EXTRA_PROCESS_ID, entry.getProcessId());
                intent.putExtra(Constants.EXTRA_EXECUTION_TIME_MS, entry.getExecutionTimeMs());
                intent.putExtra(Constants.EXTRA_ERROR, entry.getError());
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                
                ctx.sendBroadcast(intent);
            } else {
                XposedBridge.log(TAG + ": Context is null; skipping broadcast for " + entry.getFunctionName());
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Error broadcasting log: " + t.getMessage());
        }
    }

    /**
     * Create a timestamp for log entries
     */
    public String createTimestamp() {
        return dateFormat.format(new Date());
    }

    /**
     * Create a short timestamp for display
     */
    public String createShortTimestamp() {
        SimpleDateFormat shortFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
        return shortFormat.format(new Date());
    }

    /**
     * Helper to convert byte array to hex string
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return null;
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
