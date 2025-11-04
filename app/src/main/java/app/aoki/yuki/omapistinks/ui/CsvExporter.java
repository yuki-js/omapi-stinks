package app.aoki.yuki.omapistinks.ui;

import app.aoki.yuki.omapistinks.core.ApduInfo;
import app.aoki.yuki.omapistinks.core.CallLogEntry;
import app.aoki.yuki.omapistinks.core.Constants;

import java.util.List;

/**
 * Handles CSV export functionality for CallLogEntry data
 * Provides proper CSV formatting with escaping and comprehensive field coverage
 */
public class CsvExporter {
    
    /**
     * Exports a list of CallLogEntry objects to CSV format
     * 
     * @param entries List of log entries to export
     * @return CSV formatted string with header and data rows
     */
    public static String exportToCsv(List<CallLogEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return "";
        }
        
        StringBuilder csvBuilder = new StringBuilder();
        
        // CSV Header
        appendCsvHeader(csvBuilder);
        
        // CSV Data rows
        for (CallLogEntry entry : entries) {
            appendCsvRow(csvBuilder, entry);
        }
        
        return csvBuilder.toString();
    }
    
    /**
     * Appends CSV header row with all column names
     */
    private static void appendCsvHeader(StringBuilder builder) {
        builder.append("Timestamp,")
               .append("Package,")
               .append("Function,")
               .append("Type,")
               .append("Thread ID,")
               .append("Thread Name,")
               .append("Process ID,")
               .append("Execution Time (ms),")
               .append("APDU Command,")
               .append("APDU Response,")
               .append("AID,")
               .append("Select Response,")
               .append("Details")
               .append("\n");
    }
    
    /**
     * Appends a single log entry as a CSV row
     */
    private static void appendCsvRow(StringBuilder builder, CallLogEntry entry) {
        builder.append(escapeCsv(entry.getTimestamp())).append(",");
        builder.append(escapeCsv(entry.getPackageName())).append(",");
        builder.append(escapeCsv(entry.getFunctionName())).append(",");
        builder.append(escapeCsv(entry.getType())).append(",");
        builder.append(entry.getThreadId()).append(",");
        builder.append(escapeCsv(entry.getThreadName())).append(",");
        builder.append(entry.getProcessId()).append(",");
        builder.append(entry.getExecutionTimeMs()).append(",");
        
        // APDU Command and Response
        if (entry.getApduInfo() != null) {
            builder.append(escapeCsv(entry.getApduInfo().getCommand())).append(",");
            builder.append(escapeCsv(entry.getApduInfo().getResponse())).append(",");
        } else {
            builder.append(",,");
        }
        
        // AID, Select Response, Details
        builder.append(escapeCsv(entry.getAid())).append(",");
        builder.append(escapeCsv(entry.getSelectResponse())).append(",");
        builder.append(escapeCsv(entry.getDetails()));
        builder.append("\n");
    }
    
    /**
     * Escapes a string value for CSV format
     * Handles commas, quotes, and newlines according to RFC 4180
     * 
     * @param value The string value to escape
     * @return Escaped string suitable for CSV, or empty string if value is null
     */
    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        
        // If value contains comma, quote, or newline, wrap in quotes and escape internal quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        
        return value;
    }
}
