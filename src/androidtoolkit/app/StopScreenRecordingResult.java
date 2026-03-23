package androidtoolkit.app;

public class StopScreenRecordingResult {

    private final boolean stopped;
    private final boolean activeRecordingFound;
    private final String message;
    private final String recordingLocation;
    private final String buttonText;

    public StopScreenRecordingResult(
            boolean stopped,
            boolean activeRecordingFound,
            String message,
            String recordingLocation,
            String buttonText
    ) {
        this.stopped = stopped;
        this.activeRecordingFound = activeRecordingFound;
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
