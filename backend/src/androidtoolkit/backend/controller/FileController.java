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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
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
     * Opens a folder in Windows Explorer on the local machine.
     */
    @GetMapping("/recording/download")
    public void downloadRecordingFile(
            @RequestParam("path") String recordingPath,
            @RequestParam("fileName") String fileName,
            HttpServletResponse response
    ) throws IOException {
        File file = new File(recordingPath, fileName);
        if (!file.exists() || !file.isFile()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("File not found: " + fileName);
            return;
        }

        String contentType = fileName.endsWith(".mp4") ? "video/mp4" : "application/octet-stream";
        response.setContentType(contentType);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
        response.setContentLengthLong(file.length());

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                response.getOutputStream().write(buffer, 0, bytesRead);
            }
        }
    }

    @PostMapping("/open-folder")
    public Map<String, Object> openFolder(@RequestParam("path") String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            return Map.of("success", false, "message", "Folder not found: " + folderPath);
        }
        try {
            Runtime.getRuntime().exec(new String[]{"explorer.exe", folder.getAbsolutePath()});
            return Map.of("success", true, "message", "Opened: " + folderPath);
        } catch (IOException e) {
            return Map.of("success", false, "message", "Failed to open folder: " + e.getMessage());
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
