package app.aoki.yuki.omapistinks.core;

/**
 * Holds APDU command and response data with formatting utilities.
 * Formatting preserves original casing, validates only minimal structure,
 * and returns the original string when it looks invalid to avoid behavior changes.
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

    private static final int HEADER_BYTES = 4;
    private static final int HEX_PER_BYTE = 2;
    private static final String EXTENDED_MARKER = "00";

    private static boolean isEvenHexLength(String s) {
        return s != null && (s.length() % 2 == 0);
    }

    private static String joinWithSpaces(java.util.List<String> parts) {
        return String.join(" ", parts);
    }

    public String getFormattedCommand() {
        final String cmd = this.command;

        // Keep early returns to avoid behavior change
        if (cmd == null || cmd.length() < HEADER_BYTES * HEX_PER_BYTE) {
            return cmd;
        }
        if (!isEvenHexLength(cmd)) {
            return cmd;
        }

        java.util.ArrayList<String> out = new java.util.ArrayList<>(8);

        // Header: CLA INS P1 P2
        out.add(cmd.substring(0, 2));
        out.add(cmd.substring(2, 4));
        out.add(cmd.substring(4, 6));
        out.add(cmd.substring(6, 8));

        int totalBytes = cmd.length() / HEX_PER_BYTE;
        int restBytes = totalBytes - HEADER_BYTES;
        if (restBytes <= 0) {
            // Case 1: header only
            return joinWithSpaces(out);
        }

        String rest = cmd.substring(HEADER_BYTES * HEX_PER_BYTE);

        // Case 2S: Le only (1 byte)
        if (restBytes == 1) {
            out.add(rest.substring(0, 2));
            return joinWithSpaces(out);
        }

        String b0 = rest.substring(0, 2);

        // Extended length signaled by 0x00 after header
        if (EXTENDED_MARKER.equalsIgnoreCase(b0)) {
            if (restBytes == 3) {
                // Case 2E: 00 + Le(2)
                String leHi = rest.substring(2, 4);
                String leLo = rest.substring(4, 6);
                out.add("00");
                out.add(leHi);
                out.add(leLo);
                return joinWithSpaces(out);
            } else if (restBytes >= 3) {
                // Case 3E / 4E: 00 Lc(2) Data [Le(2)]
                String lcHi = rest.substring(2, 4);
                String lcLo = rest.substring(4, 6);
                int lcVal = Integer.parseInt(lcHi, 16) * 256 + Integer.parseInt(lcLo, 16);

                int dataHexLen = lcVal * HEX_PER_BYTE;
                int afterData = 6 + dataHexLen;

                if (afterData > rest.length()) {
                    // Invalid length: append raw remainder to avoid misleading formatting
                    out.add(rest);
                    return joinWithSpaces(out);
                }

                out.add("00");
                out.add(lcHi);
                out.add(lcLo);

                if (dataHexLen > 0) {
                    String dataHex = rest.substring(6, afterData);
                    out.add(dataHex);
                }

                int remaining = rest.length() - afterData;
                if (remaining == 0) {
                    // Case 3E: no Le
                    return joinWithSpaces(out);
                } else if (remaining == 4) {
                    // Case 4E: trailing two-byte Le
                    String leHi = rest.substring(afterData, afterData + 2);
                    String leLo = rest.substring(afterData + 2, afterData + 4);
                    out.add(leHi);
                    out.add(leLo);
                    return joinWithSpaces(out);
                } else {
                    // Unknown trailing content; append raw
                    out.add(rest.substring(afterData));
                    return joinWithSpaces(out);
                }
            } else {
                // Degenerate - append raw
                out.add(rest);
                return joinWithSpaces(out);
            }
        } else {
            // Short length forms: Case 3S / 4S
            int lcVal = Integer.parseInt(b0, 16);
            int dataHexLen = lcVal * HEX_PER_BYTE;

            if (restBytes == 1 + lcVal) {
                // Case 3S: Lc(1) + Data
                out.add(b0);
                String dataHex = rest.substring(2);
                if (!dataHex.isEmpty()) {
                    out.add(dataHex);
                }
                return joinWithSpaces(out);
            } else if (restBytes == 1 + lcVal + 1) {
                // Case 4S: Lc(1) + Data + Le(1)
                out.add(b0);
                if (dataHexLen > 0) {
                    String dataHex = rest.substring(2, 2 + dataHexLen);
                    out.add(dataHex);
                }
                String le = rest.substring(2 + dataHexLen, 2 + dataHexLen + 2);
                out.add(le);
                return joinWithSpaces(out);
            } else {
                // Unknown/invalid: append raw remainder
                out.add(rest);
                return joinWithSpaces(out);
            }
        }
    }

    public String getFormattedResponse() {
        final String res = this.response;
        if (res == null || res.length() < 4) {
            return res;
        }
        if (!isEvenHexLength(res)) {
            return res; // Return as-is if invalid
        }

        // Format as: [Data] SW1 SW2 (Data kept contiguous without spacing)
        int len = res.length();
        String sw1 = res.substring(len - 4, len - 2);
        String sw2 = res.substring(len - 2);
        String data = res.substring(0, len - 4);

        if (data.isEmpty()) {
            return sw1 + " " + sw2;
        } else {
            return data + " " + sw1 + " " + sw2;
        }
    }
}