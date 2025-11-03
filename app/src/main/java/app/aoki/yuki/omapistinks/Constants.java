package app.aoki.yuki.omapistinks;

/**
 * Shared constants used across the app and Xposed module
 */
public class Constants {
    
    // Broadcast action for cross-process log communication
    public static final String BROADCAST_ACTION = "app.aoki.yuki.omapistinks.LOG_ENTRY";
    
    // Intent extras for structured log data
    public static final String EXTRA_MESSAGE = "message"; // Legacy
    public static final String EXTRA_TIMESTAMP = "timestamp";
    public static final String EXTRA_PACKAGE = "packageName";
    public static final String EXTRA_FUNCTION = "functionName";
    public static final String EXTRA_TYPE = "type"; // "transmit", "open", "close", "other"
    public static final String EXTRA_APDU_COMMAND = "apduCommand";
    public static final String EXTRA_APDU_RESPONSE = "apduResponse";
    public static final String EXTRA_AID = "aid";
    public static final String EXTRA_SELECT_RESPONSE = "selectResponse";
    public static final String EXTRA_DETAILS = "details";
    public static final String EXTRA_THREAD_ID = "threadId";
    public static final String EXTRA_THREAD_NAME = "threadName";
    public static final String EXTRA_PROCESS_ID = "processId";
    public static final String EXTRA_EXECUTION_TIME_MS = "executionTimeMs";
    
    // Package name for intent targeting
    public static final String PACKAGE_NAME = "app.aoki.yuki.omapistinks";
    
    // Log entry types
    public static final String TYPE_TRANSMIT = "transmit";
    public static final String TYPE_OPEN_CHANNEL = "open_channel";
    public static final String TYPE_CLOSE = "close";
    public static final String TYPE_OTHER = "other";
    
    private Constants() {
        // Prevent instantiation
    }
}
