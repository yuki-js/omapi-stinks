package app.aoki.yuki.omapistinks;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CallLogger {
    private static CallLogger instance;
    private final List<CallLogEntry> logs;
    private final SimpleDateFormat dateFormat;
    private final int MAX_LOGS = 1000;

    private CallLogger() {
        logs = new ArrayList<>();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
    }

    public static synchronized CallLogger getInstance() {
        if (instance == null) {
            instance = new CallLogger();
        }
        return instance;
    }

    public synchronized void addLog(String message) {
        String timestamp = dateFormat.format(new Date());
        CallLogEntry entry = new CallLogEntry(timestamp, message);
        logs.add(entry);
        
        // Keep only the last MAX_LOGS entries
        if (logs.size() > MAX_LOGS) {
            logs.remove(0);
        }
    }

    public synchronized List<CallLogEntry> getLogs() {
        return new ArrayList<>(logs);
    }

    public synchronized void clearLogs() {
        logs.clear();
    }

    public static class CallLogEntry {
        private final String timestamp;
        private final String message;

        public CallLogEntry(String timestamp, String message) {
            this.timestamp = timestamp;
            this.message = message;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return timestamp + " - " + message;
        }
    }
}
