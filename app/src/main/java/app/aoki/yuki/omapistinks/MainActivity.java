package app.aoki.yuki.omapistinks;

import android.Manifest;
import android.content.pm.PackageManager;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LogAdapter adapter;
    private Handler handler;
    private Runnable refreshRunnable;
    private TextView statusText;
    private static final int REFRESH_INTERVAL_MS = 1000;
    private static final int PERMISSION_REQUEST_CODE = 100;

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

        // Request permissions if needed
        checkPermissions();
        
        // Update status
        updateStatus();

        handler = new Handler(Looper.getMainLooper());
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshLogs();
                handler.postDelayed(this, REFRESH_INTERVAL_MS);
            }
        };
    }
    
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            }
        }
    }
    
    private void updateStatus() {
        // Check if file log exists
        File logFile = new File("/data/local/tmp/omapistinks/hooks.log");
        if (logFile.exists()) {
            statusText.setText("✓ Module active - Log file: " + logFile.getAbsolutePath());
        } else {
            statusText.setText("⚠ Waiting for hooks... Ensure module is enabled in LSPosed");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        } else if (id == R.id.action_view_file_log) {
            viewFileLog();
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
    }

    private void clearLogs() {
        CallLogger.getInstance().clearLogs();
        refreshLogs();
        Toast.makeText(this, "Logs cleared", Toast.LENGTH_SHORT).show();
    }
    
    private void viewFileLog() {
        try {
            File logFile = new File("/data/local/tmp/omapistinks/hooks.log");
            if (!logFile.exists()) {
                Toast.makeText(this, "Log file not found. Module may not be active.", Toast.LENGTH_LONG).show();
                return;
            }
            
            StringBuilder content = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(logFile));
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null && lineCount < 100) {
                content.append(line).append("\n");
                lineCount++;
            }
            reader.close();
            
            if (lineCount >= 100) {
                content.append("\n... (showing last 100 lines)");
            }
            
            new AlertDialog.Builder(this)
                    .setTitle("Hook Log File")
                    .setMessage(content.toString())
                    .setPositiveButton("OK", null)
                    .setNeutralButton("Path", (dialog, which) -> {
                        Toast.makeText(this, logFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    })
                    .show();
                    
        } catch (Exception e) {
            Toast.makeText(this, "Error reading log: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void showHelp() {
        String helpText = "OMAPI Stinks - Hook Status\n\n" +
                "Setup Instructions:\n" +
                "1. Enable module in LSPosed\n" +
                "2. Add apps to scope:\n" +
                "   • android (system framework)\n" +
                "   • com.android.se (SE service)\n" +
                "   • Apps using OMAPI\n" +
                "3. Reboot or restart apps\n\n" +
                "Log Locations:\n" +
                "• In-Memory: This app\n" +
                "• File: /data/local/tmp/omapistinks/hooks.log\n" +
                "• LSPosed: Check LSPosed logs\n\n" +
                "Troubleshooting:\n" +
                "• No logs? Check LSPosed scope\n" +
                "• File log helps verify hooks work\n" +
                "• System processes need 'android' scope";
        
        new AlertDialog.Builder(this)
                .setTitle("Help")
                .setMessage(helpText)
                .setPositiveButton("OK", null)
                .show();
    }
}
