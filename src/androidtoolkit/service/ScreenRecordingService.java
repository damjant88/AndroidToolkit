package androidtoolkit.service;

import androidtoolkit.domain.RecordingSession;

import javax.swing.*;
import java.io.File;
import java.time.LocalDate;

public class ScreenRecordingService {

    private final Util utility;
    private final StoragePaths storagePaths;

    public ScreenRecordingService(Util utility, StoragePaths storagePaths) {
        this.utility = utility;
        this.storagePaths = storagePaths;
    }

    public boolean isScrcpyAvailable() {
        return findScrcpyExecutable() != null;
    }

    public void startScreenMirrorAsync(String serial) {
        File scrcpyExecutable = findScrcpyExecutable();
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
        File toolkitDir = storagePaths.screenRecordingsDir();
        if (!toolkitDir.exists()) {
            toolkitDir.mkdirs();
        }

        recordingSession.setRecordingFileName("screen_record_" + System.currentTimeMillis() + ".mp4");
        startScreenMirrorAsync(serial);

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                recordingSession.getRecordingInProgress().set(true);
                try {
                    ProcessBuilder pb = new ProcessBuilder("adb", "-s", serial, "shell", "screenrecord",
                            "/sdcard/" + recordingSession.getRecordingFileName());
                    pb.redirectErrorStream(true);
                    Process process = pb.start();
                    recordingSession.setRecordingProcess(process);
                    process.waitFor();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    System.out.println("Screen recording interrupted.");
                } finally {
                    recordingSession.getRecordingInProgress().set(false);
                }
                return null;
            }
        };
        worker.execute();
    }

    public String stopScreenRecording(String serial, String deviceName, RecordingSession recordingSession) throws InterruptedException {
        if (recordingSession.getRecordingProcess() == null) {
            return "";
        }

        recordingSession.getRecordingProcess().destroy();
        LocalDate currentDate = LocalDate.now();
        String dateString = currentDate.toString();
        File deviceDir = storagePaths.recordingDir(deviceName, dateString);
        if (!deviceDir.exists()) {
            deviceDir.mkdirs();
        }

        Thread.sleep(300);
        String recordingLocation = deviceDir.getPath();
        utility.runCommand("adb -s " + serial + " pull " + "/sdcard/" + recordingSession.getRecordingFileName() + " " + recordingLocation);
        utility.runCommand("adb -s " + serial + " shell rm " + "/sdcard/" + recordingSession.getRecordingFileName());

        String appFlavour = utility.getSafePathPackage(serial);
        utility.saveLogs(serial, appFlavour, recordingLocation);

        File logsFolder = new File(recordingLocation + "/logs");
        File[] logFiles = logsFolder.listFiles((dir, name) -> name.endsWith(".log"));
        if (logFiles != null) {
            for (File logFile : logFiles) {
                if (logFile.isFile()) {
                    File destination = new File(recordingLocation, recordingSession.getRecordingFileName() + ".log");
                    logFile.renameTo(destination);
                }
            }
        }
        logsFolder.delete();

        recordingSession.setRecordingProcess(null);
        recordingSession.getRecordingInProgress().set(false);
        recordingSession.setRecordingLocation(recordingLocation);
        return recordingLocation;
    }

    private File findScrcpyExecutable() {
        String[] pathDirectories = System.getenv("PATH").split(File.pathSeparator);
        for (String directory : pathDirectories) {
            File scrcpyExecutable = new File(directory, "scrcpy.exe");
            if (scrcpyExecutable.exists()) {
                return scrcpyExecutable;
            }
        }
        return null;
    }
}
