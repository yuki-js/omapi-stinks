package app.aoki.yuki.omapistinks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LogAdapter adapter;
    private Handler handler;
    private Runnable refreshRunnable;
    private TextView statusText;
    private static final int REFRESH_INTERVAL_MS = 1000;
    private int logCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }

        statusText = findViewById(R.id.statusText);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new LogAdapter();
        recyclerView.setAdapter(adapter);

        // Update status
        updateStatus();

        handler = new Handler(Looper.getMainLooper());
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshLogs();
                updateStatus();
                handler.postDelayed(this, REFRESH_INTERVAL_MS);
            }
        };
    }
    
    private void updateStatus() {
        int currentLogCount = CallLogger.getInstance().getLogs().size();
        if (currentLogCount > 0) {
            statusText.setText("✓ Module active - " + currentLogCount + " logs captured");
        } else {
            statusText.setText("⚠ Waiting for hooks... Check LSPosed scope and logs");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Load existing logs (captured by manifest receiver)
        refreshLogs();
        handler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        handler.removeCallbacks(refreshRunnable);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        
        // Hide "View File Log" menu item since we're not using file logging
        MenuItem fileLogItem = menu.findItem(R.id.action_view_file_log);
        if (fileLogItem != null) {
            fileLogItem.setVisible(false);
        }
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_clear) {
            clearLogs();
            return true;
        } else if (id == R.id.action_refresh) {
            refreshLogs();
            updateStatus();
            Toast.makeText(this, "Refreshed", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_help) {
            showHelp();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void refreshLogs() {
        List<CallLogger.CallLogEntry> logs = CallLogger.getInstance().getLogs();
        adapter.setLogs(logs);
        
        // Scroll to the bottom to show latest logs
        if (logs.size() > 0) {
            recyclerView.smoothScrollToPosition(logs.size() - 1);
        }
        
        updateStatus();
    }

    private void clearLogs() {
        CallLogger.getInstance().clearLogs();
        logCount = 0;
        refreshLogs();
        Toast.makeText(this, "Logs cleared", Toast.LENGTH_SHORT).show();
    }
    
    private void showHelp() {
        String helpText = "OMAPI Stinks - Setup Instructions\n\n" +
                "1. Open LSPosed Manager\n" +
                "2. Enable 'OMAPI Stinks' module\n" +
                "3. Add apps to scope:\n" +
                "   • android (REQUIRED - system framework)\n" +
                "   • com.android.se (REQUIRED - SE service)\n" +
                "   • Your target apps (Google Pay, etc.)\n" +
                "4. Reboot device or restart apps\n" +
                "5. Use app with OMAPI (e.g., Google Pay)\n" +
                "6. Logs appear here in real-time!\n\n" +
                "Troubleshooting:\n" +
                "• No logs? Check LSPosed module log\n" +
                "• Ensure 'android' scope is enabled\n" +
                "• Reboot after enabling module\n" +
                "• Check LSPosed → Logs for errors\n\n" +
                "How it works:\n" +
                "• Hooks intercept OMAPI calls in apps\n" +
                "• Sends logs via broadcast to this app\n" +
                "• Shows all APDU commands and responses\n\n" +
                "Monitored Packages:\n" +
                "• android.se.omapi.*\n" +
                "• org.simalliance.openmobileapi.*\n" +
                "• com.android.se.Terminal (system)";
        
        new AlertDialog.Builder(this)
                .setTitle("Help & Setup")
                .setMessage(helpText)
                .setPositiveButton("OK", null)
                .setNeutralButton("LSPosed Logs", (dialog, which) -> {
                    Toast.makeText(this, 
                        "Check LSPosed Manager → Logs\nLook for 'OmapiStinks' entries", 
                        Toast.LENGTH_LONG).show();
                })
                .show();
    }
}
