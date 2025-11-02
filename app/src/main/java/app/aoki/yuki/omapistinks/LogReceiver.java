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
                String packageName = intent.getStringExtra(Constants.EXTRA_PACKAGE);
                
                Log.d(TAG, "Received log from " + packageName + ": " + message);
                
                if (message != null) {
                    // Store in CallLogger (singleton, persists in memory)
                    CallLogger.getInstance().addLog(message, packageName);
                    Log.d(TAG, "Log stored successfully. Total logs: " + CallLogger.getInstance().getLogs().size());
                } else {
                    Log.w(TAG, "Message is null");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onReceive: " + e.getMessage(), e);
        }
    }
}
