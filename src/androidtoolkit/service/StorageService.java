package androidtoolkit.service;

import java.io.File;

public interface StorageService {

    File rootDir();

    File logsDir();

    File screenRecordingsDir();

    File screenshotsDir();

    File screenshotDir(String deviceName);

    File screenshotFile(String deviceName);

    File buildsFile();

    File locationFile();

    File recordingDir(String deviceName, String dateString);

    void ensureDirectoryExists(File directory);
}
