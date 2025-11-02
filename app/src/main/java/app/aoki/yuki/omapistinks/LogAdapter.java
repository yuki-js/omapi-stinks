package app.aoki.yuki.omapistinks;

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

    private List<CallLogger.CallLogEntry> logs;

    public LogAdapter() {
        this.logs = new ArrayList<>();
    }

    public void setLogs(List<CallLogger.CallLogEntry> logs) {
        this.logs = logs;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        CallLogger.CallLogEntry entry = logs.get(position);
        
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
        
        // Set details
        holder.detailsText.setText(entry.getDetails());
        
        // Handle APDU display for transmit calls
        if (entry.isTransmit() && entry.getApduInfo() != null) {
            CallLogger.ApduInfo apdu = entry.getApduInfo();
            
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
