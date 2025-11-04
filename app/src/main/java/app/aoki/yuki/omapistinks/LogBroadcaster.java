package app.aoki.yuki.omapistinks;

import android.content.Context;
import android.content.Intent;

import de.robv.android.xposed.XposedBridge;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Handles broadcasting structured log data to the UI app
 */
public class LogBroadcaster {
    private static final String TAG = "OmapiStinks";
    private final ContextProvider contextProvider;
    private final String packageName;
    private final SimpleDateFormat dateFormat;

    // Constructor now takes a ContextProvider instead of Context
    public LogBroadcaster(ContextProvider contextProvider, String packageName) {
        this.contextProvider = contextProvider;
        this.packageName = packageName;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
    }

    /**
     * Send a structured log entry via broadcast
     */
    public void logMessage(CallLogEntry entry) {
        try {
            XposedBridge.log(TAG + ": [" + packageName + "] " + entry.getFunctionName() + " (" + entry.getType() + ") [TID:" + entry.getThreadId() + ", PID:" + entry.getProcessId() + ", " + entry.getExecutionTimeMs() + "ms]");

            // Resolve context lazily each time we send
            Context ctx = null;
            try {
                ctx = contextProvider.getContext();
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": Error obtaining context from ContextProvider: " + t);
            }

            if (ctx != null) {
                Intent intent = new Intent(Constants.BROADCAST_ACTION);
                intent.setClassName(Constants.PACKAGE_NAME, Constants.PACKAGE_NAME + ".LogReceiver");
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
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

                ctx.sendBroadcast(intent);
            } else {
                XposedBridge.log(TAG + ": appContext is null; skipping broadcast for " + entry.getFunctionName());
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Error broadcasting log: " + t.getMessage());
        }
    }

    // rest unchanged...
    public String createTimestamp() {
        return dateFormat.format(new Date());
    }

    public String createShortTimestamp() {
        SimpleDateFormat shortFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
        return shortFormat.format(new Date());
    }

    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return null;
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}