package app.aoki.yuki.omapistinks;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CallLogger {
    private static CallLogger instance;
    private final List<CallLogEntry> logs;
    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat shortDateFormat;
    private final int MAX_LOGS = 1000;

    private CallLogger() {
        logs = new ArrayList<>();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        shortDateFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
    }

    public static synchronized CallLogger getInstance() {
        if (instance == null) {
            instance = new CallLogger();
        }
        return instance;
    }

    public synchronized void addLog(String message) {
        addLog(message, null);
    }

    public synchronized void addLog(String message, String packageName) {
        String timestamp = dateFormat.format(new Date());
        String shortTimestamp = shortDateFormat.format(new Date());
        CallLogEntry entry = new CallLogEntry(timestamp, shortTimestamp, message, packageName);
        logs.add(entry);
        
        // Keep only the last MAX_LOGS entries
        if (logs.size() > MAX_LOGS) {
            logs.remove(0);
        }
    }
    
    public synchronized void addStructuredLog(String packageName, String function, String type, 
                                             String apduCommand, String apduResponse, 
                                             String aid, String selectResponse, String details) {
        String timestamp = dateFormat.format(new Date());
        String shortTimestamp = shortDateFormat.format(new Date());
        CallLogEntry entry = new CallLogEntry(timestamp, shortTimestamp, packageName, function, 
                                             type, apduCommand, apduResponse, aid, selectResponse, details);
        logs.add(entry);
        
        // Keep only the last MAX_LOGS entries
        if (logs.size() > MAX_LOGS) {
            logs.remove(0);
        }
    }

    public synchronized List<CallLogEntry> getLogs() {
        return new ArrayList<>(logs);
    }

    public synchronized void clearLogs() {
        logs.clear();
    }

    public static class CallLogEntry {
        private final String timestamp;
        private final String shortTimestamp;
        private final String message;
        private final String packageName;
        private final String functionName;
        private final String details;
        private final boolean isTransmit;
        private final ApduInfo apduInfo;
        private final String type;
        private final String aid;
        private final String selectResponse;

        // Legacy constructor for text-based logs
        public CallLogEntry(String timestamp, String shortTimestamp, String message, String packageName) {
            this.timestamp = timestamp;
            this.shortTimestamp = shortTimestamp;
            this.message = message;
            this.packageName = packageName;
            this.type = null;
            this.aid = null;
            this.selectResponse = null;
            
            // Parse function name and details
            ParsedInfo parsed = parseMessage(message);
            this.functionName = parsed.functionName;
            this.details = parsed.details;
            this.isTransmit = parsed.isTransmit;
            this.apduInfo = parsed.apduInfo;
        }
        
        // New constructor for structured logs
        public CallLogEntry(String timestamp, String shortTimestamp, String packageName, String functionName,
                           String type, String apduCommand, String apduResponse, 
                           String aid, String selectResponse, String details) {
            this.timestamp = timestamp;
            this.shortTimestamp = shortTimestamp;
            this.packageName = packageName;
            this.functionName = shortenFunctionName(functionName);
            this.type = type;
            this.aid = aid;
            this.selectResponse = selectResponse;
            this.details = details;
            this.message = null;
            this.isTransmit = Constants.TYPE_TRANSMIT.equals(type);
            
            // Create ApduInfo from structured data
            if (apduCommand != null || apduResponse != null) {
                this.apduInfo = new ApduInfo(apduCommand, apduResponse);
            } else {
                this.apduInfo = null;
            }
        }

        private static class ParsedInfo {
            String functionName;
            String details;
            boolean isTransmit;
            ApduInfo apduInfo;
            
            ParsedInfo(String functionName, String details, boolean isTransmit, ApduInfo apduInfo) {
                this.functionName = functionName;
                this.details = details;
                this.isTransmit = isTransmit;
                this.apduInfo = apduInfo;
            }
        }

        private ParsedInfo parseMessage(String msg) {
            if (msg == null) {
                return new ParsedInfo("Unknown", "", false, null);
            }

            // Extract function name (everything before first '(' or '=')
            Pattern funcPattern = Pattern.compile("^(\\[SYSTEM\\]\\s+)?([a-zA-Z.]+)\\s*\\(");
            Matcher funcMatcher = funcPattern.matcher(msg);
            String func = "Unknown";
            int detailStartIndex = 0;
            
            if (funcMatcher.find()) {
                func = funcMatcher.group(2);
                detailStartIndex = funcMatcher.end() - 1;
            } else {
                // Try to match "function() = result" pattern
                Pattern resultPattern = Pattern.compile("^(\\[SYSTEM\\]\\s+)?([a-zA-Z.]+)\\s*\\(\\)");
                Matcher resultMatcher = resultPattern.matcher(msg);
                if (resultMatcher.find()) {
                    func = resultMatcher.group(2);
                    detailStartIndex = resultMatcher.end();
                }
            }

            // Shorten common function names
            func = shortenFunctionName(func);

            // Check if this is a transmit call
            boolean isTransmit = msg.contains("transmit(") || msg.contains("transmit()");

            // Parse APDU if this is a transmit call
            ApduInfo apdu = null;
            if (isTransmit) {
                apdu = parseApdu(msg);
            }

            // Extract details (everything after function name)
            String detail = msg;
            if (detailStartIndex > 0 && detailStartIndex < msg.length()) {
                detail = msg.substring(detailStartIndex);
            }

            return new ParsedInfo(func, detail, isTransmit, apdu);
        }

        private String shortenFunctionName(String func) {
            // Create short forms for common functions
            if (func.endsWith(".transmit")) return "TX";
            if (func.endsWith(".openBasicChannel")) return "OpenBasic";
            if (func.endsWith(".openLogicalChannel")) return "OpenLogical";
            if (func.endsWith(".getReaders")) return "GetReaders";
            if (func.endsWith(".openSession")) return "OpenSession";
            if (func.endsWith(".getATR")) return "GetATR";
            if (func.endsWith(".close")) return "Close";
            if (func.endsWith(".isClosed")) return "IsClosed";
            if (func.endsWith(".isSecureElementPresent")) return "SEPresent";
            if (func.endsWith(".isConnected")) return "Connected";
            
            // Return last component if it has a dot
            int lastDot = func.lastIndexOf('.');
            if (lastDot > 0) {
                return func.substring(lastDot + 1);
            }
            return func;
        }

        private ApduInfo parseApdu(String msg) {
            // Look for command= or returned hex patterns
            Pattern cmdPattern = Pattern.compile("command=([0-9A-Fa-f]+)");
            Pattern retPattern = Pattern.compile("returned\\s+([0-9A-Fa-f]+)");
            
            Matcher cmdMatcher = cmdPattern.matcher(msg);
            Matcher retMatcher = retPattern.matcher(msg);
            
            String command = null;
            String response = null;
            
            if (cmdMatcher.find()) {
                command = cmdMatcher.group(1);
            }
            if (retMatcher.find()) {
                response = retMatcher.group(1);
            }
            
            if (command != null || response != null) {
                return new ApduInfo(command, response);
            }
            return null;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getShortTimestamp() {
            return shortTimestamp;
        }

        public String getMessage() {
            return message;
        }

        public String getPackageName() {
            return packageName;
        }

        public String getFunctionName() {
            return functionName;
        }

        public String getDetails() {
            return details;
        }

        public boolean isTransmit() {
            return isTransmit;
        }

        public ApduInfo getApduInfo() {
            return apduInfo;
        }
        
        public String getType() {
            return type;
        }
        
        public String getAid() {
            return aid;
        }
        
        public String getSelectResponse() {
            return selectResponse;
        }

        @Override
        public String toString() {
            return timestamp + " - " + message;
        }
    }

    public static class ApduInfo {
        private final String command;
        private final String response;

        public ApduInfo(String command, String response) {
            this.command = command;
            this.response = response;
        }

        public String getCommand() {
            return command;
        }

        public String getResponse() {
            return response;
        }

        public String getFormattedCommand() {
            if (command == null || command.length() < 8) {
                return command;
            }
            
            // Validate even length (hex strings should have pairs of characters)
            if (command.length() % 2 != 0) {
                return command; // Return as-is if invalid
            }
            
            // Format as: CLA INS P1 P2 [Lc [Data]] [Le]
            StringBuilder sb = new StringBuilder();
            sb.append(command.substring(0, 2)).append(" "); // CLA
            sb.append(command.substring(2, 4)).append(" "); // INS
            sb.append(command.substring(4, 6)).append(" "); // P1
            sb.append(command.substring(6, 8));             // P2
            
            if (command.length() > 8) {
                sb.append(" ").append(command.substring(8));
            }
            
            return sb.toString();
        }

        public String getFormattedResponse() {
            if (response == null || response.length() < 4) {
                return response;
            }
            
            // Validate even length (hex strings should have pairs of characters)
            if (response.length() % 2 != 0) {
                return response; // Return as-is if invalid
            }
            
            // Format as: [Data] SW1 SW2
            int len = response.length();
            String sw1sw2 = response.substring(len - 4);
            String data = response.substring(0, len - 4);
            
            if (data.isEmpty()) {
                return sw1sw2.substring(0, 2) + " " + sw1sw2.substring(2);
            } else {
                return data + " " + sw1sw2.substring(0, 2) + " " + sw1sw2.substring(2);
            }
        }
    }
}
