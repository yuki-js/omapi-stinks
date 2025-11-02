package app.aoki.yuki.omapistinks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

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
                String message = intent.getStringExtra(Constants.EXTRA_MESSAGE);
                String timestamp = intent.getStringExtra(Constants.EXTRA_TIMESTAMP);
                
                Log.d(TAG, "Received log: " + message);
                
                if (message != null && timestamp != null) {
                    // Store in CallLogger (singleton, persists in memory)
                    CallLogger.getInstance().addLog(timestamp + " | " + message);
                    Log.d(TAG, "Log stored successfully. Total logs: " + CallLogger.getInstance().getLogs().size());
                } else {
                    Log.w(TAG, "Message or timestamp is null");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onReceive: " + e.getMessage(), e);
        }
    }
}
