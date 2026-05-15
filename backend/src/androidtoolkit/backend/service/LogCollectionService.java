package androidtoolkit.backend.service;

import androidtoolkit.app.AppServices;
import androidtoolkit.app.LogExportManager;
import androidtoolkit.app.LogExportResponse;
import androidtoolkit.backend.dto.LogCollectionResponse;
import androidtoolkit.backend.dto.LogcatData;
import androidtoolkit.backend.entity.LogUploadMetadata;
import androidtoolkit.backend.entity.Project;
import androidtoolkit.backend.repository.LogUploadMetadataRepository;
import androidtoolkit.backend.repository.ProjectRepository;
import androidtoolkit.service.DeviceGateway;
import androidtoolkit.service.PackageClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Orchestrates log collection: local save, ZIP archiving, and shared storage upload.
 * Wraps the existing LogExportManager without modifying it.
 */
@Service
public class LogCollectionService {

    private static final Logger log = LoggerFactory.getLogger(LogCollectionService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH-mm-ss");

    private final LogExportManager logExportManager;
    private final AppServices appServices;
    private final ZipArchiveService zipArchiveService;
    private final SharedStorageService sharedStorageService;
    private final ObjectStorageService objectStorageService;
    private final LogUploadMetadataRepository metadataRepository;
    private final ProjectRepository projectRepository;
    private final DeviceGateway deviceGateway;
    private final PackageClassifier packageClassifier;
    private final LogcatStreamManager logcatStreamManager;

    public LogCollectionService(
            LogExportManager logExportManager,
            AppServices appServices,
            ZipArchiveService zipArchiveService,
            SharedStorageService sharedStorageService,
            ObjectStorageService objectStorageService,
            LogUploadMetadataRepository metadataRepository,
            ProjectRepository projectRepository,
            DeviceGateway deviceGateway,
            PackageClassifier packageClassifier,
            LogcatStreamManager logcatStreamManager
    ) {
        this.logExportManager = logExportManager;
        this.appServices = appServices;
        this.zipArchiveService = zipArchiveService;
        this.sharedStorageService = sharedStorageService;
        this.objectStorageService = objectStorageService;
        this.metadataRepository = metadataRepository;
        this.projectRepository = projectRepository;
        this.deviceGateway = deviceGateway;
        this.packageClassifier = packageClassifier;
        this.logcatStreamManager = logcatStreamManager;
    }

    /**
     * Pulls logs from device, saves locally, zips, and uploads to shared storage.
     * Returns immediately after local save; upload happens async.
     *
     * Local structure: {logsDir}/{flavor}/{deviceName}_{serial}/{date}/logs/
     * Remote structure: {basePath}/{flavor}/{deviceName}_{serial}/{date}/{archiveName}
     */
    public LogCollectionResponse collectLogs(String serial, Long projectId) {
        // 1. Resolve device info for folder structure
        String packageName = deviceGateway.getSafePathPackage(serial);
        String flavor = resolveFlavorName(packageName);
        String deviceName = resolveDeviceName(serial);
        String date = LocalDate.now().toString();

        // 2. Build local target folder: {logsDir}/{flavor}/{deviceName}_{serial}/{date}
        String baseLogsDir = appServices.storagePaths().logsDir().getPath();
        Path localTargetDir = Path.of(baseLogsDir)
                .resolve(flavor)
                .resolve(deviceName + "_" + sanitizeSerial(serial))
                .resolve(date);
        try {
            Files.createDirectories(localTargetDir);
        } catch (IOException e) {
            log.warn("Failed to create local log directory: {}", localTargetDir, e);
        }

        // 3. Local save via existing LogExportManager
        String targetFolder = localTargetDir.toString();
        LogExportResponse exportResponse = logExportManager.exportDeviceLogs(serial, deviceName, targetFolder);

        // 4. Rename pulled log files to include metadata: {model}_{serial}_{type}_{date}.log
        renameLogFiles(Path.of(exportResponse.getExportedLogsFolder()), deviceName, serial, date);

        // 5. Determine project association
        Project project = resolveProject(serial, projectId);

        // 6. Trigger async ZIP + upload if project has shared storage configured
        if (project != null && project.getSharedLogStoragePath() != null
                && !project.getSharedLogStoragePath().isBlank()) {
            if (sharedStorageService.isAccessible(project.getSharedLogStoragePath())) {
                uploadAsync(exportResponse.getExportedLogsFolder(), project, serial, flavor, deviceName);
            } else {
                log.warn("Shared storage unreachable for project '{}': {}",
                        project.getName(), project.getSharedLogStoragePath());
                // Still upload to object storage even if shared storage is unreachable
                uploadToObjectStorageAsync(exportResponse.getExportedLogsFolder(), project, serial, flavor, deviceName);
            }
        } else if (project != null) {
            log.info("Project '{}' has no shared log storage path configured, uploading to object storage only",
                    project.getName());
            uploadToObjectStorageAsync(exportResponse.getExportedLogsFolder(), project, serial, flavor, deviceName);
        } else {
            log.info("No project association found for device {}, skipping upload", serial);
        }

        // 6. Return response immediately
        return new LogCollectionResponse(
                exportResponse.getSelectedFolder(),
                exportResponse.getExportedLogsFolder(),
                exportResponse.getMessage(),
                project != null ? project.getSharedLogStoragePath() : null,
                project != null ? project.getId() : null
        );
    }

    /**
     * Asynchronously creates ZIP archive and uploads ONLY to Object Storage (MinIO/S3).
     * Used when shared network storage is not configured or unreachable.
     */
    @Async
    public void uploadToObjectStorageAsync(String exportedLogsFolder, Project project, String serial,
                                           String flavor, String deviceName) {
        try {
            Path sourceDir = Path.of(exportedLogsFolder);
            if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
                log.warn("Exported logs folder does not exist: {}", exportedLogsFolder);
                return;
            }

            String date = LocalDate.now().toString();
            String time = LocalTime.now().format(TIME_FORMATTER);
            String tokenType = resolveTokenType(serial);

            StringBuilder archiveBuilder = new StringBuilder();
            if (!"Device".equals(deviceName)) archiveBuilder.append(deviceName).append("_");
            archiveBuilder.append(sanitizeSerial(serial));
            if (!"unknown".equals(tokenType)) archiveBuilder.append("_").append(tokenType);
            archiveBuilder.append("_").append(date).append("_").append(time).append(".zip");
            String archiveName = archiveBuilder.toString();

            Path zipFile = zipArchiveService.createArchive(sourceDir, archiveName);

            long fileSize = Files.size(zipFile);
            String objectKey = objectStorageService.upload(
                    1L, // default tenant
                    project.getName(),
                    archiveName,
                    Files.newInputStream(zipFile),
                    fileSize
            );
            log.info("Uploaded to object storage: {}", objectKey);

            // Persist metadata
            LogUploadMetadata metadata = new LogUploadMetadata(
                    project, serial, Instant.now(), objectKey, fileSize, LocalDate.now()
            );
            metadataRepository.save(metadata);

            Files.deleteIfExists(zipFile);
        } catch (Exception e) {
            log.error("Failed to upload to object storage for device {}: {}", serial, e.getMessage(), e);
        }
    }

    /**
     * Asynchronously creates ZIP archive and uploads to shared storage.
     * Remote structure: {basePath}/{flavor}/{deviceName}_{serial}/{date}/{archiveName}
     */
    @Async
    public void uploadAsync(String exportedLogsFolder, Project project, String serial,
                            String flavor, String deviceName) {
        try {
            Path sourceDir = Path.of(exportedLogsFolder);
            if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
                log.warn("Exported logs folder does not exist: {}", exportedLogsFolder);
                return;
            }

            // Create ZIP archive with descriptive name: {model}_{serial}_{type}_{date}.zip
            String date = LocalDate.now().toString();
            String time = LocalTime.now().format(TIME_FORMATTER);
            String tokenType = resolveTokenType(serial);

            // Build archive name, skipping unknown parts
            StringBuilder archiveBuilder = new StringBuilder();
            if (!"Device".equals(deviceName)) archiveBuilder.append(deviceName).append("_");
            archiveBuilder.append(sanitizeSerial(serial));
            if (!"unknown".equals(tokenType)) archiveBuilder.append("_").append(tokenType);
            archiveBuilder.append("_").append(date).append("_").append(time).append(".zip");
            String archiveName = archiveBuilder.toString();

            Path zipFile = zipArchiveService.createArchive(sourceDir, archiveName);

            // Upload to shared storage with new structure: {basePath}/{flavor}/{deviceName}_{serial}/{date}/
            String deviceFolder = deviceName + "_" + sanitizeSerial(serial);
            Optional<String> uploadedPath = sharedStorageService.upload(
                    zipFile,
                    project.getSharedLogStoragePath(),
                    flavor,
                    date,
                    deviceFolder
            );

            // Persist metadata on successful upload
            if (uploadedPath.isPresent()) {
                long fileSize = Files.size(zipFile);
                LogUploadMetadata metadata = new LogUploadMetadata(
                        project,
                        serial,
                        Instant.now(),
                        uploadedPath.get(),
                        fileSize,
                        LocalDate.now()
                );
                metadataRepository.save(metadata);
                log.info("Upload metadata saved for device {} project '{}'", serial, project.getName());
            }

            // Also upload to Object Storage (MinIO/S3) for cloud backup
            try {
                long fileSize = Files.size(zipFile);
                String objectKey = objectStorageService.upload(
                        1L, // default tenant for standalone/dev
                        project.getName(),
                        archiveName,
                        Files.newInputStream(zipFile),
                        fileSize
                );
                log.info("Uploaded to object storage: {}", objectKey);
            } catch (Exception e) {
                log.warn("Object storage upload failed (non-fatal): {}", e.getMessage());
            }

            // Clean up local ZIP file
            Files.deleteIfExists(zipFile);

        } catch (IOException e) {
            log.error("Failed to create ZIP archive or upload for device {}: {}", serial, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during async upload for device {}: {}", serial, e.getMessage(), e);
        }
    }

    /**
     * Resolves the project for this log pull.
     * Uses provided projectId first, then tries to detect from installed packages.
     * Falls back to a "Default" project so files always get uploaded to Object Storage.
     */
    private Project resolveProject(String serial, Long projectId) {
        // If projectId is explicitly provided, use it
        if (projectId != null) {
            return projectRepository.findById(projectId).orElse(null);
        }

        // Try to detect from installed SafePath packages on the device
        try {
            ArrayList<String> installedPackages = deviceGateway.getInstalledPackages(serial);
            String detectedPackage = packageClassifier.detectSafePathPackage(installedPackages);

            if (!detectedPackage.isEmpty()) {
                // Try to find a project that matches this package
                Project matched = projectRepository.findAll().stream()
                        .filter(p -> packageMatchesProject(detectedPackage, p))
                        .findFirst()
                        .orElse(null);
                if (matched != null) return matched;
            }
        } catch (Exception e) {
            log.warn("Failed to detect project from device packages for serial {}: {}",
                    serial, e.getMessage());
        }

        // Fall back to "Default" project — create it if it doesn't exist
        return projectRepository.findByName("Default")
                .orElseGet(() -> {
                    Project defaultProject = new Project("Default", "", "");
                    projectRepository.save(defaultProject);
                    log.info("Created 'Default' project for unassociated log uploads");
                    return defaultProject;
                });
    }

    /**
     * Heuristic to match a detected package name to a project.
     */
    private boolean packageMatchesProject(String packageName, Project project) {
        String projectNameLower = project.getName().toLowerCase();
        String packageLower = packageName.toLowerCase();
        return packageLower.contains(projectNameLower) || projectNameLower.contains(packageLower);
    }

    /**
     * Maps a package name to a human-readable flavor name for folder organization.
     */
    private String resolveFlavorName(String packageName) {
        if (packageName == null || packageName.isEmpty()) return "Unknown";
        return switch (packageName) {
            case "com.smithmicro.safepath.family", "com.smithmicro.safepath.family.child" -> "SPFamily";
            case "com.smithmicro.safepath.family.light", "com.smithmicro.safepath.family.speakeasy" -> "SPFamily-Light";
            case "com.smithmicro.cci.test" -> "SpeakEasy";
            case "com.smithmicro.att.securefamily", "com.wavemarket.waplauncher", "com.att.securefamilycompanion" -> "SecureFamily";
            case "com.smithmicro.tmobile.familymode.test", "com.tmobile.familycontrols" -> "FamilyMode";
            case "com.smithmicro.sprint.safeandfound.test", "com.sprint.safefound" -> "SafeAndFound";
            case "com.smithmicro.safepath.dish.test", "com.smithmicro.safepath.dish.kid.test" -> "Dish";
            case "com.smithmicro.orangespain.test", "com.orange.es.TuYo" -> "TuYo";
            default -> packageName.substring(packageName.lastIndexOf('.') + 1);
        };
    }

    /**
     * Gets a human-readable device name from the device properties.
     */
    private String resolveDeviceName(String serial) {
        try {
            String model = deviceGateway.getDeviceModel(serial);
            if (model != null && !model.isBlank()) {
                return model.replace(" ", "_");
            }
        } catch (Exception e) {
            log.debug("Failed to get device model for {}: {}", serial, e.getMessage());
        }
        return "Device";
    }

    /**
     * Sanitizes a device serial for use in folder names (replaces colons and dots).
     */
    private String sanitizeSerial(String serial) {
        return serial.replace(":", "-").replace(".", "_");
    }

    /**
     * Renames log files in the exported folder to include device metadata.
     * Format: {model}_{serial}_{type}_{date}.log
     */
    private void renameLogFiles(Path exportedDir, String deviceName, String serial, String date) {
        if (!Files.exists(exportedDir) || !Files.isDirectory(exportedDir)) return;
        String tokenType = resolveTokenType(serial);

        // Build prefix, skipping unknown parts
        StringBuilder prefix = new StringBuilder();
        if (!"Device".equals(deviceName)) prefix.append(deviceName).append("_");
        prefix.append(sanitizeSerial(serial));
        if (!"unknown".equals(tokenType)) prefix.append("_").append(tokenType);
        prefix.append("_").append(date);

        String finalPrefix = prefix.toString();
        try (var files = Files.list(exportedDir)) {
            files.filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().endsWith(".log"))
                    .forEach(f -> {
                        try {
                            String originalName = f.getFileName().toString();
                            // Strip existing prefix if already applied (prevents duplication)
                            if (originalName.startsWith(finalPrefix + "_")) {
                                originalName = originalName.substring(finalPrefix.length() + 1);
                            }
                            String newName = finalPrefix + "_" + originalName;
                            Files.move(f, f.resolveSibling(newName), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            log.warn("Failed to rename log file {}: {}", f, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to list log files in {}: {}", exportedDir, e.getMessage());
        }
    }

    /**
     * Resolves the token type from the logcat stream data for the given device.
     * Maps raw JWT types to friendly names: godevice->child, admin->adult.
     * Returns "unknown" if no token type is available.
     */
    private String resolveTokenType(String serial) {
        LogcatData data = logcatStreamManager.getCurrentData(serial);
        if (data != null && data.getTokenType() != null && !data.getTokenType().isBlank()) {
            String rawType = data.getTokenType();
            String packageName = deviceGateway.getSafePathPackage(serial);
            return switch (rawType) {
                case "godevice" -> "com.smithmicro.cci.test".equals(packageName) ? "senior" : "child";
                case "admin" -> "adult";
                default -> rawType;
            };
        }
        return "unknown";
    }
}
