package androidtoolkit.backend.service;

import androidtoolkit.app.AppServices;
import androidtoolkit.app.LogExportManager;
import androidtoolkit.app.LogExportResponse;
import androidtoolkit.backend.dto.LogCollectionResponse;
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
    private final LogUploadMetadataRepository metadataRepository;
    private final ProjectRepository projectRepository;
    private final DeviceGateway deviceGateway;
    private final PackageClassifier packageClassifier;

    public LogCollectionService(
            LogExportManager logExportManager,
            AppServices appServices,
            ZipArchiveService zipArchiveService,
            SharedStorageService sharedStorageService,
            LogUploadMetadataRepository metadataRepository,
            ProjectRepository projectRepository,
            DeviceGateway deviceGateway,
            PackageClassifier packageClassifier
    ) {
        this.logExportManager = logExportManager;
        this.appServices = appServices;
        this.zipArchiveService = zipArchiveService;
        this.sharedStorageService = sharedStorageService;
        this.metadataRepository = metadataRepository;
        this.projectRepository = projectRepository;
        this.deviceGateway = deviceGateway;
        this.packageClassifier = packageClassifier;
    }

    /**
     * Pulls logs from device, saves locally, zips, and uploads to shared storage.
     * Returns immediately after local save; upload happens async.
     */
    public LogCollectionResponse collectLogs(String serial, Long projectId) {
        // 1. Local save via existing LogExportManager
        String targetFolder = appServices.storagePaths().logsDir().getPath();
        LogExportResponse exportResponse = logExportManager.exportDeviceLogs(serial, serial, targetFolder);

        // 2. Determine project association
        Project project = resolveProject(serial, projectId);

        // 3. Trigger async ZIP + upload if project has shared storage configured
        if (project != null && project.getSharedLogStoragePath() != null
                && !project.getSharedLogStoragePath().isBlank()) {
            if (sharedStorageService.isAccessible(project.getSharedLogStoragePath())) {
                uploadAsync(exportResponse.getExportedLogsFolder(), project, serial);
            } else {
                log.warn("Shared storage unreachable for project '{}': {}",
                        project.getName(), project.getSharedLogStoragePath());
            }
        } else if (project != null) {
            log.info("Project '{}' has no shared log storage path configured, skipping upload",
                    project.getName());
        } else {
            log.info("No project association found for device {}, skipping shared upload", serial);
        }

        // 4. Return response immediately
        return new LogCollectionResponse(
                exportResponse.getSelectedFolder(),
                exportResponse.getExportedLogsFolder(),
                exportResponse.getMessage(),
                project != null ? project.getSharedLogStoragePath() : null,
                project != null ? project.getId() : null
        );
    }

    /**
     * Asynchronously creates ZIP archive and uploads to shared storage.
     */
    @Async
    public void uploadAsync(String exportedLogsFolder, Project project, String serial) {
        try {
            Path sourceDir = Path.of(exportedLogsFolder);
            if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
                log.warn("Exported logs folder does not exist: {}", exportedLogsFolder);
                return;
            }

            // Create ZIP archive
            String date = LocalDate.now().toString();
            String time = LocalTime.now().format(TIME_FORMATTER);
            String archiveName = "logs_" + time + ".zip";

            Path zipFile = zipArchiveService.createArchive(sourceDir, archiveName);

            // Upload to shared storage
            Optional<String> uploadedPath = sharedStorageService.upload(
                    zipFile,
                    project.getSharedLogStoragePath(),
                    project.getName(),
                    date,
                    serial
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
                // For now, search all projects and match by name heuristic
                return projectRepository.findAll().stream()
                        .filter(p -> packageMatchesProject(detectedPackage, p))
                        .findFirst()
                        .orElse(null);
            }
        } catch (Exception e) {
            log.warn("Failed to detect project from device packages for serial {}: {}",
                    serial, e.getMessage());
        }

        return null;
    }

    /**
     * Heuristic to match a detected package name to a project.
     */
    private boolean packageMatchesProject(String packageName, Project project) {
        String projectNameLower = project.getName().toLowerCase();
        String packageLower = packageName.toLowerCase();
        return packageLower.contains(projectNameLower) || projectNameLower.contains(packageLower);
    }
}
