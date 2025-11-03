package app.aoki.yuki.omapistinks;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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

        // Display information
        TextView timestampView = findViewById(R.id.detailTimestamp);
        TextView packageView = findViewById(R.id.detailPackage);
        TextView functionView = findViewById(R.id.detailFunction);
        TextView typeView = findViewById(R.id.detailType);
        TextView aidView = findViewById(R.id.detailAid);
        TextView apduCommandView = findViewById(R.id.detailApduCommand);
        TextView apduResponseView = findViewById(R.id.detailApduResponse);
        TextView selectResponseView = findViewById(R.id.detailSelectResponse);
        TextView detailsView = findViewById(R.id.detailDetails);

        timestampView.setText("Time: " + (timestamp != null ? timestamp : "N/A"));
        packageView.setText("Package: " + (packageName != null ? packageName : "N/A"));
        functionView.setText("Function: " + (function != null ? function : "N/A"));
        typeView.setText("Type: " + (type != null ? type : "N/A"));

        if (Constants.TYPE_TRANSMIT.equals(type)) {
            aidView.setText("");
            apduCommandView.setText("Command: " + (apduCommand != null ? apduCommand : "N/A"));
            apduResponseView.setText("Response: " + (apduResponse != null ? apduResponse : "N/A"));
            selectResponseView.setText("");
            detailsView.setText("");
        } else if (Constants.TYPE_OPEN_CHANNEL.equals(type)) {
            aidView.setText("AID: " + (aid != null ? aid : "N/A"));
            apduCommandView.setText("");
            apduResponseView.setText("");
            selectResponseView.setText("Select Response: " + (selectResponse != null ? selectResponse : "N/A"));
            detailsView.setText("");
        } else {
            aidView.setText("");
            apduCommandView.setText("");
            apduResponseView.setText("");
            selectResponseView.setText("");
            detailsView.setText(details != null ? details : "N/A");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
