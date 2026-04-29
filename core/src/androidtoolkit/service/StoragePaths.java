package androidtoolkit.service;

import java.io.File;

public class StoragePaths {

    private static final String DEFAULT_ROOT = "C:/AdbToolkit";
    private final String rootPath;

    public StoragePaths() {
        this(System.getProperty("adbtoolkit.root", DEFAULT_ROOT));
    }

    public StoragePaths(String rootPath) {
        this.rootPath = rootPath;
    }

    public String rootPath() {
        return rootPath;
    }

    public File rootDir() {
        return new File(rootPath);
    }

    public File logsDir() {
        return new File(rootDir(), "Logs");
    }

    public File screenRecordingsDir() {
        return new File(rootDir(), "Screen_Recordings");
    }

    public File screenshotsDir() {
        return new File(rootDir(), "Screenshots");
    }

    public File screenshotDir(String deviceName) {
        return new File(screenshotsDir(), sanitize(deviceName));
    }

    public File screenshotFile(String deviceName) {
        return new File(screenshotDir(deviceName), "screenshot.png");
    }

    public File buildsFile() {
        return new File(rootDir(), "builds.ser");
    }

    public File locationFile() {
        return new File(rootDir(), "location.ser");
    }

    public File recordingDir(String deviceName, String dateString) {
        return new File(screenRecordingsDir(), sanitize(deviceName) + "/" + dateString);
    }

    // Replace characters that are invalid in Windows file/folder names
    private String sanitize(String name) {
        return name.replace(":", "_").replace("?", "_").replace("*", "_");
    }
}
