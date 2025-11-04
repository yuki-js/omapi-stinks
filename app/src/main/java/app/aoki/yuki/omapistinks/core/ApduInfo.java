package app.aoki.yuki.omapistinks.core;

/**
 * Holds APDU command and response data with formatting utilities
 */
public class ApduInfo {
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
