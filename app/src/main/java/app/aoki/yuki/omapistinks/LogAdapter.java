package app.aoki.yuki.omapistinks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        holder.timestampText.setText(entry.getTimestamp());
        holder.messageText.setText(entry.getMessage());
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView timestampText;
        TextView messageText;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            timestampText = itemView.findViewById(R.id.timestampText);
            messageText = itemView.findViewById(R.id.messageText);
        }
    }
}
