package androidtoolkit.app;

public class RecordingActionResponse {

    private final boolean success;
    private final String message;
    private final String buttonText;
    private final String recordingLocation;

    public RecordingActionResponse(boolean success, String message, String buttonText, String recordingLocation) {
        this.success = success;
        this.message = message;
        this.buttonText = buttonText;
        this.recordingLocation = recordingLocation;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getButtonText() {
        return buttonText;
    }

    public String getRecordingLocation() {
        return recordingLocation;
    }
}
