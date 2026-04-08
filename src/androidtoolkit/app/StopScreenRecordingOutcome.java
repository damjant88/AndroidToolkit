package androidtoolkit.app;

public class StopScreenRecordingOutcome {

    private final String recordingLocation;
    private final boolean logsCaptured;

    public StopScreenRecordingOutcome(String recordingLocation, boolean logsCaptured) {
        this.recordingLocation = recordingLocation;
        this.logsCaptured = logsCaptured;
    }

    public String getRecordingLocation() {
        return recordingLocation;
    }

    public boolean isLogsCaptured() {
        return logsCaptured;
    }
}
