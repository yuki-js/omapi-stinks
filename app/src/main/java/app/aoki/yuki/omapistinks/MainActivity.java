package app.aoki.yuki.omapistinks;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

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
    private static final int REFRESH_INTERVAL_MS = 1000;

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
            Toast.makeText(this, "Refreshed", Toast.LENGTH_SHORT).show();
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
}
