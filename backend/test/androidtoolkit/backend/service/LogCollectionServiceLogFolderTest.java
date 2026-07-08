package androidtoolkit.backend.service;

import androidtoolkit.app.AppServices;
import androidtoolkit.app.LogExportManager;
import androidtoolkit.backend.dto.ResolvedProjectResponse;
import androidtoolkit.backend.entity.Project;
import androidtoolkit.backend.entity.User;
import androidtoolkit.backend.repository.LogUploadMetadataRepository;
import androidtoolkit.backend.repository.ProjectRepository;
import androidtoolkit.service.DeviceGateway;
import androidtoolkit.service.PackageClassifier;
import androidtoolkit.service.StoragePaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LogCollectionService.resolveBaseLogsDir() method.
 * Validates Requirements 8.1, 8.2, 8.3, 8.4.
 */
class LogCollectionServiceLogFolderTest {

    private LogExportManager logExportManager;
    private AppServices appServices;
    private ZipArchiveService zipArchiveService;
    private SharedStorageService sharedStorageService;
    private ObjectStorageService objectStorageService;
    private LogUploadMetadataRepository metadataRepository;
    private ProjectRepository projectRepository;
    private ProjectService projectService;
    private DeviceGateway deviceGateway;
    private PackageClassifier packageClassifier;
    private LogcatStreamManager logcatStreamManager;

    private LogCollectionService service;

    private static final String APP_DEFAULT_LOGS_DIR = "C:/AdbToolkit/Logs";

    @BeforeEach
    void setUp() {
        logExportManager = mock(LogExportManager.class);
        appServices = mock(AppServices.class);
        zipArchiveService = mock(ZipArchiveService.class);
        sharedStorageService = mock(SharedStorageService.class);
        objectStorageService = mock(ObjectStorageService.class);
        metadataRepository = mock(LogUploadMetadataRepository.class);
        projectRepository = mock(ProjectRepository.class);
        projectService = mock(ProjectService.class);
        deviceGateway = mock(DeviceGateway.class);
        packageClassifier = mock(PackageClassifier.class);
        logcatStreamManager = mock(LogcatStreamManager.class);

        // Set up the default app logs directory
        StoragePaths storagePaths = mock(StoragePaths.class);
        when(appServices.storagePaths()).thenReturn(storagePaths);
        when(storagePaths.logsDir()).thenReturn(new File(APP_DEFAULT_LOGS_DIR));

        service = new LogCollectionService(
                logExportManager,
                appServices,
                zipArchiveService,
                sharedStorageService,
                objectStorageService,
                metadataRepository,
                projectRepository,
                projectService,
                deviceGateway,
                packageClassifier,
                logcatStreamManager
        );
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private Project createProject(Long id, String name) {
        Project project = new Project(name, "remote/path", "/apk/folder", "/default/logs");
        // Use reflection to set the id since it's generated
        try {
            var idField = Project.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(project, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return project;
    }

    private User createUser(String username) {
        return new User(username, username + "@test.com", "hash");
    }

    // ─── Test: User override is used as base directory (Req 8.1) ─────────────────

    @Test
    @DisplayName("resolveBaseLogsDir returns user override path when override exists")
    void resolveBaseLogsDir_returnsUserOverridePath() {
        Project project = createProject(1L, "TestProject");
        User user = createUser("testuser");

        ResolvedProjectResponse resolved = new ResolvedProjectResponse(
                1L, "TestProject", "remote/path",
                "/apk/folder", true,
                "/custom/logs/override", true, null, null, null, null
        );
        when(projectService.getResolved(1L, user)).thenReturn(resolved);

        String result = service.resolveBaseLogsDir(project, user);

        assertEquals("/custom/logs/override", result);
    }

    // ─── Test: Project default is used when no override (Req 8.1) ────────────────

    @Test
    @DisplayName("resolveBaseLogsDir returns project default when no user override exists")
    void resolveBaseLogsDir_returnsProjectDefault_whenNoOverride() {
        Project project = createProject(1L, "TestProject");
        User user = createUser("testuser");

        ResolvedProjectResponse resolved = new ResolvedProjectResponse(
                1L, "TestProject", "remote/path",
                "/apk/folder", false,
                "/custom/logs", false, null, null, null, null
        );
        when(projectService.getResolved(1L, user)).thenReturn(resolved);

        String result = service.resolveBaseLogsDir(project, user);

        assertEquals("/custom/logs", result);
    }

    // ─── Test: Fallback when project is null (Req 8.3) ───────────────────────────

    @Test
    @DisplayName("resolveBaseLogsDir returns app default when project is null")
    void resolveBaseLogsDir_returnsAppDefault_whenProjectIsNull() {
        User user = createUser("testuser");

        String result = service.resolveBaseLogsDir(null, user);

        assertEquals(APP_DEFAULT_LOGS_DIR, result);
        verifyNoInteractions(projectService);
    }

    // ─── Test: Fallback when user is null (Req 8.3) ──────────────────────────────

    @Test
    @DisplayName("resolveBaseLogsDir returns app default when user is null")
    void resolveBaseLogsDir_returnsAppDefault_whenUserIsNull() {
        Project project = createProject(1L, "TestProject");

        String result = service.resolveBaseLogsDir(project, null);

        assertEquals(APP_DEFAULT_LOGS_DIR, result);
        verifyNoInteractions(projectService);
    }

    // ─── Test: Fallback when resolved path is empty string (Req 8.2) ─────────────

    @Test
    @DisplayName("resolveBaseLogsDir returns app default when resolved path is empty string")
    void resolveBaseLogsDir_returnsAppDefault_whenResolvedPathIsEmpty() {
        Project project = createProject(1L, "TestProject");
        User user = createUser("testuser");

        ResolvedProjectResponse resolved = new ResolvedProjectResponse(
                1L, "TestProject", "remote/path",
                "/apk/folder", false,
                "", false, null, null, null, null
        );
        when(projectService.getResolved(1L, user)).thenReturn(resolved);

        String result = service.resolveBaseLogsDir(project, user);

        assertEquals(APP_DEFAULT_LOGS_DIR, result);
    }

    // ─── Test: Fallback when resolved path is blank (Req 8.2) ────────────────────

    @Test
    @DisplayName("resolveBaseLogsDir returns app default when resolved path is blank")
    void resolveBaseLogsDir_returnsAppDefault_whenResolvedPathIsBlank() {
        Project project = createProject(1L, "TestProject");
        User user = createUser("testuser");

        ResolvedProjectResponse resolved = new ResolvedProjectResponse(
                1L, "TestProject", "remote/path",
                "/apk/folder", false,
                "   ", false, null, null, null, null
        );
        when(projectService.getResolved(1L, user)).thenReturn(resolved);

        String result = service.resolveBaseLogsDir(project, user);

        assertEquals(APP_DEFAULT_LOGS_DIR, result);
    }

    // ─── Test: Fallback when resolved path is null (Req 8.2) ─────────────────────

    @Test
    @DisplayName("resolveBaseLogsDir returns app default when resolved path is null")
    void resolveBaseLogsDir_returnsAppDefault_whenResolvedPathIsNull() {
        Project project = createProject(1L, "TestProject");
        User user = createUser("testuser");

        ResolvedProjectResponse resolved = new ResolvedProjectResponse(
                1L, "TestProject", "remote/path",
                "/apk/folder", false,
                null, false, null, null, null, null
        );
        when(projectService.getResolved(1L, user)).thenReturn(resolved);

        String result = service.resolveBaseLogsDir(project, user);

        assertEquals(APP_DEFAULT_LOGS_DIR, result);
    }

    // ─── Test: Fallback when projectService throws exception (Req 8.2) ───────────

    @Test
    @DisplayName("resolveBaseLogsDir returns app default when projectService throws exception")
    void resolveBaseLogsDir_returnsAppDefault_whenProjectServiceThrows() {
        Project project = createProject(1L, "TestProject");
        User user = createUser("testuser");

        when(projectService.getResolved(1L, user)).thenThrow(new RuntimeException("DB error"));

        String result = service.resolveBaseLogsDir(project, user);

        assertEquals(APP_DEFAULT_LOGS_DIR, result);
    }
}
