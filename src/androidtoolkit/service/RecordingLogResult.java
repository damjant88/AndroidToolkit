package androidtoolkit.service;

public class RecordingLogResult {

    private final boolean logsCaptured;
    private final String pidUsed;

    public RecordingLogResult(boolean logsCaptured, String pidUsed) {
        this.logsCaptured = logsCaptured;
        this.pidUsed = pidUsed;
    }

    public boolean isLogsCaptured() {
        return logsCaptured;
    }

    public String getPidUsed() {
        return pidUsed;
    }
}
