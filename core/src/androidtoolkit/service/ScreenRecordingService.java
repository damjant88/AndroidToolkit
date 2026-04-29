package androidtoolkit.service;

import androidtoolkit.app.StopScreenRecordingOutcome;
import androidtoolkit.domain.RecordingSession;

import javax.swing.*;
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

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                ProcessBuilder pb = new ProcessBuilder(scrcpyExecutable.getAbsolutePath(), "-s", serial);
                pb.redirectErrorStream(true);
                pb.start().waitFor();
                return null;
            }
        };
        worker.execute();
    }

    public void startScreenRecording(String serial, RecordingSession recordingSession) {
        File toolkitDir = storageService.screenRecordingsDir();
        storageService.ensureDirectoryExists(toolkitDir);

        recordingSession.setRecordingFileName("screen_record_" + System.currentTimeMillis() + ".mp4");
        recordingSession.getRecordingInProgress().set(true);
        startScreenMirrorAsync(serial);

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
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
                }
                // No finally block here — stopScreenRecording owns the state cleanup.
                // This avoids a race condition where both threads set recordingInProgress to false.
                return null;
            }
        };
        worker.execute();
    }

    public StopScreenRecordingOutcome stopScreenRecording(String serial, String deviceName, String pid, RecordingSession recordingSession) throws InterruptedException {
        if (!recordingSession.isActive()) {
            return new StopScreenRecordingOutcome("", false);
        }

        // Stop the recording gracefully by sending SIGINT to screenrecord on the device.
        // This lets it finalize the MP4 file header properly (destroying the local process corrupts the file).
        Process recordingProcess = recordingSession.getRecordingProcess();
        if (recordingProcess != null) {
            commandExecutor.runCommand("adb -s " + serial + " shell pkill -INT screenrecord");
            // Wait for the process to finish writing
            if (!recordingProcess.waitFor(10, TimeUnit.SECONDS)) {
                recordingProcess.destroyForcibly();
            }
        }

        // Now that the process is confirmed dead, clean up the session state
        recordingSession.setRecordingProcess(null);
        recordingSession.getRecordingInProgress().set(false);

        LocalDate currentDate = LocalDate.now();
        String dateString = currentDate.toString();
        File deviceDir = storageService.recordingDir(deviceName, dateString);
        storageService.ensureDirectoryExists(deviceDir);

        String recordingLocation = deviceDir.getPath();
        commandExecutor.runCommand("adb -s " + serial + " pull " + "/sdcard/" + recordingSession.getRecordingFileName() + " " + recordingLocation);
        commandExecutor.runCommand("adb -s " + serial + " shell rm " + "/sdcard/" + recordingSession.getRecordingFileName());
        RecordingLogResult recordingLogResult = saveScreenRecordingLogs(serial, deviceName, pid, recordingSession.getRecordingFileName());

        recordingSession.setRecordingLocation(recordingLocation);
        return new StopScreenRecordingOutcome(recordingLocation, recordingLogResult.isLogsCaptured());
    }

    private RecordingLogResult saveScreenRecordingLogs(String serial, String deviceName, String fallbackPid, String recordingFileName) {
        String resolvedPid = resolveCurrentPid(serial);
        if (resolvedPid.isBlank()) {
            resolvedPid = fallbackPid == null ? "" : fallbackPid.trim();
        }

        LocalDate currentDate = LocalDate.now();
        String dateString = currentDate.toString();
        String logFileName = storageService.recordingDir(deviceName, dateString).getPath() + "/" + recordingFileName + ".log";

        if (resolvedPid.isBlank()) {
            // No PID available — save full logcat dump instead of nothing
            String command = "adb -s " + serial + " logcat -d";
            commandExecutor.runCommandAndSave(command, logFileName);
            return new RecordingLogResult(true, "");
        }

        // Save filtered logcat for the app's PID
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
