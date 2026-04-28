package androidtoolkit.app;

public class ScreenshotResult {

    private final String screenshotFolder;
    private final String message;

    public ScreenshotResult(String screenshotFolder, String message) {
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
