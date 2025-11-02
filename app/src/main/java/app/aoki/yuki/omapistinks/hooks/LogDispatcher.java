package app.aoki.yuki.omapistinks.hooks;

import android.content.Intent;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.robv.android.xposed.XposedBridge;

/**
 * Handles dispatching logs to multiple destinations:
 * - File log (always works across processes)
 * - Broadcast to UI app (if available)
 * - In-memory logger (same process only)
 */
public class LogDispatcher {
    private static final String TAG = "OmapiStinks";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    
    public static final String BROADCAST_ACTION = "app.aoki.yuki.omapistinks.LOG_ENTRY";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_TIMESTAMP = "timestamp";
    
    private final File logFile;
    private final boolean fileLoggingAvailable;
    private final String processName;
    
    public LogDispatcher(String processName) {
        this.processName = processName;
        
        // Initialize file logging
        File logDir = new File("/data/local/tmp/omapistinks");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        this.logFile = new File(logDir, "hooks.log");
        this.fileLoggingAvailable = logFile.getParentFile() != null && logFile.getParentFile().canWrite();
        
        if (fileLoggingAvailable) {
            logToFile("=== LogDispatcher initialized for process: " + processName + " ===");
        }
    }
    
    /**
     * Dispatch a log message to all available destinations
     */
    public void dispatchLog(String message) {
        String timestamp = DATE_FORMAT.format(new Date());
        
        // Always log to file (works across all processes)
        logToFile(message);
        
        // Try to send broadcast to UI app
        try {
            sendBroadcast(message, timestamp);
        } catch (Throwable t) {
            // Broadcast might fail in some contexts
            XposedBridge.log(TAG + ": Failed to send broadcast: " + t.getMessage());
        }
        
        // Try in-memory logger (only works in same process)
        try {
            Class<?> callLoggerClass = Class.forName("app.aoki.yuki.omapistinks.CallLogger");
            Object instance = callLoggerClass.getMethod("getInstance").invoke(null);
            callLoggerClass.getMethod("addLog", String.class).invoke(instance, message);
        } catch (Throwable t) {
            // Expected to fail in different processes
        }
    }
    
    private synchronized void logToFile(String message) {
        if (!fileLoggingAvailable) {
            return;
        }
        
        try {
            FileWriter fw = new FileWriter(logFile, true);
            PrintWriter pw = new PrintWriter(fw);
            String timestamp = DATE_FORMAT.format(new Date());
            pw.println(timestamp + " | [" + processName + "] " + message);
            pw.flush();
            pw.close();
        } catch (Throwable t) {
            // Silently fail
        }
    }
    
    private void sendBroadcast(String message, String timestamp) {
        try {
            // Create broadcast intent
            Intent intent = new Intent(BROADCAST_ACTION);
            intent.putExtra(EXTRA_MESSAGE, message);
            intent.putExtra(EXTRA_TIMESTAMP, timestamp);
            intent.setPackage("app.aoki.yuki.omapistinks");
            
            // Send using reflection (context might not be directly available)
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object currentActivityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            if (currentActivityThread != null) {
                Object context = activityThreadClass.getMethod("getSystemContext").invoke(currentActivityThread);
                if (context != null) {
                    context.getClass().getMethod("sendBroadcast", Intent.class).invoke(context, intent);
                }
            }
        } catch (Throwable t) {
            // Expected to fail in some contexts
        }
    }
}
