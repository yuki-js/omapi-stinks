package app.aoki.yuki.omapistinks.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Arrays;

/**
 * Persistent BroadcastReceiver registered in AndroidManifest
 * Captures all OMAPI logs even when the app is not running
 */
public class LogReceiver extends BroadcastReceiver {
    
    private static final String TAG = "OmapiStinks.LogReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Log.d(TAG, "onReceive called with action: " + intent.getAction());
            
            if (Constants.BROADCAST_ACTION.equals(intent.getAction())) {
                String packageName = intent.getStringExtra(Constants.EXTRA_PACKAGE);
                String function = intent.getStringExtra(Constants.EXTRA_FUNCTION);
                String type = intent.getStringExtra(Constants.EXTRA_TYPE);
                
                // Check if this is structured data
                // All logs should now be structured
                if (type != null) {
                    String apduCommand = intent.getStringExtra(Constants.EXTRA_APDU_COMMAND);
                    String apduResponse = intent.getStringExtra(Constants.EXTRA_APDU_RESPONSE);
                    String aid = intent.getStringExtra(Constants.EXTRA_AID);
                    String selectResponse = intent.getStringExtra(Constants.EXTRA_SELECT_RESPONSE);
                    String details = intent.getStringExtra(Constants.EXTRA_DETAILS);
                    long threadId = intent.getLongExtra(Constants.EXTRA_THREAD_ID, 0);
                    String threadName = intent.getStringExtra(Constants.EXTRA_THREAD_NAME);
                    int processId = intent.getIntExtra(Constants.EXTRA_PROCESS_ID, 0);
                    long executionTimeMs = intent.getLongExtra(Constants.EXTRA_EXECUTION_TIME_MS, 0);
                    String error = intent.getStringExtra(Constants.EXTRA_ERROR);
                    String timestamp = intent.getStringExtra(Constants.EXTRA_TIMESTAMP);
                    String shortTimestamp = intent.getStringExtra(Constants.EXTRA_SHORT_TIMESTAMP);
                    
                    // Handle StackTraceElement extraction with API level compatibility
                    StackTraceElement[] stackTraceElements = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        stackTraceElements = intent.getSerializableExtra(Constants.EXTRA_STACKTRACE, StackTraceElement[].class);
                    } else {
                        @SuppressWarnings("deprecation")
                        java.io.Serializable serializable = intent.getSerializableExtra(Constants.EXTRA_STACKTRACE);
                        if (serializable instanceof StackTraceElement[]) {
                            stackTraceElements = (StackTraceElement[]) serializable;
                        }
                    }
                    Log.d(TAG, "stackTraceElements: " + Arrays.toString(stackTraceElements));
                    
                    Log.d(TAG, "Received structured log from " + packageName + ": " + function + " [TID:" + threadId + ", PID:" + processId + ", " + executionTimeMs + "ms]");
                    if (error != null && !error.isEmpty()) {
                        Log.e(TAG, "Log contains error: " + error);
                    }
                    
                    CallLogger.getInstance().addStructuredLog(packageName, function, type,
                                                             apduCommand, apduResponse,
                                                             aid, selectResponse, details,
                                                             threadId, threadName, processId, executionTimeMs, error,
                                                             timestamp, shortTimestamp, stackTraceElements);
                    Log.d(TAG, "Structured log stored. Total logs: " + CallLogger.getInstance().getLogs().size());
                } else {
                    Log.w(TAG, "Received log without type - ignoring");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onReceive: " + e.getMessage(), e);
        }
    }
}
