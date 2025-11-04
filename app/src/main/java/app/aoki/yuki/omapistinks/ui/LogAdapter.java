package app.aoki.yuki.omapistinks.ui;

import app.aoki.yuki.omapistinks.core.ApduInfo;
import app.aoki.yuki.omapistinks.core.CallLogEntry;
import app.aoki.yuki.omapistinks.core.Constants;
import app.aoki.yuki.omapistinks.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {

    private List<CallLogEntry> logs;

    public LogAdapter() {
        this.logs = new ArrayList<>();
    }

    public void setLogs(List<CallLogEntry> logs) {
        this.logs = logs;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_log, parent, false);
        LogViewHolder holder = new LogViewHolder(view);
        
        // Set click listener on card
        view.findViewById(R.id.logCard).setOnClickListener(v -> {
            int position = holder.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                openDetailActivity(parent.getContext(), logs.get(position));
            }
        });
        
        return holder;
    }
    
    private void openDetailActivity(android.content.Context context, CallLogEntry entry) {
        android.content.Intent intent = new android.content.Intent(context, LogDetailActivity.class);
        intent.putExtra("timestamp", entry.getTimestamp());
        intent.putExtra("packageName", entry.getPackageName());
        intent.putExtra("function", entry.getFunctionName());
        intent.putExtra("type", entry.getType());
        
        if (entry.getApduInfo() != null) {
            intent.putExtra("apduCommand", entry.getApduInfo().getCommand());
            intent.putExtra("apduResponse", entry.getApduInfo().getResponse());
        }
        
        intent.putExtra("aid", entry.getAid());
        intent.putExtra("selectResponse", entry.getSelectResponse());
        intent.putExtra("details", entry.getDetails());
        intent.putExtra("threadId", entry.getThreadId());
        intent.putExtra("threadName", entry.getThreadName());
        intent.putExtra("processId", entry.getProcessId());
        intent.putExtra("executionTimeMs", entry.getExecutionTimeMs());
        intent.putExtra("stackTrace", entry.getStackTrace());
        
        context.startActivity(intent);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        CallLogEntry entry = logs.get(position);
        
        // Set timestamp (short format)
        holder.timestampText.setText(entry.getShortTimestamp());
        
        // Set package name
        String packageName = entry.getPackageName();
        if (packageName != null && !packageName.isEmpty()) {
            holder.packageText.setText(packageName);
            holder.packageText.setVisibility(View.VISIBLE);
        } else {
            holder.packageText.setVisibility(View.GONE);
        }
        
        // Set function name
        holder.functionText.setText(entry.getFunctionName());
        
        // For transmit: hide details text, only show APDU
        // For open channel: show AID and select response
        // For others: show details
        if (entry.isTransmit()) {
            holder.detailsText.setVisibility(View.GONE);
        } else if (Constants.TYPE_OPEN_CHANNEL.equals(entry.getType())) {
            // Show AID for open channel
            String aid = entry.getAid();
            if (aid != null && !aid.isEmpty()) {
                holder.detailsText.setText("AID: " + aid);
                holder.detailsText.setVisibility(View.VISIBLE);
            } else {
                holder.detailsText.setVisibility(View.GONE);
            }
        } else {
            // Legacy text-based or other types
            String details = entry.getDetails();
            if (details != null && !details.isEmpty()) {
                holder.detailsText.setText(details);
                holder.detailsText.setVisibility(View.VISIBLE);
            } else {
                holder.detailsText.setVisibility(View.GONE);
            }
        }
        
        // Handle APDU display for transmit calls
        if (entry.isTransmit() && entry.getApduInfo() != null) {
            ApduInfo apdu = entry.getApduInfo();
            
            // Show command if available
            if (apdu.getCommand() != null) {
                holder.apduCommandLayout.setVisibility(View.VISIBLE);
                holder.apduCommandText.setText(apdu.getFormattedCommand());
            } else {
                holder.apduCommandLayout.setVisibility(View.GONE);
            }
            
            // Show response if available
            if (apdu.getResponse() != null) {
                holder.apduResponseLayout.setVisibility(View.VISIBLE);
                holder.apduResponseText.setText(apdu.getFormattedResponse());
            } else {
                holder.apduResponseLayout.setVisibility(View.GONE);
            }
        } else if (Constants.TYPE_OPEN_CHANNEL.equals(entry.getType())) {
            // For open channel, show select response as "response"
            holder.apduCommandLayout.setVisibility(View.GONE);
            
            String selectResponse = entry.getSelectResponse();
            if (selectResponse != null && !selectResponse.isEmpty()) {
                holder.apduResponseLayout.setVisibility(View.VISIBLE);
                holder.apduResponseText.setText(selectResponse);
            } else {
                holder.apduResponseLayout.setVisibility(View.GONE);
            }
        } else {
            holder.apduCommandLayout.setVisibility(View.GONE);
            holder.apduResponseLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView timestampText;
        TextView packageText;
        TextView functionText;
        TextView detailsText;
        LinearLayout apduCommandLayout;
        TextView apduCommandText;
        LinearLayout apduResponseLayout;
        TextView apduResponseText;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            timestampText = itemView.findViewById(R.id.timestampText);
            packageText = itemView.findViewById(R.id.packageText);
            functionText = itemView.findViewById(R.id.functionText);
            detailsText = itemView.findViewById(R.id.detailsText);
            apduCommandLayout = itemView.findViewById(R.id.apduCommandLayout);
            apduCommandText = itemView.findViewById(R.id.apduCommandText);
            apduResponseLayout = itemView.findViewById(R.id.apduResponseLayout);
            apduResponseText = itemView.findViewById(R.id.apduResponseText);
        }
    }
}
