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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    @GetMapping("/logs/{deviceName}/download")
    public void downloadLogsAsZip(@PathVariable String deviceName, HttpServletResponse response) throws IOException {
        File logsDir = new File(storagePaths.logsDir(), "logs");
        if (!logsDir.exists() || !logsDir.isDirectory()) {
            logsDir = storagePaths.logsDir();
        }

        File[] logFiles = collectAllFiles(logsDir);
        if (logFiles == null || logFiles.length == 0) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("No log files found");
            return;
        }

        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"logs_" + deviceName + ".zip\"");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            for (File file : logFiles) {
                addFileToZip(zipOut, file, file.getName());
            }
        }
    }

    /**
     * Downloads recording files (video + log) matching the given filename prefix.
     * Only includes the specific recording's files, not old ones from the same folder.
     */
    @GetMapping("/recording/download")
    public void downloadRecording(
            @RequestParam("path") String recordingPath,
            @RequestParam(value = "fileName", required = false) String fileName,
            HttpServletResponse response
    ) throws IOException {
        File recordingDir = new File(recordingPath);
        if (!recordingDir.exists() || !recordingDir.isDirectory()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("Recording folder not found: " + recordingPath);
            return;
        }

        // Only include files that match this recording session
        String prefix = (fileName != null && !fileName.isBlank())
                ? fileName.replace(".mp4", "")
                : null;

        File[] files = recordingDir.listFiles(file -> {
            if (!file.isFile()) return false;
            if (prefix == null) return true;
            return file.getName().startsWith(prefix);
        });

        if (files == null || files.length == 0) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("No recording files found");
            return;
        }

        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"recording_" + recordingDir.getName() + ".zip\"");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            for (File file : files) {
                addFileToZip(zipOut, file, file.getName());
            }
        }
    }

    private File[] collectAllFiles(File directory) {
        if (directory == null || !directory.exists()) {
            return null;
        }
        return directory.listFiles(File::isFile);
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
