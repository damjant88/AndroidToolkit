package androidtoolkit.service;

import java.io.File;

public class LocalStorageService implements StorageService {

    private final StoragePaths storagePaths;

    public LocalStorageService(StoragePaths storagePaths) {
        this.storagePaths = storagePaths;
    }

    @Override
    public File rootDir() {
        return storagePaths.rootDir();
    }

    @Override
    public File logsDir() {
        return storagePaths.logsDir();
    }

    @Override
    public File screenRecordingsDir() {
        return storagePaths.screenRecordingsDir();
    }

    @Override
    public File screenshotsDir() {
        return storagePaths.screenshotsDir();
    }

    @Override
    public File screenshotDir(String deviceName) {
        return storagePaths.screenshotDir(deviceName);
    }

    @Override
    public File screenshotFile(String deviceName) {
        return storagePaths.screenshotFile(deviceName);
    }

    @Override
    public File buildsFile() {
        return storagePaths.buildsFile();
    }

    @Override
    public File locationFile() {
        return storagePaths.locationFile();
    }

    @Override
    public File recordingDir(String deviceName, String dateString) {
        return storagePaths.recordingDir(deviceName, dateString);
    }

    @Override
    public void ensureDirectoryExists(File directory) {
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }
}
