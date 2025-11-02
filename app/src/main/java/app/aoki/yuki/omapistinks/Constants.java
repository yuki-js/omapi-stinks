package app.aoki.yuki.omapistinks;

/**
 * Shared constants used across the app and Xposed module
 */
public class Constants {
    
    // Broadcast action for cross-process log communication
    public static final String BROADCAST_ACTION = "app.aoki.yuki.omapistinks.LOG_ENTRY";
    
    // Intent extras for log data
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_TIMESTAMP = "timestamp";
    public static final String EXTRA_PACKAGE = "packageName";
    
    // Package name for intent targeting
    public static final String PACKAGE_NAME = "app.aoki.yuki.omapistinks";
    
    private Constants() {
        // Prevent instantiation
    }
}
