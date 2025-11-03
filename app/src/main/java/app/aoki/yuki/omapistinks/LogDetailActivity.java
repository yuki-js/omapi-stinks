package app.aoki.yuki.omapistinks;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class LogDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Log Detail");
        }

        // Get data from intent
        String timestamp = getIntent().getStringExtra("timestamp");
        String packageName = getIntent().getStringExtra("packageName");
        String function = getIntent().getStringExtra("function");
        String type = getIntent().getStringExtra("type");
        String apduCommand = getIntent().getStringExtra("apduCommand");
        String apduResponse = getIntent().getStringExtra("apduResponse");
        String aid = getIntent().getStringExtra("aid");
        String selectResponse = getIntent().getStringExtra("selectResponse");
        String details = getIntent().getStringExtra("details");
        long threadId = getIntent().getLongExtra("threadId", 0);
        String threadName = getIntent().getStringExtra("threadName");
        int processId = getIntent().getIntExtra("processId", 0);
        long executionTimeMs = getIntent().getLongExtra("executionTimeMs", 0);

        // Get views
        TextView timestampView = findViewById(R.id.detailTimestamp);
        TextView packageView = findViewById(R.id.detailPackage);
        TextView functionView = findViewById(R.id.detailFunction);
        TextView typeView = findViewById(R.id.detailType);
        TextView aidView = findViewById(R.id.detailAid);
        TextView apduCommandView = findViewById(R.id.detailApduCommand);
        TextView apduResponseView = findViewById(R.id.detailApduResponse);
        TextView selectResponseView = findViewById(R.id.detailSelectResponse);
        TextView detailsView = findViewById(R.id.detailDetails);

        // Get cards
        MaterialCardView cardApduCommand = findViewById(R.id.cardApduCommand);
        MaterialCardView cardApduResponse = findViewById(R.id.cardApduResponse);
        MaterialCardView cardAid = findViewById(R.id.cardAid);
        MaterialCardView cardSelectResponse = findViewById(R.id.cardSelectResponse);
        MaterialCardView cardDetails = findViewById(R.id.cardDetails);

        // Get copy buttons
        MaterialButton btnCopyCommand = findViewById(R.id.btnCopyCommand);
        MaterialButton btnCopyResponse = findViewById(R.id.btnCopyResponse);
        MaterialButton btnCopyAid = findViewById(R.id.btnCopyAid);
        MaterialButton btnCopySelectResponse = findViewById(R.id.btnCopySelectResponse);

        // Set basic info
        timestampView.setText("⏱ " + (timestamp != null ? timestamp : "N/A"));
        packageView.setText("📦 " + (packageName != null ? packageName : "N/A"));
        functionView.setText("⚙ " + (function != null ? function : "N/A"));
        
        // Build type info with execution details
        StringBuilder typeInfo = new StringBuilder();
        typeInfo.append("🏷 ").append(type != null ? type : "N/A");
        if (executionTimeMs > 0) {
            typeInfo.append("\n⏲ Execution time: ").append(executionTimeMs).append(" ms");
        }
        if (threadId > 0) {
            typeInfo.append("\n🧵 Thread: ").append(threadName != null ? threadName : "unknown");
            typeInfo.append(" (ID: ").append(threadId).append(")");
        }
        if (processId > 0) {
            typeInfo.append("\n🔢 Process ID: ").append(processId);
        }
        typeView.setText(typeInfo.toString());

        // Hide all cards initially
        cardApduCommand.setVisibility(View.GONE);
        cardApduResponse.setVisibility(View.GONE);
        cardAid.setVisibility(View.GONE);
        cardSelectResponse.setVisibility(View.GONE);
        cardDetails.setVisibility(View.GONE);

        // Show relevant cards based on type
        if (Constants.TYPE_TRANSMIT.equals(type)) {
            if (apduCommand != null && !apduCommand.isEmpty()) {
                cardApduCommand.setVisibility(View.VISIBLE);
                apduCommandView.setText(apduCommand);
                btnCopyCommand.setOnClickListener(v -> copyToClipboard("APDU Command", apduCommand));
            }
            if (apduResponse != null && !apduResponse.isEmpty()) {
                cardApduResponse.setVisibility(View.VISIBLE);
                apduResponseView.setText(apduResponse);
                btnCopyResponse.setOnClickListener(v -> copyToClipboard("APDU Response", apduResponse));
            }
        } else if (Constants.TYPE_OPEN_CHANNEL.equals(type)) {
            if (aid != null && !aid.isEmpty()) {
                cardAid.setVisibility(View.VISIBLE);
                aidView.setText(aid);
                btnCopyAid.setOnClickListener(v -> copyToClipboard("AID", aid));
            }
            if (selectResponse != null && !selectResponse.isEmpty()) {
                cardSelectResponse.setVisibility(View.VISIBLE);
                selectResponseView.setText(selectResponse);
                btnCopySelectResponse.setOnClickListener(v -> copyToClipboard("Select Response", selectResponse));
            }
        } else {
            if (details != null && !details.isEmpty()) {
                cardDetails.setVisibility(View.VISIBLE);
                detailsView.setText(details);
            }
        }
    }

    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, label + " copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
