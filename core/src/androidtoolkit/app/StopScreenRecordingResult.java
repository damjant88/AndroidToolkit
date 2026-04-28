package androidtoolkit.app;

public class StopScreenRecordingResult {

    private final boolean stopped;
    private final boolean activeRecordingFound;
    private final boolean logsCaptured;
    private final String message;
    private final String recordingLocation;
    private final String buttonText;

    public StopScreenRecordingResult(
            boolean stopped,
            boolean activeRecordingFound,
            boolean logsCaptured,
            String message,
            String recordingLocation,
            String buttonText
    ) {
        this.stopped = stopped;
        this.activeRecordingFound = activeRecordingFound;
        this.logsCaptured = logsCaptured;
        this.message = message;
        this.recordingLocation = recordingLocation;
        this.buttonText = buttonText;
    }

    public boolean isStopped() {
        return stopped;
    }

    public boolean isActiveRecordingFound() {
        return activeRecordingFound;
    }

    public boolean isLogsCaptured() {
        return logsCaptured;
    }

    public String getMessage() {
        return message;
    }

    public String getRecordingLocation() {
        return recordingLocation;
    }

    public String getButtonText() {
        return buttonText;
    }
}
