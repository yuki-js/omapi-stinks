package app.aoki.yuki.omapistinks;

/**
 * Represents a single OMAPI call log entry with structured data
 */
public class CallLogEntry {
    private final String timestamp;
    private final String shortTimestamp;
    private final String packageName;
    private final String functionName;
    private final String type;
    private final String apduCommand;
    private final String apduResponse;
    private final String aid;
    private final String selectResponse;
    private final String details;

    public CallLogEntry(String timestamp, String shortTimestamp, String packageName, 
                       String functionName, String type, String apduCommand, String apduResponse,
                       String aid, String selectResponse, String details) {
        this.timestamp = timestamp;
        this.shortTimestamp = shortTimestamp;
        this.packageName = packageName;
        this.functionName = functionName;
        this.type = type;
        this.apduCommand = apduCommand;
        this.apduResponse = apduResponse;
        this.aid = aid;
        this.selectResponse = selectResponse;
        this.details = details;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getShortTimestamp() {
        return shortTimestamp;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getType() {
        return type;
    }

    public String getDetails() {
        return details;
    }

    public boolean isTransmit() {
        return Constants.TYPE_TRANSMIT.equals(type);
    }

    public ApduInfo getApduInfo() {
        if (apduCommand != null || apduResponse != null) {
            return new ApduInfo(apduCommand, apduResponse);
        }
        return null;
    }

    public String getAid() {
        return aid;
    }

    public String getSelectResponse() {
        return selectResponse;
    }

    // Legacy compatibility - getMessage() is not used for structured entries
    public String getMessage() {
        return null;
    }

    @Override
    public String toString() {
        return timestamp + " [" + packageName + "] " + functionName + " (" + type + ")";
    }
}
