package androidtoolkit.backend.service;

import androidtoolkit.app.AppServices;
import androidtoolkit.backend.dto.ResolvedProjectResponse;
import androidtoolkit.backend.entity.Project;
import androidtoolkit.backend.entity.User;
import androidtoolkit.service.StoragePaths;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for log collection path resolution in {@link LogCollectionService}.
 * Tests the resolveBaseLogsDir method and subdirectory structure preservation.
 */
class LogCollectionPathProperties {

    private ProjectService projectService;
    private AppServices appServices;
    private StoragePaths storagePaths;
    private LogCollectionService logCollectionService;

    private static final File APP_DEFAULT_LOGS_DIR_FILE = new File("C:/AdbToolkit/Logs");
    private static final String APP_DEFAULT_LOGS_DIR = APP_DEFAULT_LOGS_DIR_FILE.getPath();

    @BeforeProperty
    void setUp() {
        projectService = mock(ProjectService.class);
        appServices = mock(AppServices.class);
        storagePaths = mock(StoragePaths.class);

        when(appServices.storagePaths()).thenReturn(storagePaths);
        when(storagePaths.logsDir()).thenReturn(APP_DEFAULT_LOGS_DIR_FILE);

        logCollectionService = new LogCollectionService(
                null, // logExportManager - not needed for resolveBaseLogsDir
                appServices,
                null, // zipArchiveService
                null, // sharedStorageService
                null, // objectStorageService
                null, // metadataRepository
                null, // projectRepository
                projectService,
                null, // deviceGateway
                null, // packageClassifier
                null  // logcatStreamManager
        );
    }

    // ─── Property 5: Log Collection Uses Resolved Path ───────────────────────────
    // Feature: default-log-location-setting, Property 5: Log Collection Uses Resolved Path
    // **Validates: Requirements 8.1, 8.2, 8.3**

    @Property(tries = 100)
    void logCollection_usesResolvedPath_withOverride(
            @ForAll("validLogFolderPaths") String overridePath,
            @ForAll("projectNames") String projectName
    ) {
        // Given: a project with a user override that resolves to overridePath
        Project project = new Project(projectName, "remote", "/apk", "/default/logs");
        setProjectId(project, 1L);
        User user = new User("testuser", "test@example.com", "hash");

        ResolvedProjectResponse resolved = new ResolvedProjectResponse(
                1L, projectName, "remote", "/apk", true,
                overridePath, true
        );
        when(projectService.getResolved(1L, user)).thenReturn(resolved);

        // When: resolving the base logs directory
        String result = logCollectionService.resolveBaseLogsDir(project, user);

        // Then: the resolved path (override) is used
        assertEquals(overridePath, result);
    }

    @Property(tries = 100)
    void logCollection_usesResolvedPath_withProjectDefault(
            @ForAll("validLogFolderPaths") String projectLogFolder,
            @ForAll("projectNames") String projectName
    ) {
        // Given: a project with no user override, resolving to project default
        Project project = new Project(projectName, "remote", "/apk", projectLogFolder);
        setProjectId(project, 2L);
        User user = new User("testuser", "test@example.com", "hash");

        ResolvedProjectResponse resolved = new ResolvedProjectResponse(
                2L, projectName, "remote", "/apk", false,
                projectLogFolder, false
        );
        when(projectService.getResolved(2L, user)).thenReturn(resolved);

        // When: resolving the base logs directory
        String result = logCollectionService.resolveBaseLogsDir(project, user);

        // Then: the project default is used
        assertEquals(projectLogFolder, result);
    }

    @Property(tries = 100)
    void logCollection_fallsBackToAppDefault_whenResolvedIsEmpty(
            @ForAll("emptyOrBlankStrings") String emptyLogFolder,
            @ForAll("projectNames") String projectName
    ) {
        // Given: a project whose resolved localLogFolder is empty/blank
        Project project = new Project(projectName, "remote", "/apk", emptyLogFolder);
        setProjectId(project, 3L);
        User user = new User("testuser", "test@example.com", "hash");

        ResolvedProjectResponse resolved = new ResolvedProjectResponse(
                3L, projectName, "remote", "/apk", false,
                emptyLogFolder, false
        );
        when(projectService.getResolved(3L, user)).thenReturn(resolved);

        // When: resolving the base logs directory
        String result = logCollectionService.resolveBaseLogsDir(project, user);

        // Then: falls back to app default
        assertEquals(APP_DEFAULT_LOGS_DIR, result);
    }

    @Property(tries = 100)
    void logCollection_fallsBackToAppDefault_whenNoProject() {
        // Given: no project associated (null project)
        User user = new User("testuser", "test@example.com", "hash");

        // When: resolving the base logs directory with null project
        String result = logCollectionService.resolveBaseLogsDir(null, user);

        // Then: falls back to app default
        assertEquals(APP_DEFAULT_LOGS_DIR, result);
    }

    @Property(tries = 100)
    void logCollection_fallsBackToAppDefault_whenNoUser(
            @ForAll("projectNames") String projectName
    ) {
        // Given: no user (null user)
        Project project = new Project(projectName, "remote", "/apk", "/some/path");
        setProjectId(project, 4L);

        // When: resolving the base logs directory with null user
        String result = logCollectionService.resolveBaseLogsDir(project, null);

        // Then: falls back to app default
        assertEquals(APP_DEFAULT_LOGS_DIR, result);
    }

    // ─── Property 6: Subdirectory Structure Preservation ─────────────────────────
    // Feature: default-log-location-setting, Property 6: Subdirectory Structure Preservation
    // **Validates: Requirements 8.5**

    @Property(tries = 100)
    void logCollection_preservesSubdirectoryStructure(
            @ForAll("validLogFolderPaths") String baseDir,
            @ForAll("flavorNames") String flavor,
            @ForAll("deviceNames") String deviceName,
            @ForAll("serialNumbers") String serial,
            @ForAll("dateStrings") String date
    ) {
        // Given: any resolved base directory, flavor, device name, serial, and date
        // When: constructing the local target directory path (mirrors collectLogs logic)
        String sanitizedSerial = serial.replace(":", "-").replace(".", "_");
        Path localTargetDir = Path.of(baseDir)
                .resolve(flavor)
                .resolve(deviceName + "_" + sanitizedSerial)
                .resolve(date);

        // Then: the path structure is {baseDir}/{flavor}/{deviceName}_{serial}/{date}/
        String expectedPath = Path.of(baseDir, flavor, deviceName + "_" + sanitizedSerial, date).toString();
        assertEquals(expectedPath, localTargetDir.toString());

        // And: the path starts with the base directory
        assertTrue(localTargetDir.startsWith(Path.of(baseDir)),
                "Path should start with base directory: " + baseDir);

        // And: the path contains all expected segments in order
        String pathStr = localTargetDir.toString();
        int flavorIdx = pathStr.indexOf(flavor);
        int deviceIdx = pathStr.indexOf(deviceName + "_" + sanitizedSerial);
        int dateIdx = pathStr.lastIndexOf(date);
        assertTrue(flavorIdx > 0, "Path should contain flavor");
        assertTrue(deviceIdx > flavorIdx, "Device segment should come after flavor");
        assertTrue(dateIdx > deviceIdx, "Date segment should come after device");
    }

    // ─── Generators ──────────────────────────────────────────────────────────────

    @Provide
    Arbitrary<String> validLogFolderPaths() {
        // Generate valid non-blank folder paths
        Arbitrary<String> segment = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars('-', '_')
                .ofMinLength(1)
                .ofMaxLength(15);

        return segment.list().ofMinSize(2).ofMaxSize(5)
                .map(segments -> "/" + String.join("/", segments));
    }

    @Provide
    Arbitrary<String> projectNames() {
        return Arbitraries.strings()
                .withCharRange('A', 'Z')
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .ofMinLength(3)
                .ofMaxLength(20);
    }

    @Provide
    Arbitrary<String> emptyOrBlankStrings() {
        return Arbitraries.of("", " ", "  ", "\t", " \t ", "\n");
    }

    @Provide
    Arbitrary<String> flavorNames() {
        return Arbitraries.of(
                "SPFamily", "SPFamily-Light", "SecureFamily",
                "FamilyMode", "SafeAndFound", "Unknown", "Dish"
        );
    }

    @Provide
    Arbitrary<String> deviceNames() {
        // Device names with underscores replacing spaces (as resolveDeviceName does)
        Arbitrary<String> brand = Arbitraries.of("Pixel", "Samsung", "OnePlus", "Motorola");
        Arbitrary<String> model = Arbitraries.of("6", "7_Pro", "S23", "Edge_40");
        return Combinators.combine(brand, model).as((b, m) -> b + "_" + m);
    }

    @Provide
    Arbitrary<String> serialNumbers() {
        // Generate serial numbers like "9A191FFAZ004H2" or "192.168.1.1:5555"
        Arbitrary<String> usbSerial = Arbitraries.strings()
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .ofMinLength(8)
                .ofMaxLength(16);

        Arbitrary<String> tcpSerial = Arbitraries.integers().between(1, 254)
                .list().ofSize(4)
                .map(parts -> parts.get(0) + "." + parts.get(1) + "." + parts.get(2) + "." + parts.get(3) + ":5555");

        return Arbitraries.oneOf(usbSerial, tcpSerial);
    }

    @Provide
    Arbitrary<String> dateStrings() {
        Arbitrary<Integer> year = Arbitraries.integers().between(2024, 2030);
        Arbitrary<Integer> month = Arbitraries.integers().between(1, 12);
        Arbitrary<Integer> day = Arbitraries.integers().between(1, 28);
        return Combinators.combine(year, month, day)
                .as((y, m, d) -> String.format("%d-%02d-%02d", y, m, d));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    /**
     * Sets the id field on a Project entity via reflection (since there's no setter).
     */
    private void setProjectId(Project project, Long id) {
        try {
            var field = Project.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(project, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set project id", e);
        }
    }
}
