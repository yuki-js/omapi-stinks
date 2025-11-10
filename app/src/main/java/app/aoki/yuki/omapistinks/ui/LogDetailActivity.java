package app.aoki.yuki.omapistinks.ui;

import app.aoki.yuki.omapistinks.core.Constants;
import app.aoki.yuki.omapistinks.core.ApduInfo;
import app.aoki.yuki.omapistinks.R;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;

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

        // New optional extras
        String error = getIntent().getStringExtra("error");

        // Prefer strongly-typed stack trace elements passed as Serializable[]
        StackTraceElement[] stackTraceElements = null;
        if (Build.VERSION.SDK_INT >= 33) {
            stackTraceElements = getIntent().getSerializableExtra("stackTraceElements", StackTraceElement[].class);
            if (stackTraceElements == null) {
                stackTraceElements = getIntent().getSerializableExtra(Constants.EXTRA_STACKTRACE, StackTraceElement[].class);
            }
        } else {
            java.io.Serializable ser = getIntent().getSerializableExtra("stackTraceElements");
            if (ser instanceof StackTraceElement[]) {
                stackTraceElements = (StackTraceElement[]) ser;
            } else {
                java.io.Serializable ser2 = getIntent().getSerializableExtra(Constants.EXTRA_STACKTRACE);
                if (ser2 instanceof StackTraceElement[]) {
                    stackTraceElements = (StackTraceElement[]) ser2;
                }
            }
        }

        // Get views (from included header in ScrollView)
        TextView timestampView = findViewById(R.id.detailTimestamp);
        TextView packageView = findViewById(R.id.detailPackage);
        TextView functionView = findViewById(R.id.detailFunction);
        TextView typeView = findViewById(R.id.detailType);
        TextView aidView = findViewById(R.id.detailAid);
        TextView apduCommandView = findViewById(R.id.detailApduCommand);
        TextView apduResponseView = findViewById(R.id.detailApduResponse);
        TextView selectResponseView = findViewById(R.id.detailSelectResponse);
        TextView detailsView = findViewById(R.id.detailDetails);
        TextView errorView = findViewById(R.id.detailError);

        // Get cards
        MaterialCardView cardApduCommand = findViewById(R.id.cardApduCommand);
        MaterialCardView cardApduResponse = findViewById(R.id.cardApduResponse);
        MaterialCardView cardAid = findViewById(R.id.cardAid);
        MaterialCardView cardSelectResponse = findViewById(R.id.cardSelectResponse);
        MaterialCardView cardDetails = findViewById(R.id.cardDetails);
        MaterialCardView cardError = findViewById(R.id.cardError);
        MaterialCardView cardStackTrace = findViewById(R.id.cardStackTrace);

        // Get copy buttons
        MaterialButton btnCopyCommand = findViewById(R.id.btnCopyCommand);
        MaterialButton btnCopyResponse = findViewById(R.id.btnCopyResponse);
        MaterialButton btnCopyAid = findViewById(R.id.btnCopyAid);
        MaterialButton btnCopySelectResponse = findViewById(R.id.btnCopySelectResponse);
        MaterialButton btnCopyError = findViewById(R.id.btnCopyError);
        MaterialButton btnCopyStack = findViewById(R.id.btnCopyStack);

        // Stack frames container
        LinearLayout stackTraceContainer = findViewById(R.id.stackTraceContainer);

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
        cardError.setVisibility(View.GONE);
        cardStackTrace.setVisibility(View.GONE);

        // Show relevant cards based on type
        if (Constants.TYPE_TRANSMIT.equals(type)) {
            ApduInfo apduInfo = new ApduInfo(apduCommand, apduResponse);

            if (apduCommand != null && !apduCommand.isEmpty()) {
                cardApduCommand.setVisibility(View.VISIBLE);
                apduCommandView.setText(apduInfo.getFormattedCommand());
                btnCopyCommand.setOnClickListener(v -> copyToClipboard("APDU Command", apduCommand));
            }
            if (apduResponse != null && !apduResponse.isEmpty()) {
                cardApduResponse.setVisibility(View.VISIBLE);
                apduResponseView.setText(apduInfo.getFormattedResponse());
                btnCopyResponse.setOnClickListener(v -> copyToClipboard("APDU Response", apduResponse));
            }
            // Show AID if available for transmit calls (Channel mapped via SessionOpenChannelHook)
            if (aid != null && !aid.isEmpty()) {
                cardAid.setVisibility(View.VISIBLE);
                aidView.setText(aid);
                btnCopyAid.setOnClickListener(v -> copyToClipboard("AID", aid));
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

        // Error card (always independent of type)
        if (error != null && !error.isEmpty()) {
            cardError.setVisibility(View.VISIBLE);
            errorView.setText(error);
            btnCopyError.setOnClickListener(v -> copyToClipboard("Error", error));
        }

        // Stack trace (always independent of type)
        if (stackTraceElements != null && stackTraceElements.length > 0) {
            String fullStack = formatStackTraceElements(stackTraceElements);

            // Show card
            cardStackTrace.setVisibility(View.VISIBLE);

            // Populate dynamic stack frames as cards (no RecyclerView/ListView)
            LayoutInflater inflater = LayoutInflater.from(this);
            for (StackTraceElement element : stackTraceElements) {
                if (element == null) continue;

                View itemView = inflater.inflate(R.layout.item_stack_frame, stackTraceContainer, false);
                MaterialCardView frameCard = (MaterialCardView) itemView;
                TextView methodName = itemView.findViewById(R.id.methodName);
                TextView classNameView = itemView.findViewById(R.id.className);
                TextView fileLocation = itemView.findViewById(R.id.fileLocation);

                String clsName = element.getClassName() != null ? element.getClassName() : "UnknownClass";
                boolean isAppFrame = clsName.startsWith("app.aoki.yuki.omapistinks") ||
                        (packageName != null && !packageName.isEmpty() && clsName.startsWith(packageName));

                // Method name
                String mName = element.getMethodName() != null ? element.getMethodName() : "unknownMethod";
                methodName.setText(mName + "()");
                methodName.setTextColor(isAppFrame ? 0xFF6200EE : 0xFF424242);

                // Class name with package prefix dim and class name bold
                android.text.SpannableStringBuilder classText = new android.text.SpannableStringBuilder();
                String packagePrefix = "";
                String simpleClassName = clsName;
                int lastDot = clsName.lastIndexOf('.');
                if (lastDot > 0) {
                    packagePrefix = clsName.substring(0, lastDot + 1);
                    simpleClassName = clsName.substring(lastDot + 1);
                }
                classText.append(packagePrefix);
                int classStart = classText.length();
                classText.append(simpleClassName);
                classText.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                        classStart, classText.length(), android.text.SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
                classNameView.setText(classText);

                // File location
                String fileName = element.getFileName() != null ? element.getFileName() : "Unknown";
                int lineNum = element.getLineNumber();
                String location = fileName + ":" + (lineNum >= 0 ? lineNum : "?");
                if (element.isNativeMethod()) {
                    location += " (native)";
                }
                fileLocation.setText(location);

                // Card background color
                frameCard.setCardBackgroundColor(isAppFrame ? 0xFFF3E5F5 : 0xFFF5F5F5);

                // Long-press to copy frame
                itemView.setOnLongClickListener(v -> {
                    copyToClipboard("Stack Frame", "at " + element.toString());
                    return true;
                });

                stackTraceContainer.addView(itemView);
            }

            // Copy full stack
            btnCopyStack.setOnClickListener(v -> copyToClipboard("Call Stack", fullStack));
        } else {
            // No stack trace available
            cardStackTrace.setVisibility(View.GONE);
        }
        
    }

    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, label + " copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private String joinLines(ArrayList<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            sb.append(lines.get(i));
            if (i < lines.size() - 1) sb.append('\n');
        }
        return sb.toString();
    }

    private String collapseLines(String text, int maxLines) {
        String[] lines = text.split("\\r?\\n");
        if (lines.length <= maxLines) return text;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxLines; i++) {
            sb.append(lines[i]);
            if (i < maxLines - 1) sb.append('\n');
        }
        sb.append("\n… (").append(lines.length - maxLines).append(" more)");
        return sb.toString();
    }

    private String formatStackTraceElements(StackTraceElement[] elements) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < elements.length; i++) {
            StackTraceElement el = elements[i];
            if (el != null) {
                sb.append("at ").append(el.toString());
                if (i < elements.length - 1) sb.append('\n');
            }
        }
        return sb.toString();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
