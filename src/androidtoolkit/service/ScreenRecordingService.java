package androidtoolkit.service;

import androidtoolkit.app.StopScreenRecordingOutcome;
import androidtoolkit.domain.RecordingSession;

import javax.swing.*;
import java.io.File;
import java.time.LocalDate;

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
                            "/sdcard/" + recordingSession.getRecordingFileName());
                    pb.redirectErrorStream(true);
                    Process process = pb.start();
                    recordingSession.setRecordingProcess(process);
                    process.waitFor();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    recordingSession.getRecordingInProgress().set(false);
                }
                return null;
            }
        };
        worker.execute();
    }

    public StopScreenRecordingOutcome stopScreenRecording(String serial, String deviceName, String pid, RecordingSession recordingSession) throws InterruptedException {
        if (!recordingSession.getRecordingInProgress().get() && recordingSession.getRecordingProcess() == null) {
            return new StopScreenRecordingOutcome("", false);
        }

        Process recordingProcess = recordingSession.getRecordingProcess();
        if (recordingProcess != null) {
            recordingProcess.destroy();
        }
        LocalDate currentDate = LocalDate.now();
        String dateString = currentDate.toString();
        File deviceDir = storageService.recordingDir(deviceName, dateString);
        storageService.ensureDirectoryExists(deviceDir);

        Thread.sleep(300);
        String recordingLocation = deviceDir.getPath();
        commandExecutor.runCommand("adb -s " + serial + " pull " + "/sdcard/" + recordingSession.getRecordingFileName() + " " + recordingLocation);
        commandExecutor.runCommand("adb -s " + serial + " shell rm " + "/sdcard/" + recordingSession.getRecordingFileName());
        RecordingLogResult recordingLogResult = saveScreenRecordingLogs(serial, deviceName, pid, recordingSession.getRecordingFileName());

        recordingSession.setRecordingProcess(null);
        recordingSession.getRecordingInProgress().set(false);
        recordingSession.setRecordingLocation(recordingLocation);
        return new StopScreenRecordingOutcome(recordingLocation, recordingLogResult.isLogsCaptured());
    }

    private RecordingLogResult saveScreenRecordingLogs(String serial, String deviceName, String fallbackPid, String recordingFileName) {
        String resolvedPid = resolveCurrentPid(serial);
        if (resolvedPid.isBlank()) {
            resolvedPid = fallbackPid == null ? "" : fallbackPid.trim();
        }
        if (resolvedPid.isBlank()) {
            return new RecordingLogResult(false, "");
        }

        deviceGateway.saveScreenRecordingLogs(serial, resolvedPid, deviceName, recordingFileName);
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
