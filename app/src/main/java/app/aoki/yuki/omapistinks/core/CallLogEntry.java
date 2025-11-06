package app.aoki.yuki.omapistinks.core;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
// Log module
import android.util.Log;

/**
 * Represents a single OMAPI call log entry with structured data
 */
public class CallLogEntry {
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
    private final StackTraceElement[] stackTraceElements;
    private final long threadId;
    private final String threadName;
    private final int processId;
    private final long executionTimeMs;
    private final String error;

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
        this.stackTraceElements = builder.stackTraceElements;
    }

    /**
     * Capture the current thread's stack trace elements, skipping internal frames
     * (Thread.getStackTrace, Builder ctor, CallLogEntry) and limiting depth.
     * Returns null if stack cannot be obtained or no relevant frames found.
     */
    private static StackTraceElement[] captureStackTraceElements(Thread thread) {
        StackTraceElement[] frames;
        try {
            frames = thread.getStackTrace();
        } catch (Throwable ignored) {
            return null;
        }
        if (frames.length == 0) return null;

        int start = 0;
        for (int i = 0; i < frames.length; i++) {
            String cls = frames[i].getClassName();
            if (!cls.contains("XC_MethodHook")) {
                continue;
            }
            start = i;
            break;
        }

        int available = frames.length - start;

        StackTraceElement[] out = new StackTraceElement[available];
        System.arraycopy(frames, start, out, 0, available);

        return out;
    }

    private static StackTraceElement[] captureStackTraceElements() {
        return captureStackTraceElements(Thread.currentThread());
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
                .stackTraceElements(captureStackTraceElements())
                .build();
    }
    
    /**
     * Create a log entry for Channel.transmit calls with associated AID (if available)
     */
    public static CallLogEntry createTransmitEntry(String packageName, String functionName,
                                                   String apduCommand, String apduResponse,
                                                   String aid, long executionTimeMs) {
        return new Builder()
                .packageName(packageName)
                .functionName(functionName)
                .type(Constants.TYPE_TRANSMIT)
                .apduCommand(apduCommand)
                .apduResponse(apduResponse)
                .aid(aid)
                .executionTimeMs(executionTimeMs)
                .stackTraceElements(captureStackTraceElements())
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
                .stackTraceElements(captureStackTraceElements())
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
                .stackTraceElements(captureStackTraceElements())
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
                .stackTraceElements(captureStackTraceElements())
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
        private StackTraceElement[] stackTraceElements;

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

        // Overrides to preserve remote process metadata from Xposed
        public Builder threadId(long threadId) {
            this.threadId = threadId;
            return this;
        }

        public Builder threadName(String threadName) {
            this.threadName = threadName;
            return this;
        }

        public Builder processId(int processId) {
            this.processId = processId;
            return this;
        }

        public Builder timestamp(String timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder shortTimestamp(String shortTimestamp) {
            this.shortTimestamp = shortTimestamp;
            return this;
        }
        
        // Optional: allow overriding captured stack trace elements (rarely needed)
        public Builder stackTraceElements(StackTraceElement[] stackTraceElements) {
            this.stackTraceElements = stackTraceElements;
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

    /**
     * Returns the captured call stack elements (may be null).
     */
    public StackTraceElement[] getStackTraceElements() {
        return stackTraceElements;
    }

    public boolean hasStackTrace() {
        return stackTraceElements != null && stackTraceElements.length > 0;
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
