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
                    
                    Log.d(TAG, "Received structured log from " + packageName + ": " + function);
                    
                    CallLogger.getInstance().addStructuredLog(packageName, function, type, 
                                                             apduCommand, apduResponse, 
                                                             aid, selectResponse, details);
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
