package androidtoolkit.backend.controller;

import androidtoolkit.service.CommandExecutor;
import androidtoolkit.app.AppServices;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * REST API for build (APK) operations.
 *
 * Test with Postman:
 *   POST http://localhost:8080/api/builds/upload     (form-data: file=<apk>)
 *   POST http://localhost:8080/api/builds/install/{serial}?path=<uploaded path>
 */
@RestController
@RequestMapping("/api/builds")
public class BuildController {

    private final CommandExecutor commandExecutor;
    private final Path uploadsDir;

    public BuildController(AppServices appServices) {
        this.commandExecutor = appServices.commandExecutor();
        // Store uploaded APKs in C:/AdbToolkit/uploads
        this.uploadsDir = Path.of(appServices.storagePaths().rootPath(), "uploads");
        uploadsDir.toFile().mkdirs();
    }

    /**
     * Upload an APK file to the server.
     * Returns the server-side path so the client can use it for install.
     */
    @PostMapping("/upload")
    public Map<String, String> uploadBuild(@RequestParam("file") MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.endsWith(".apk")) {
            throw new IllegalArgumentException("Only .apk files are accepted");
        }

        File destination = uploadsDir.resolve(fileName).toFile();
        file.transferTo(destination);

        return Map.of(
                "fileName", fileName,
                "path", destination.getAbsolutePath(),
                "message", "Uploaded: " + fileName
        );
    }

    /**
     * Install a previously uploaded APK on a specific device.
     */
    @PostMapping("/install/{serial}")
    public Map<String, Object> installOnDevice(
            @PathVariable String serial,
            @RequestParam("path") String apkPath
    ) {
        File apkFile = new File(apkPath);
        if (!apkFile.exists()) {
            return Map.of("success", false, "message", "APK not found: " + apkPath);
        }

        String output = commandExecutor.runCommand("adb -s " + serial + " install \"" + apkPath + "\"");
        boolean success = output.contains("Success");

        return Map.of(
                "success", success,
                "message", success
                        ? "App installed on " + serial
                        : "Install failed on " + serial + ": " + output
        );
    }
}
