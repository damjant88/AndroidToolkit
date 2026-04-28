package androidtoolkit.backend.controller;

import androidtoolkit.app.AppServices;
import androidtoolkit.service.StoragePaths;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Serves files (screenshots, logs) over HTTP so the browser can display or download them.
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final StoragePaths storagePaths;

    public FileController(AppServices appServices) {
        this.storagePaths = appServices.storagePaths();
    }

    @GetMapping("/screenshot/{deviceName}")
    public ResponseEntity<Resource> getScreenshot(@PathVariable String deviceName) {
        File file = storagePaths.screenshotFile(deviceName);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(new FileSystemResource(file));
    }

    /**
     * Zips all log files in the logs directory and streams the zip to the browser.
     * The browser will download it as "logs_<deviceName>.zip"
     */
    @GetMapping("/logs/{deviceName}/download")
    public void downloadLogsAsZip(@PathVariable String deviceName, HttpServletResponse response) throws IOException {
        // Logs are pulled to: C:/AdbToolkit/Logs/logs/
        File logsDir = new File(storagePaths.logsDir(), "logs");
        if (!logsDir.exists() || !logsDir.isDirectory()) {
            // Try the root logs dir as fallback
            logsDir = storagePaths.logsDir();
        }

        File[] logFiles = collectLogFiles(logsDir);
        if (logFiles == null || logFiles.length == 0) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("No log files found");
            return;
        }

        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"logs_" + deviceName + ".zip\"");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            for (File logFile : logFiles) {
                addFileToZip(zipOut, logFile, logFile.getName());
            }
            // Also include any subdirectory log files
            File[] subDirs = logsDir.listFiles(File::isDirectory);
            if (subDirs != null) {
                for (File subDir : subDirs) {
                    File[] subFiles = collectLogFiles(subDir);
                    if (subFiles != null) {
                        for (File subFile : subFiles) {
                            addFileToZip(zipOut, subFile, subDir.getName() + "/" + subFile.getName());
                        }
                    }
                }
            }
        }
    }

    private File[] collectLogFiles(File directory) {
        if (directory == null || !directory.exists()) {
            return null;
        }
        return directory.listFiles((dir, name) -> name.endsWith(".log") || name.endsWith(".txt"));
    }

    private void addFileToZip(ZipOutputStream zipOut, File file, String entryName) throws IOException {
        zipOut.putNextEntry(new ZipEntry(entryName));
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                zipOut.write(buffer, 0, bytesRead);
            }
        }
        zipOut.closeEntry();
    }
}
