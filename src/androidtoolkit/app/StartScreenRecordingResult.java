package androidtoolkit.app;

public class StartScreenRecordingResult {

    private final boolean started;
    private final String message;
    private final String buttonText;

    public StartScreenRecordingResult(boolean started, String message, String buttonText) {
        this.started = started;
        this.message = message;
        this.buttonText = buttonText;
    }

    public boolean isStarted() {
        return started;
    }

    public String getMessage() {
        return message;
    }

    public String getButtonText() {
        return buttonText;
    }
}
