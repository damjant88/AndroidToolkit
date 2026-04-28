package androidtoolkit.app;

public class ScreenshotCaptureResponse {

    private final String screenshotFolder;
    private final String message;

    public ScreenshotCaptureResponse(String screenshotFolder, String message) {
        this.screenshotFolder = screenshotFolder;
        this.message = message;
    }

    public String getScreenshotFolder() {
        return screenshotFolder;
    }

    public String getMessage() {
        return message;
    }
}
