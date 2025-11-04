package app.aoki.yuki.omapistinks.core;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Represents a single OMAPI call log entry with structured data
 */
public class CallLogEntry {
    private static final int MAX_STACK_FRAMES = 20;
    
    private final String timestamp;
    private final String shortTimestamp;
    private final String packageName;
    private final String functionName;
    private final String type;
    private final String apduCommand;
    private final String apduResponse;
    private final String aid;
    private final String selectResponse;
    private final String details;
    private final long threadId;
    private final String threadName;
    private final int processId;
    private final long executionTimeMs;
    private final String error;
    private final String stackTrace;

    private CallLogEntry(Builder builder) {
        this.timestamp = builder.timestamp;
        this.shortTimestamp = builder.shortTimestamp;
        this.packageName = builder.packageName;
        this.functionName = builder.functionName;
        this.type = builder.type;
        this.apduCommand = builder.apduCommand;
        this.apduResponse = builder.apduResponse;
        this.aid = builder.aid;
        this.selectResponse = builder.selectResponse;
        this.details = builder.details;
        this.threadId = builder.threadId;
        this.threadName = builder.threadName;
        this.processId = builder.processId;
        this.executionTimeMs = builder.executionTimeMs;
        this.error = builder.error;
        this.stackTrace = builder.stackTrace;
    }

    /**
     * Create a log entry for Channel.transmit calls
     */
    public static CallLogEntry createTransmitEntry(String packageName, String functionName,
                                                   String apduCommand, String apduResponse,
                                                   long executionTimeMs) {
        return new Builder()
                .packageName(packageName)
                .functionName(functionName)
                .type(Constants.TYPE_TRANSMIT)
                .apduCommand(apduCommand)
                .apduResponse(apduResponse)
                .executionTimeMs(executionTimeMs)
                .build();
    }

    /**
     * Create a log entry for Session.openChannel calls
     */
    public static CallLogEntry createOpenChannelEntry(String packageName, String functionName,
                                                      String aid, String selectResponse,
                                                      long executionTimeMs) {
        return new Builder()
                .packageName(packageName)
                .functionName(functionName)
                .type(Constants.TYPE_OPEN_CHANNEL)
                .aid(aid)
                .selectResponse(selectResponse)
                .executionTimeMs(executionTimeMs)
                .build();
    }

    /**
     * Create a log entry for Application.attach hook
     */
    public static CallLogEntry createHookEntry(String packageName, String functionName, String details) {
        return new Builder()
                .packageName(packageName)
                .functionName(functionName)
                .type(Constants.TYPE_OTHER)
                .details(details)
                .executionTimeMs(0)
                .build();
    }

    /**
     * Create a log entry for errors
     */
    public static CallLogEntry createErrorEntry(String packageName, String functionName,
                                                String type, String error) {
        return new Builder()
                .packageName(packageName)
                .functionName(functionName)
                .type(type)
                .error(error)
                .executionTimeMs(0)
                .build();
    }

    /**
     * Builder for CallLogEntry to simplify construction
     */
    public static class Builder {
        private String timestamp;
        private String shortTimestamp;
        private String packageName;
        private String functionName;
        private String type;
        private String apduCommand;
        private String apduResponse;
        private String aid;
        private String selectResponse;
        private String details;
        private long threadId;
        private String threadName;
        private int processId;
        private long executionTimeMs;
        private String error;
        private String stackTrace;

        public Builder() {
            // Automatically capture thread and process info
            Thread currentThread = Thread.currentThread();
            this.threadId = currentThread.getId();
            this.threadName = currentThread.getName();
            this.processId = android.os.Process.myPid();
            
            // Automatically create timestamps
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
            SimpleDateFormat shortFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
            Date now = new Date();
            this.timestamp = dateFormat.format(now);
            this.shortTimestamp = shortFormat.format(now);
            
            // Capture call stack
            this.stackTrace = captureStackTrace();
        }
        
        /**
         * Captures the current call stack as a formatted string
         */
        private String captureStackTrace() {
            StringBuilder sb = new StringBuilder();
            StackTraceElement[] elements = Thread.currentThread().getStackTrace();
            
            // Skip the first few frames (getStackTrace, captureStackTrace, Builder constructor)
            // Start from frame 4 to show the actual caller context
            int maxFrames = 4 + MAX_STACK_FRAMES;
            for (int i = 4; i < elements.length && i < maxFrames; i++) {
                StackTraceElement element = elements[i];
                sb.append("  at ").append(element.toString()).append("\n");
            }
            
            return sb.toString();
        }

        public Builder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder functionName(String functionName) {
            this.functionName = functionName;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder apduCommand(String apduCommand) {
            this.apduCommand = apduCommand;
            return this;
        }

        public Builder apduResponse(String apduResponse) {
            this.apduResponse = apduResponse;
            return this;
        }

        public Builder aid(String aid) {
            this.aid = aid;
            return this;
        }

        public Builder selectResponse(String selectResponse) {
            this.selectResponse = selectResponse;
            return this;
        }

        public Builder details(String details) {
            this.details = details;
            return this;
        }

        public Builder executionTimeMs(long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public CallLogEntry build() {
            return new CallLogEntry(this);
        }
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getShortTimestamp() {
        return shortTimestamp;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getType() {
        return type;
    }

    public String getDetails() {
        return details;
    }

    public boolean isTransmit() {
        return Constants.TYPE_TRANSMIT.equals(type);
    }

    public ApduInfo getApduInfo() {
        if (apduCommand != null || apduResponse != null) {
            return new ApduInfo(apduCommand, apduResponse);
        }
        return null;
    }

    public String getAid() {
        return aid;
    }

    public String getSelectResponse() {
        return selectResponse;
    }

    public long getThreadId() {
        return threadId;
    }

    public String getThreadName() {
        return threadName;
    }

    public int getProcessId() {
        return processId;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public String getError() {
        return error;
    }

    public boolean hasError() {
        return error != null && !error.isEmpty();
    }

    public String getStackTrace() {
        return stackTrace;
    }

    // Legacy compatibility - getMessage() is not used for structured entries
    public String getMessage() {
        return null;
    }

    @Override
    public String toString() {
        if (hasError()) {
            return timestamp + " [" + packageName + "] " + functionName + " ERROR: " + error;
        }
        return timestamp + " [" + packageName + "] " + functionName + " (" + type + ")";
    }
}
