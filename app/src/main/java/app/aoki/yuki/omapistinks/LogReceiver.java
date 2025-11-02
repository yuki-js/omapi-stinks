package app.aoki.yuki.omapistinks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Persistent BroadcastReceiver registered in AndroidManifest
 * Captures all OMAPI logs even when the app is not running
 */
public class LogReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Constants.BROADCAST_ACTION.equals(intent.getAction())) {
            String message = intent.getStringExtra(Constants.EXTRA_MESSAGE);
            String timestamp = intent.getStringExtra(Constants.EXTRA_TIMESTAMP);
            
            if (message != null && timestamp != null) {
                // Store in CallLogger (singleton, persists in memory)
                CallLogger.getInstance().addLog(timestamp + " | " + message);
            }
        }
    }
}
