package app.aoki.yuki.omapistinks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LogAdapter adapter;
    private Handler handler;
    private Runnable refreshRunnable;
    
    private static final int REFRESH_INTERVAL_MS = 1000;
    private int logCount = 0;
    
    // Filter state
    private String searchQuery = "";
    private String packageFilter = null;
    private String functionFilter = null;
    private long timeRangeStart = 0;
    private long timeRangeEnd = Long.MAX_VALUE;

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

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new LogAdapter();
        recyclerView.setAdapter(adapter);

        handler = new Handler(Looper.getMainLooper());
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshLogs();
                handler.postDelayed(this, REFRESH_INTERVAL_MS);
            }
        };
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
        
        if (id == R.id.action_filter) {
            showFilterDialog();
            return true;
        } else if (id == R.id.action_clear) {
            clearLogs();
            return true;
        } else if (id == R.id.action_export) {
            exportLogs();
            return true;
        } else if (id == R.id.action_help) {
            showHelp();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void refreshLogs() {
        List<CallLogEntry> logs = CallLogger.getInstance().getLogs();
        
        // Apply filters
        List<CallLogEntry> filteredLogs = filterLogs(logs);
        
        adapter.setLogs(filteredLogs);
        
        // Don't auto-scroll - let user control their scroll position
    }
    
    private List<CallLogEntry> filterLogs(List<CallLogEntry> logs) {
        List<CallLogEntry> filtered = new ArrayList<>();
        
        for (CallLogEntry entry : logs) {
            // Apply search query filter
            if (!searchQuery.isEmpty()) {
                String searchLower = searchQuery.toLowerCase(java.util.Locale.ROOT);
                boolean matches = entry.getMessage().toLowerCase(java.util.Locale.ROOT).contains(searchLower) ||
                                (entry.getPackageName() != null && entry.getPackageName().toLowerCase(java.util.Locale.ROOT).contains(searchLower)) ||
                                entry.getFunctionName().toLowerCase(java.util.Locale.ROOT).contains(searchLower);
                if (!matches) continue;
            }
            
            // Apply package filter
            if (packageFilter != null && !packageFilter.isEmpty()) {
                if (entry.getPackageName() == null || !entry.getPackageName().equals(packageFilter)) {
                    continue;
                }
            }
            
            // Apply function filter
            if (functionFilter != null && !functionFilter.isEmpty()) {
                if (!entry.getFunctionName().equals(functionFilter)) {
                    continue;
                }
            }
            
            // All filters passed
            filtered.add(entry);
        }
        
        return filtered;
    }
    
    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filter Logs");
        
        android.view.LayoutInflater inflater = getLayoutInflater();
        android.view.View dialogView = inflater.inflate(R.layout.dialog_filter, null);
        builder.setView(dialogView);
        
        // Get views from dialog
        TextInputEditText dialogSearchEditText = dialogView.findViewById(R.id.dialogSearchEditText);
        Chip dialogPackageChip = dialogView.findViewById(R.id.dialogFilterPackageChip);
        Chip dialogFunctionChip = dialogView.findViewById(R.id.dialogFilterFunctionChip);
        Chip dialogTimeChip = dialogView.findViewById(R.id.dialogFilterTimeChip);
        
        // Set current values
        dialogSearchEditText.setText(searchQuery);
        if (packageFilter != null) {
            dialogPackageChip.setChecked(true);
            dialogPackageChip.setText("Package: " + packageFilter.substring(packageFilter.lastIndexOf('.') + 1));
        }
        if (functionFilter != null) {
            dialogFunctionChip.setChecked(true);
            dialogFunctionChip.setText("Fn: " + functionFilter);
        }
        
        // Set up chip click listeners
        dialogPackageChip.setOnClickListener(v -> showPackageFilterDialogInner(dialogPackageChip));
        dialogFunctionChip.setOnClickListener(v -> showFunctionFilterDialogInner(dialogFunctionChip));
        dialogTimeChip.setOnClickListener(v -> showTimeRangeDialogInner(dialogTimeChip));
        
        builder.setPositiveButton("Apply", (dialog, which) -> {
            searchQuery = dialogSearchEditText.getText().toString();
            refreshLogs();
        });
        
        builder.setNeutralButton("Clear All", (dialog, which) -> {
            searchQuery = "";
            packageFilter = null;
            functionFilter = null;
            timeRangeStart = 0;
            timeRangeEnd = Long.MAX_VALUE;
            refreshLogs();
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void showPackageFilterDialogInner(Chip parentChip) {
        List<CallLogEntry> logs = CallLogger.getInstance().getLogs();
        Set<String> packages = new HashSet<>();
        for (CallLogEntry entry : logs) {
            if (entry.getPackageName() != null && !entry.getPackageName().isEmpty()) {
                packages.add(entry.getPackageName());
            }
        }
        
        String[] packageArray = packages.toArray(new String[0]);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filter by Package");
        
        int selectedIndex = -1;
        if (packageFilter != null) {
            for (int i = 0; i < packageArray.length; i++) {
                if (packageArray[i].equals(packageFilter)) {
                    selectedIndex = i;
                    break;
                }
            }
        }
        
        builder.setSingleChoiceItems(packageArray, selectedIndex, (dialog, which) -> {
            packageFilter = packageArray[which];
            parentChip.setChecked(true);
            parentChip.setText("Package: " + packageFilter.substring(packageFilter.lastIndexOf('.') + 1));
            dialog.dismiss();
        });
        
        builder.setNeutralButton("Clear", (dialog, which) -> {
            packageFilter = null;
            parentChip.setChecked(false);
            parentChip.setText("Select Package");
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void showFunctionFilterDialogInner(Chip parentChip) {
        List<CallLogEntry> logs = CallLogger.getInstance().getLogs();
        Set<String> functions = new HashSet<>();
        for (CallLogEntry entry : logs) {
            functions.add(entry.getFunctionName());
        }
        
        String[] functionArray = functions.toArray(new String[0]);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filter by Function");
        
        int selectedIndex = -1;
        if (functionFilter != null) {
            for (int i = 0; i < functionArray.length; i++) {
                if (functionArray[i].equals(functionFilter)) {
                    selectedIndex = i;
                    break;
                }
            }
        }
        
        builder.setSingleChoiceItems(functionArray, selectedIndex, (dialog, which) -> {
            functionFilter = functionArray[which];
            parentChip.setChecked(true);
            parentChip.setText("Fn: " + functionFilter);
            dialog.dismiss();
        });
        
        builder.setNeutralButton("Clear", (dialog, which) -> {
            functionFilter = null;
            parentChip.setChecked(false);
            parentChip.setText("Select Function");
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void showTimeRangeDialogInner(Chip parentChip) {
        // Simple time range filter - last N seconds
        String[] options = {"Last 10 seconds", "Last 30 seconds", "Last minute", "Last 5 minutes", "All time"};
        long[] timeRanges = {10000, 30000, 60000, 300000, 0};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filter by Time Range");
        
        builder.setItems(options, (dialog, which) -> {
            if (timeRanges[which] == 0) {
                timeRangeStart = 0;
                timeRangeEnd = Long.MAX_VALUE;
                parentChip.setChecked(false);
                parentChip.setText("Select Time Range");
            } else {
                timeRangeEnd = System.currentTimeMillis();
                timeRangeStart = timeRanges[which];
                parentChip.setChecked(true);
                parentChip.setText(options[which]);
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void clearLogs() {
        CallLogger.getInstance().clearLogs();
        logCount = 0;
        
        // Reset filters
        searchQuery = "";
        packageFilter = null;
        functionFilter = null;
        timeRangeStart = 0;
        timeRangeEnd = Long.MAX_VALUE;
        
        refreshLogs();
        Toast.makeText(this, "Logs cleared", Toast.LENGTH_SHORT).show();
    }
    
    private void exportLogs() {
        List<CallLogEntry> filteredLogs = filterLogs(CallLogger.getInstance().getLogs());
        
        if (filteredLogs.isEmpty()) {
            Toast.makeText(this, "No logs to export", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Export as CSV
        StringBuilder csvBuilder = new StringBuilder();
        
        // CSV Header
        csvBuilder.append("Timestamp,Package,Function,Type,Thread ID,Thread Name,Process ID,Execution Time (ms),APDU Command,APDU Response,AID,Select Response,Details\n");
        
        // CSV Data
        for (CallLogEntry entry : filteredLogs) {
            csvBuilder.append(escapeCsv(entry.getTimestamp())).append(",");
            csvBuilder.append(escapeCsv(entry.getPackageName())).append(",");
            csvBuilder.append(escapeCsv(entry.getFunctionName())).append(",");
            csvBuilder.append(escapeCsv(entry.getType())).append(",");
            csvBuilder.append(entry.getThreadId()).append(",");
            csvBuilder.append(escapeCsv(entry.getThreadName())).append(",");
            csvBuilder.append(entry.getProcessId()).append(",");
            csvBuilder.append(entry.getExecutionTimeMs()).append(",");
            
            if (entry.getApduInfo() != null) {
                csvBuilder.append(escapeCsv(entry.getApduInfo().getCommand())).append(",");
                csvBuilder.append(escapeCsv(entry.getApduInfo().getResponse())).append(",");
            } else {
                csvBuilder.append(",,");
            }
            
            csvBuilder.append(escapeCsv(entry.getAid())).append(",");
            csvBuilder.append(escapeCsv(entry.getSelectResponse())).append(",");
            csvBuilder.append(escapeCsv(entry.getDetails()));
            csvBuilder.append("\n");
        }
        
        // Share via Intent
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/csv");
        shareIntent.putExtra(Intent.EXTRA_TEXT, csvBuilder.toString());
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "OMAPI Stinks Export - " + filteredLogs.size() + " logs");
        
        Intent chooser = Intent.createChooser(shareIntent, "Export logs as CSV");
        startActivity(chooser);
    }
    
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        // Escape quotes and wrap in quotes if contains comma, quote, or newline
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
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
