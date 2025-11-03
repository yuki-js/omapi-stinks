package app.aoki.yuki.omapistinks;

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
     * Create and add a structured log entry
     */
    public synchronized void addStructuredLog(String packageName, String function, String type, 
                                             String apduCommand, String apduResponse, 
                                             String aid, String selectResponse, String details,
                                             long threadId, String threadName, int processId, long executionTimeMs) {
        String timestamp = dateFormat.format(new Date());
        String shortTimestamp = shortDateFormat.format(new Date());
        CallLogEntry entry = new CallLogEntry(timestamp, shortTimestamp, packageName, function, 
                                             type, apduCommand, apduResponse, aid, selectResponse, details,
                                             threadId, threadName, processId, executionTimeMs);
        addLog(entry);
    }

    public synchronized List<CallLogEntry> getLogs() {
        return new ArrayList<>(logs);
    }

    public synchronized void clearLogs() {
        logs.clear();
    }
}
