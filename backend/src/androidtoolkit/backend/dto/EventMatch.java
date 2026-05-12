package androidtoolkit.backend.dto;

import java.util.List;

/**
 * Represents a matched event from logcat with surrounding context lines.
 */
public class EventMatch {

    private String serial;
    private String matchedLine;
    private List<String> contextLines; // 5 before + matched + 5 after = 11 lines
    private long timestamp;

    public EventMatch(String serial, String matchedLine, List<String> contextLines) {
        this.serial = serial;
        this.matchedLine = matchedLine;
        this.contextLines = contextLines;
        this.timestamp = System.currentTimeMillis();
    }

    public String getSerial() { return serial; }
    public String getMatchedLine() { return matchedLine; }
    public List<String> getContextLines() { return contextLines; }
    public long getTimestamp() { return timestamp; }
}
