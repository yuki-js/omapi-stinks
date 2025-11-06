package app.aoki.yuki.omapistinks.core;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Singleton logger for storing OMAPI call log entries
 */
public class CallLogger {
    private static CallLogger instance;
    private final List<CallLogEntry> logs;
    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat shortDateFormat;
    private final int MAX_LOGS = 1000;

    private CallLogger() {
        logs = new ArrayList<>();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        shortDateFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
    }

    public static synchronized CallLogger getInstance() {
        if (instance == null) {
            instance = new CallLogger();
        }
        return instance;
    }

    /**
     * Add a log entry with structured data
     */
    public synchronized void addLog(CallLogEntry entry) {
        logs.add(entry);
        
        // Keep only the last MAX_LOGS entries
        if (logs.size() > MAX_LOGS) {
            logs.remove(0);
        }
    }

    /**
     /**
      * Create and add a structured log entry
      */
     public synchronized void addStructuredLog(String packageName, String function, String type,
                                              String apduCommand, String apduResponse,
                                              String aid, String selectResponse, String details,
                                              long threadId, String threadName, int processId, long executionTimeMs,
                                              String error, String timestamp, String shortTimestamp,
                                              StackTraceElement[] stackTraceElements) {
         // Use Builder to create entry with all fields
         CallLogEntry.Builder builder = new CallLogEntry.Builder()
             .packageName(packageName)
             .functionName(function)
             .type(type)
             .apduCommand(apduCommand)
             .apduResponse(apduResponse)
             .aid(aid)
             .selectResponse(selectResponse)
             .details(details)
             .executionTimeMs(executionTimeMs);
 
         // Override timestamps if provided from Xposed (remote process)
         if (timestamp != null && !timestamp.isEmpty()) {
             builder.timestamp(timestamp);
         }
         if (shortTimestamp != null && !shortTimestamp.isEmpty()) {
             builder.shortTimestamp(shortTimestamp);
         }
 
         // Preserve remote thread/process info
         builder.threadId(threadId)
                .threadName(threadName)
                .processId(processId);
         
         if (error != null && !error.isEmpty()) {
             builder.error(error);
         }
 
         // Attach stack trace if available
         if (stackTraceElements != null && stackTraceElements.length > 0) {
             builder.stackTraceElements(stackTraceElements);
         }
         
         CallLogEntry entry = builder.build();
         addLog(entry);
     }
    public synchronized List<CallLogEntry> getLogs() {
        return new ArrayList<>(logs);
    }

    public synchronized void clearLogs() {
        logs.clear();
    }
}
