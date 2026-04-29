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
import java.nio.file.Path;
import java.util.Map;

import static androidtoolkit.backend.validation.InputValidator.*;

/**
 * REST API for build (APK) operations.
 *
 * Two ways to install:
 * 1. Upload via drag-drop/file picker → server saves to uploads dir → install from there
 * 2. Paste a local path → install directly (server and browser are on the same machine)
 */
@RestController
@RequestMapping("/api/builds")
public class BuildController {

    private final CommandExecutor commandExecutor;
    private final Path uploadsDir;

    public BuildController(AppServices appServices) {
        this.commandExecutor = appServices.commandExecutor();
        this.uploadsDir = Path.of(appServices.storagePaths().rootPath(), "uploads");
        uploadsDir.toFile().mkdirs();
    }

    /**
     * Upload an APK via drag-drop or file picker.
     * Returns the server-side path so the client can use it for install.
     */
    @PostMapping("/upload")
    public Map<String, String> uploadBuild(@RequestParam("file") MultipartFile file) throws IOException {
        String fileName = sanitizeFileName(file.getOriginalFilename());

        File destination = uploadsDir.resolve(fileName).toFile();
        file.transferTo(destination);

        return Map.of(
                "fileName", fileName,
                "path", destination.getAbsolutePath(),
                "message", "Uploaded: " + fileName
        );
    }

    /**
     * Install an APK on a device from a local path.
     */
    @PostMapping("/install/{serial}")
    public Map<String, Object> installOnDevice(
            @PathVariable String serial,
            @RequestParam("path") String apkPath
    ) {
        validateSerial(serial);
        validateFilePath(apkPath);

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
