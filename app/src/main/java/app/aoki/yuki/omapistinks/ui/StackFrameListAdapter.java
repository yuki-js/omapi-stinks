package app.aoki.yuki.omapistinks.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

import app.aoki.yuki.omapistinks.R;

public class StackFrameListAdapter extends BaseAdapter {

    private final StackTraceElement[] frames;
    private final String packageName;
    private final Context context;
    private final LayoutInflater inflater;

    public StackFrameListAdapter(Context context, StackTraceElement[] frames, String packageName) {
        this.context = context;
        this.frames = frames;
        this.packageName = packageName;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return frames != null ? frames.length : 0;
    }

    @Override
    public Object getItem(int position) {
        return frames[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_stack_frame, parent, false);
            holder = new ViewHolder();
            holder.frameCard = (MaterialCardView) convertView;
            holder.methodName = convertView.findViewById(R.id.methodName);
            holder.className = convertView.findViewById(R.id.className);
            holder.fileLocation = convertView.findViewById(R.id.fileLocation);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        StackTraceElement element = frames[position];
        if (element == null) return convertView;

        String className = element.getClassName() != null ? element.getClassName() : "UnknownClass";
        boolean isAppFrame = className.startsWith("app.aoki.yuki.omapistinks") ||
                (packageName != null && !packageName.isEmpty() && className.startsWith(packageName));

        // Method name
        String methodName = element.getMethodName() != null ? element.getMethodName() : "unknownMethod";
        holder.methodName.setText(methodName + "()");
        holder.methodName.setTextColor(isAppFrame ?
                ContextCompat.getColor(context, R.color.colorPrimary) : 0xFF424242);

        // Class name with package prefix dim and class name bold
        SpannableStringBuilder classText = new SpannableStringBuilder();
        String packagePrefix = "";
        String simpleClassName = className;
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            packagePrefix = className.substring(0, lastDot + 1);
            simpleClassName = className.substring(lastDot + 1);
        }
        classText.append(packagePrefix);
        int classStart = classText.length();
        classText.append(simpleClassName);
        classText.setSpan(new StyleSpan(Typeface.BOLD),
                classStart, classText.length(), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
        holder.className.setText(classText);

        // File location
        String fileName = element.getFileName() != null ? element.getFileName() : "Unknown";
        int lineNum = element.getLineNumber();
        String location = fileName + ":" + (lineNum >= 0 ? lineNum : "?");
        if (element.isNativeMethod()) {
            location += " (native)";
        }
        holder.fileLocation.setText(location);

        // Set card background color
        holder.frameCard.setCardBackgroundColor(isAppFrame ? 0xFFF3E5F5 : 0xFFF5F5F5);

        // Long-press to copy frame
        convertView.setOnLongClickListener(v -> {
            copyToClipboard(context, "Stack Frame", "at " + element.toString());
            return true;
        });

        return convertView;
    }

    private void copyToClipboard(Context context, String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, label + " copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    static class ViewHolder {
        MaterialCardView frameCard;
        TextView methodName;
        TextView className;
        TextView fileLocation;
    }
}