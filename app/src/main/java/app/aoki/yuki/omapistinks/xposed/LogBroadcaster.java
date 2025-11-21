package app.aoki.yuki.omapistinks.xposed;

import app.aoki.yuki.omapistinks.core.CallLogEntry;
import app.aoki.yuki.omapistinks.core.Constants;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import de.robv.android.xposed.XposedBridge;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles broadcasting structured log data to the UI app
 * Uses ContextProvider for lazy context resolution with fallback support
 * 
 * LOW_IMPACT_MODE: When enabled, minimizes logging verbosity to reduce detection
 * and impact on hooked applications. Only essential data is logged.
 * 
 * The mode is configurable via SharedPreferences and can be changed from the UI.
 * Default is false (full logging enabled).
 */
public class LogBroadcaster {
    private static final String TAG = "OmapiStinks";
    
    private final ContextProvider contextProvider;
    private final String packageName;
    private final SimpleDateFormat dateFormat;
    
    /**
     * Single-threaded executor for non-blocking async broadcasts
     * Ensures broadcasts don't block the hooked application thread
     */
    private static final ExecutorService broadcastExecutor = Executors.newSingleThreadExecutor();

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
     * Check if LOW_IMPACT_MODE is enabled from SharedPreferences
     * Falls back to default if preferences cannot be read
     */
    private boolean isLowImpactMode(Context ctx) {
        if (ctx == null) {
            return Constants.DEFAULT_LOW_IMPACT_MODE;
        }
        try {
            SharedPreferences prefs = ctx.createPackageContext(
                Constants.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY
            ).getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
            return prefs.getBoolean(Constants.PREF_LOW_IMPACT_MODE, Constants.DEFAULT_LOW_IMPACT_MODE);
        } catch (Throwable t) {
            // If we can't read preferences, use default
            return Constants.DEFAULT_LOW_IMPACT_MODE;
        }
    }

    /**
     * Send a structured log entry via broadcast
     * Context is resolved lazily each time to handle cases where context becomes available later
     * 
     * All exceptions are fully absorbed to ensure the hooked application never receives errors.
     * Broadcasting is performed asynchronously to avoid blocking the hooked thread.
     */
    public void logMessage(CallLogEntry entry) {
        // Fully catch and absorb all Throwables so hooked app never receives exceptions
        try {
            // Resolve context lazily each time we send
            Context ctx = null;
            try {
                ctx = contextProvider.getContext();
            } catch (Throwable t) {
                // Suppress error - context not available yet
            }
            
            // Check LOW_IMPACT_MODE setting
            final boolean lowImpactMode = isLowImpactMode(ctx);
            
            // In LOW_IMPACT_MODE, suppress verbose XposedBridge.log output
            if (!lowImpactMode) {
                String logMsg = TAG + ": [" + packageName + "] " + entry.getFunctionName() + " (" + entry.getType() + ") [TID:" + entry.getThreadId() + ", PID:" + entry.getProcessId() + ", " + entry.getExecutionTimeMs() + "ms]";
                if (entry.hasError()) {
                    logMsg += " ERROR: " + entry.getError();
                }
                XposedBridge.log(logMsg);
            }
            
            if (ctx != null) {
                final Context finalCtx = ctx;
                
                // Perform broadcast asynchronously to avoid blocking hooked thread
                broadcastExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Intent intent = new Intent(Constants.BROADCAST_ACTION);
                            intent.setClassName(Constants.PACKAGE_NAME, Constants.PACKAGE_NAME + ".core.LogReceiver");
                            
                            // Always include minimal essential data
                            intent.putExtra(Constants.EXTRA_TIMESTAMP, entry.getTimestamp());
                            intent.putExtra(Constants.EXTRA_SHORT_TIMESTAMP, entry.getShortTimestamp());
                            intent.putExtra(Constants.EXTRA_PACKAGE, entry.getPackageName());
                            intent.putExtra(Constants.EXTRA_FUNCTION, entry.getFunctionName());
                            intent.putExtra(Constants.EXTRA_TYPE, entry.getType());
                            intent.putExtra(Constants.EXTRA_EXECUTION_TIME_MS, entry.getExecutionTimeMs());
                            
                            // In LOW_IMPACT_MODE, only send minimal data
                            // Do NOT include APDU command/response, stack traces, details/AID/selectResponse
                            if (!lowImpactMode) {
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
                                intent.putExtra(Constants.EXTRA_ERROR, entry.getError());

                                if (entry.hasStackTrace()) {
                                    intent.putExtra(Constants.EXTRA_STACKTRACE, entry.getStackTraceElements());
                                }
                            } else {
                                // In LOW_IMPACT_MODE, only include error if present (essential for debugging)
                                if (entry.hasError()) {
                                    intent.putExtra(Constants.EXTRA_ERROR, entry.getError());
                                }
                            }

                            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                            
                            finalCtx.sendBroadcast(intent);
                        } catch (Throwable t) {
                            // Absorb all exceptions in async broadcast thread
                            // In LOW_IMPACT_MODE, suppress error logging
                            if (!lowImpactMode) {
                                XposedBridge.log(TAG + ": Error in async broadcast: " + t.getMessage());
                            }
                        }
                    }
                });
            } else {
                // Context is null - skip broadcast
                // In LOW_IMPACT_MODE, suppress logging
                if (!lowImpactMode) {
                    XposedBridge.log(TAG + ": Context is null; skipping broadcast for " + entry.getFunctionName());
                }
            }
        } catch (Throwable t) {
            // Final catch-all: absorb ANY exception to protect hooked app
            // We can't check lowImpactMode here since we may not have context
            // Just silently ignore to be safe
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
