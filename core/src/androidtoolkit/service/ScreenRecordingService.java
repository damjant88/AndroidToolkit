package androidtoolkit.service;

import androidtoolkit.app.StopScreenRecordingOutcome;
import androidtoolkit.domain.RecordingSession;

import java.io.File;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

public class ScreenRecordingService {

    private final CommandExecutor commandExecutor;
    private final DeviceGateway deviceGateway;
    private final StorageService storageService;
    private final HostToolsGateway hostToolsGateway;

    public ScreenRecordingService(
            CommandExecutor commandExecutor,
            DeviceGateway deviceGateway,
            StorageService storageService,
            HostToolsGateway hostToolsGateway
    ) {
        this.commandExecutor = commandExecutor;
        this.deviceGateway = deviceGateway;
        this.storageService = storageService;
        this.hostToolsGateway = hostToolsGateway;
    }

    public boolean isScrcpyAvailable() {
        return hostToolsGateway.findScrcpyExecutable() != null;
    }

    public void startScreenMirrorAsync(String serial) {
        File scrcpyExecutable = hostToolsGateway.findScrcpyExecutable();
        if (scrcpyExecutable == null) {
            throw new IllegalStateException("scrcpy executable not found in System variables Path!");
        }

        Thread thread = new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(scrcpyExecutable.getAbsolutePath(), "-s", serial);
                pb.redirectErrorStream(true);
                pb.start().waitFor();
            } catch (Exception e) {
                System.err.println("Screen mirror failed: " + e.getMessage());
            }
        }, "scrcpy-" + serial);
        thread.setDaemon(true);
        thread.start();
    }

    public void startScreenRecording(String serial, RecordingSession recordingSession) {
        File toolkitDir = storageService.screenRecordingsDir();
        storageService.ensureDirectoryExists(toolkitDir);

        recordingSession.setRecordingFileName("screen_record_" + System.currentTimeMillis() + ".mp4");
        recordingSession.getRecordingInProgress().set(true);
        // Capture PID now while the app is running — we'll use it for log filtering at stop time
        recordingSession.setPid(resolveCurrentPid(serial));
        startScreenMirrorAsync(serial);

        Thread thread = new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("adb", "-s", serial, "shell", "screenrecord",
                        "--bit-rate", "4000000",
                        "/sdcard/" + recordingSession.getRecordingFileName());
                pb.redirectErrorStream(true);
                Process process = pb.start();
                recordingSession.setRecordingProcess(process);
                process.waitFor();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("Screen recording failed: " + e.getMessage());
            }
            // No state cleanup here — stopScreenRecording owns it.
        }, "screenrecord-" + serial);
        thread.setDaemon(true);
        thread.start();
    }

    public StopScreenRecordingOutcome stopScreenRecording(String serial, String deviceName, String pid, RecordingSession recordingSession) throws InterruptedException {
        if (!recordingSession.isActive()) {
            return new StopScreenRecordingOutcome("", false);
        }

        // Stop the recording gracefully by sending SIGINT to screenrecord on the device.
        Process recordingProcess = recordingSession.getRecordingProcess();
        if (recordingProcess != null) {
            commandExecutor.runCommand("adb -s " + serial + " shell pkill -INT screenrecord");
            if (!recordingProcess.waitFor(10, TimeUnit.SECONDS)) {
                recordingProcess.destroyForcibly();
            }
        }

        // Clean up session state
        recordingSession.setRecordingProcess(null);
        recordingSession.getRecordingInProgress().set(false);

        LocalDate currentDate = LocalDate.now();
        String dateString = currentDate.toString();
        File deviceDir = storageService.recordingDir(deviceName, dateString);
        storageService.ensureDirectoryExists(deviceDir);

        String recordingLocation = deviceDir.getPath();
        commandExecutor.runCommand("adb -s " + serial + " pull " + "/sdcard/" + recordingSession.getRecordingFileName() + " " + recordingLocation);
        commandExecutor.runCommand("adb -s " + serial + " shell rm " + "/sdcard/" + recordingSession.getRecordingFileName());
        String effectivePid = (pid != null && !pid.isBlank()) ? pid : recordingSession.getPid();
        RecordingLogResult recordingLogResult = saveScreenRecordingLogs(serial, deviceName, effectivePid, recordingSession.getRecordingFileName());

        recordingSession.setRecordingLocation(recordingLocation);
        return new StopScreenRecordingOutcome(recordingLocation, recordingLogResult.isLogsCaptured());
    }

    private RecordingLogResult saveScreenRecordingLogs(String serial, String deviceName, String fallbackPid, String recordingFileName) {
        String resolvedPid = (fallbackPid != null && !fallbackPid.isBlank()) ? fallbackPid.trim() : "";
        if (resolvedPid.isBlank()) {
            resolvedPid = resolveCurrentPid(serial);
        }

        LocalDate currentDate = LocalDate.now();
        String dateString = currentDate.toString();
        String logFileName = storageService.recordingDir(deviceName, dateString).getPath() + "/" + recordingFileName + ".log";

        if (resolvedPid.isBlank()) {
            String command = "adb -s " + serial + " logcat -d";
            commandExecutor.runCommandAndSave(command, logFileName);
            return new RecordingLogResult(true, "");
        }

        String command = "adb -s " + serial + " logcat -d --pid=" + resolvedPid;
        commandExecutor.runCommandAndSave(command, logFileName);
        return new RecordingLogResult(true, resolvedPid);
    }

    private String resolveCurrentPid(String serial) {
        String installedPackage = deviceGateway.getSafePathPackage(serial);
        if (installedPackage == null || installedPackage.isBlank()) {
            return "";
        }
        return commandExecutor.runCommand("adb -s " + serial + " shell pidof -s " + installedPackage).trim();
    }
}
