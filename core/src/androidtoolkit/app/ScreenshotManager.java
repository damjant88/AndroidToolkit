package androidtoolkit.app;

public class ScreenshotManager {

    private final DeviceOperations deviceOperations;

    public ScreenshotManager(DeviceOperations deviceOperations) {
        this.deviceOperations = deviceOperations;
    }

    public ScreenshotCaptureResponse captureScreenshot(String serial, String deviceName) {
        ScreenshotResult result = deviceOperations.captureScreenshot(new ScreenshotRequest(serial, deviceName));
        return new ScreenshotCaptureResponse(result.getScreenshotFolder(), result.getMessage());
    }
}
