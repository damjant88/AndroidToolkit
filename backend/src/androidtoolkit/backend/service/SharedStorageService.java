package androidtoolkit.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Service for uploading and managing log archives on shared storage.
 * Uses java.nio.file operations exclusively, making it agnostic to whether
 * the configured path is a local directory or a mounted network drive (NFS/SMB/EFS/FUSE).
 */
@Service
public class SharedStorageService {

    private static final Logger log = LoggerFactory.getLogger(SharedStorageService.class);

    /**
     * Uploads a file to the shared storage location.
     * Path structure: {basePath}/{projectName}/{date}/{deviceSerial}/{fileName}
     *
     * @param localFile     the local file to upload
     * @param basePath      the root shared storage path (from project's sharedLogStoragePath)
     * @param projectName   the project name for directory organization
     * @param date          the date string in ISO 8601 format (YYYY-MM-DD)
     * @param deviceSerial  the device serial for directory organization
     * @return the full path where the file was stored, or empty if upload failed
     */
    public Optional<String> upload(Path localFile, String basePath, String projectName,
                                   String date, String deviceSerial) {
        if (localFile == null || basePath == null || projectName == null
                || date == null || deviceSerial == null) {
            log.warn("Upload called with null parameters");
            return Optional.empty();
        }

        try {
            String fileName = localFile.getFileName().toString();
            Path targetDir = Path.of(basePath)
                    .resolve(projectName)
                    .resolve(date)
                    .resolve(deviceSerial);

            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(fileName);
            Files.copy(localFile, targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("Uploaded archive to shared storage: {}", targetPath);
            return Optional.of(targetPath.toString());
        } catch (IOException e) {
            log.error("Failed to upload file to shared storage: {}", localFile, e);
            return Optional.empty();
        }
    }

    /**
     * Checks if the shared storage path is accessible (exists, is a directory, and is writable).
     *
     * @param basePath the shared storage base path to check
     * @return true if the path is accessible and writable, false otherwise
     */
    public boolean isAccessible(String basePath) {
        if (basePath == null || basePath.isBlank()) {
            return false;
        }

        Path path = Path.of(basePath);
        return Files.isDirectory(path) && Files.isWritable(path);
    }

    /**
     * Lists ZIP archives in shared storage for a given project within a date range.
     * Traverses the directory structure: {basePath}/{projectName}/{date}/{deviceSerial}/*.zip
     *
     * @param basePath    the root shared storage path
     * @param projectName the project name to filter by
     * @param from        the start date (inclusive)
     * @param to          the end date (inclusive)
     * @return list of paths to ZIP archives within the date range
     */
    public List<Path> listArchives(String basePath, String projectName,
                                   LocalDate from, LocalDate to) {
        List<Path> archives = new ArrayList<>();

        if (basePath == null || projectName == null || from == null || to == null) {
            log.warn("listArchives called with null parameters");
            return archives;
        }

        Path projectDir = Path.of(basePath).resolve(projectName);
        if (!Files.isDirectory(projectDir)) {
            log.debug("Project directory does not exist: {}", projectDir);
            return archives;
        }

        try (Stream<Path> dateDirs = Files.list(projectDir)) {
            dateDirs.filter(Files::isDirectory)
                    .filter(dateDir -> isDateInRange(dateDir.getFileName().toString(), from, to))
                    .forEach(dateDir -> collectArchivesFromDateDir(dateDir, archives));
        } catch (IOException e) {
            log.error("Failed to list archives for project '{}' in: {}", projectName, basePath, e);
        }

        return archives;
    }

    private boolean isDateInRange(String dirName, LocalDate from, LocalDate to) {
        try {
            LocalDate dirDate = LocalDate.parse(dirName, DateTimeFormatter.ISO_LOCAL_DATE);
            return !dirDate.isBefore(from) && !dirDate.isAfter(to);
        } catch (Exception e) {
            log.debug("Skipping non-date directory: {}", dirName);
            return false;
        }
    }

    private void collectArchivesFromDateDir(Path dateDir, List<Path> archives) {
        try (Stream<Path> deviceDirs = Files.list(dateDir)) {
            deviceDirs.filter(Files::isDirectory)
                    .forEach(deviceDir -> collectZipFiles(deviceDir, archives));
        } catch (IOException e) {
            log.error("Failed to list device directories in: {}", dateDir, e);
        }
    }

    private void collectZipFiles(Path deviceDir, List<Path> archives) {
        try (Stream<Path> files = Files.list(deviceDir)) {
            files.filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().endsWith(".zip"))
                    .forEach(archives::add);
        } catch (IOException e) {
            log.error("Failed to list ZIP files in: {}", deviceDir, e);
        }
    }
}
