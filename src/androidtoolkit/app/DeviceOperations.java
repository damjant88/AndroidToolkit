package androidtoolkit.app;

import androidtoolkit.service.DeviceActionService;
import androidtoolkit.service.ScreenRecordingService;

import java.io.File;

public class DeviceOperations {

    private final DeviceActionService deviceActionService;
    private final ScreenRecordingService screenRecordingService;

    public DeviceOperations(DeviceActionService deviceActionService, ScreenRecordingService screenRecordingService) {
        this.deviceActionService = deviceActionService;
        this.screenRecordingService = screenRecordingService;
    }

    public ScreenshotResult captureScreenshot(ScreenshotRequest request) {
        File screenshotFolder = deviceActionService.captureScreenshot(request.getSerial(), request.getDeviceName());
        return new ScreenshotResult(
                screenshotFolder.getPath(),
                "Screenhot is captured on " + request.getDeviceName()
        );
    }

    public WifiDebugResult toggleWifiDebugging(WifiDebugRequest request) {
        if (!request.hasWifiIp()) {
            return new WifiDebugResult(
                    true,
                    false,
                    "Connect the device " + request.getDeviceName() + " to WiFi and click on 'Display Connected Devices' button to refresh IP! ",
                    "WiFi Debug"
            );
        }

        if (!request.isWifiDebugSession()) {
            deviceActionService.enableWifiDebugging(request.getSerial(), request.getIpAddress());
            return new WifiDebugResult(
                    false,
                    true,
                    "WiFi debugging is enabled on " + request.getDeviceName() + "!\n" +
                            "If prompted on the device, allow wireless debugging on specific wifi network.\n" +
                            "You may disconnect USB cable from this device.",
                    "Disable WiFi"
            );
        }

        deviceActionService.disableWifiDebugging(request.getSerial(), request.getIpAddress());
        return new WifiDebugResult(
                false,
                false,
                "WiFi debugging is disabled on " + request.getDeviceName(),
                "WiFi Debug"
        );
    }

    public DeviceMessageResult enableFirebaseDebugging(FirebaseDebugRequest request) {
        deviceActionService.enableFirebaseDebugging(request.getSerial(), request.getPackageName());
        return new DeviceMessageResult(
                "Firebase Debugging enabled on " + request.getDeviceName() + "!" + "\n"
                        + "Make sure 'Logging Analytics Events' toggle button is also enabled in Debug menu."
        );
    }

    public DeviceMessageResult rebootDevice(RebootDeviceRequest request) {
        deviceActionService.reboot(request.getSerial());
        return new DeviceMessageResult(request.getDeviceName() + " is restarted!");
    }

    public UninstallAppResult uninstallApp(UninstallAppRequest request) {
        deviceActionService.uninstallApp(request.getSerial(), request.getPackageName());
        return new UninstallAppResult(true, "App is uninstalled from " + request.getDeviceName() + "!");
    }

    public DeviceMessageResult startScreenMirror(ScreenMirrorRequest request) {
        screenRecordingService.startScreenMirrorAsync(request.getSerial());
        return new DeviceMessageResult("Screen mirror is started on " + request.getDeviceName());
    }

    public StartScreenRecordingResult startScreenRecording(StartScreenRecordingRequest request) {
        screenRecordingService.startScreenRecording(request.getSerial(), request.getRecordingSession());
        return new StartScreenRecordingResult(
                true,
                "Screen recording is started on " + request.getDeviceName() + ".",
                "Stop Record"
        );
    }

    public StopScreenRecordingResult stopScreenRecording(StopScreenRecordingRequest request) throws InterruptedException {
        if (!request.getRecordingSession().getRecordingInProgress().get()
                && request.getRecordingSession().getRecordingProcess() == null) {
            return new StopScreenRecordingResult(
                    false,
                    false,
                    false,
                    "No active recording!",
                    "",
                    "Start Record"
            );
        }

        StopScreenRecordingOutcome outcome = screenRecordingService.stopScreenRecording(
                request.getSerial(),
                request.getDeviceName(),
                request.getPid(),
                request.getRecordingSession()
        );
        String recordingLocation = outcome.getRecordingLocation();

        if (recordingLocation == null || recordingLocation.isBlank()) {
            return new StopScreenRecordingResult(
                    false,
                    false,
                    false,
                    "No active recording!",
                    "",
                    "Start Record"
            );
        }

        String message = "Screen recording is stopped on " + request.getDeviceName() + "." + "\n"
                + "Screen recording saved to:\n" + recordingLocation;
        if (!outcome.isLogsCaptured()) {
            message = message + "\n" + "Recording logs could not be captured for this session.";
        }

        return new StopScreenRecordingResult(
                true,
                true,
                outcome.isLogsCaptured(),
                message,
                recordingLocation,
                "Start Record"
        );
    }
}
