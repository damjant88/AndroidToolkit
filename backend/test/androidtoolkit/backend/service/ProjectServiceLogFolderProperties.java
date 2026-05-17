package androidtoolkit.backend.service;

import androidtoolkit.backend.dto.OverrideRequest;
import androidtoolkit.backend.dto.OverrideResponse;
import androidtoolkit.backend.dto.ProjectRequest;
import androidtoolkit.backend.dto.ProjectResponse;
import androidtoolkit.backend.dto.ResolvedProjectResponse;
import androidtoolkit.backend.entity.Project;
import androidtoolkit.backend.entity.User;
import androidtoolkit.backend.entity.UserProjectOverride;
import androidtoolkit.backend.repository.ProjectRepository;
import androidtoolkit.backend.repository.UserProjectOverrideRepository;
import net.jqwik.api.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for {@link ProjectService} localLogFolder validation and resolution.
 * Uses jqwik to verify correctness properties across many random inputs.
 *
 * Validates: Requirements 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 3.4, 3.5, 4.1, 4.2
 */
class ProjectServiceLogFolderProperties {

    private ProjectRepository projectRepository;
    private UserProjectOverrideRepository overrideRepository;
    private ProjectService projectService;

    private void setupMocks() {
        projectRepository = mock(ProjectRepository.class);
        overrideRepository = mock(UserProjectOverrideRepository.class);
        projectService = new ProjectService(projectRepository, overrideRepository);
    }

    // ─── Property 1: Persistence Round-Trip ──────────────────────────────────────

    // Feature: default-log-location-setting, Property 1: Persistence Round-Trip
    // **Validates: Requirements 1.2, 1.3**
    @Property(tries = 100)
    void persistenceRoundTrip_localLogFolder(
            @ForAll("validLogFolderStrings") String logFolder
    ) {
        setupMocks();

        // Mock repository behavior: no duplicate name, save returns the entity as-is
        when(projectRepository.existsByName(any())).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project p = invocation.getArgument(0);
            // Simulate ID assignment
            Project saved = new Project(p.getName(), p.getRemoteApkLocation(), p.getLocalApkFolder(), p.getLocalLogFolder());
            try {
                var idField = Project.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(saved, 1L);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return saved;
        });

        ProjectRequest request = new ProjectRequest("TestProject", "/remote/apk", "/local/apk", logFolder);
        ProjectResponse response = projectService.create(request);

        assertEquals(logFolder.trim(), response.localLogFolder(),
                "Created project should return trimmed localLogFolder");
    }

    // Feature: default-log-location-setting, Property 1: Persistence Round-Trip (update)
    // **Validates: Requirements 1.2, 1.3**
    @Property(tries = 100)
    void persistenceRoundTrip_update_localLogFolder(
            @ForAll("validLogFolderStrings") String logFolder
    ) {
        setupMocks();

        // Existing project
        Project existing = new Project("ExistingProject", "/remote", "/local/apk", "/old/log/folder");
        try {
            var idField = Project.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(existing, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(projectRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(projectRepository.findByName(any())).thenReturn(Optional.of(existing));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectRequest request = new ProjectRequest("ExistingProject", "/remote", "/local/apk", logFolder);
        ProjectResponse response = projectService.update(1L, request);

        assertEquals(logFolder.trim(), response.localLogFolder(),
                "Updated project should return trimmed localLogFolder");
    }

    // ─── Property 2: Blank Input Rejection ───────────────────────────────────────

    // Feature: default-log-location-setting, Property 2: Blank Input Rejection
    // **Validates: Requirements 2.1, 2.2, 3.4**
    @Property(tries = 100)
    void blankInput_rejectedForLogFolder_create(
            @ForAll("whitespaceOnlyStrings") String blankInput
    ) {
        setupMocks();

        ProjectRequest request = new ProjectRequest("TestProject", "/remote/apk", "/local/apk", blankInput);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> projectService.create(request));
        assertEquals(400, ex.getStatusCode().value(),
                "Blank localLogFolder should be rejected with 400");
    }

    // Feature: default-log-location-setting, Property 2: Blank Input Rejection (update)
    // **Validates: Requirements 2.1, 2.2, 3.4**
    @Property(tries = 100)
    void blankInput_rejectedForLogFolder_update(
            @ForAll("whitespaceOnlyStrings") String blankInput
    ) {
        setupMocks();

        Project existing = new Project("ExistingProject", "/remote", "/local/apk", "/log/folder");
        try {
            var idField = Project.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(existing, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        when(projectRepository.findById(1L)).thenReturn(Optional.of(existing));

        ProjectRequest request = new ProjectRequest("ExistingProject", "/remote/apk", "/local/apk", blankInput);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> projectService.update(1L, request));
        assertEquals(400, ex.getStatusCode().value(),
                "Blank localLogFolder should be rejected with 400 on update");
    }

    // Feature: default-log-location-setting, Property 2: Blank Input Rejection (override)
    // **Validates: Requirements 2.1, 2.2, 3.4**
    @Property(tries = 100)
    void blankInput_rejectedForLogFolder_setOverride(
            @ForAll("whitespaceOnlyStrings") String blankInput
    ) {
        setupMocks();

        OverrideRequest request = new OverrideRequest("/local/apk", blankInput);
        User user = new User("testuser", "test@example.com", "hash");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> projectService.setOverride(1L, user, request));
        assertEquals(400, ex.getStatusCode().value(),
                "Blank localLogFolder should be rejected with 400 on setOverride");
    }

    // Feature: default-log-location-setting, Property 2: Blank Input Rejection (null)
    // **Validates: Requirements 2.1, 2.2, 3.4**
    @Property(tries = 100)
    void nullInput_rejectedForLogFolder_create() {
        setupMocks();

        ProjectRequest request = new ProjectRequest("TestProject", "/remote/apk", "/local/apk", null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> projectService.create(request));
        assertEquals(400, ex.getStatusCode().value(),
                "Null localLogFolder should be rejected with 400");
    }

    // ─── Property 3: Length Validation ───────────────────────────────────────────

    // Feature: default-log-location-setting, Property 3: Length Validation (project)
    // **Validates: Requirements 2.4, 3.5**
    @Property(tries = 100)
    void overlengthInput_rejectedForLogFolder_project(
            @ForAll("overLengthProjectStrings") String longInput
    ) {
        setupMocks();

        ProjectRequest request = new ProjectRequest("TestProject", "/remote/apk", "/local/apk", longInput);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> projectService.create(request));
        assertEquals(400, ex.getStatusCode().value(),
                "localLogFolder exceeding 1024 chars should be rejected with 400");
    }

    // Feature: default-log-location-setting, Property 3: Length Validation (override)
    // **Validates: Requirements 2.4, 3.5**
    @Property(tries = 100)
    void overlengthInput_rejectedForLogFolder_override(
            @ForAll("overLengthOverrideStrings") String longInput
    ) {
        setupMocks();

        Project existing = new Project("ExistingProject", "/remote", "/local/apk", "/log/folder");
        try {
            var idField = Project.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(existing, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        when(projectRepository.findById(1L)).thenReturn(Optional.of(existing));

        OverrideRequest request = new OverrideRequest("/local/apk", longInput);
        User user = new User("testuser", "test@example.com", "hash");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> projectService.setOverride(1L, user, request));
        assertEquals(400, ex.getStatusCode().value(),
                "localLogFolder exceeding 500 chars should be rejected with 400 on setOverride");
    }

    // ─── Property 4: Resolution Logic ────────────────────────────────────────────

    // Feature: default-log-location-setting, Property 4: Resolution Logic (override present)
    // **Validates: Requirements 4.1, 4.2**
    @Property(tries = 100)
    void resolution_prefersOverride_whenPresent(
            @ForAll("validLogFolderStrings") String projectDefault,
            @ForAll("validShortLogFolderStrings") String overrideValue
    ) {
        setupMocks();

        Project project = new Project("TestProject", "/remote", "/local/apk", projectDefault.trim());
        try {
            var idField = Project.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(project, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        User user = new User("testuser", "test@example.com", "hash");
        UserProjectOverride override = new UserProjectOverride(user, project, "/local/apk");
        override.setLocalLogFolder(overrideValue.trim());

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(overrideRepository.findByUserAndProject(user, project)).thenReturn(Optional.of(override));

        ResolvedProjectResponse resolved = projectService.getResolved(1L, user);

        assertEquals(overrideValue.trim(), resolved.localLogFolder(),
                "Resolved localLogFolder should equal override when present");
        assertTrue(resolved.overriddenLogFolder(),
                "overriddenLogFolder should be true when override is present");
    }

    // Feature: default-log-location-setting, Property 4: Resolution Logic (no override)
    // **Validates: Requirements 4.1, 4.2**
    @Property(tries = 100)
    void resolution_usesProjectDefault_whenNoOverride(
            @ForAll("validLogFolderStrings") String projectDefault
    ) {
        setupMocks();

        Project project = new Project("TestProject", "/remote", "/local/apk", projectDefault.trim());
        try {
            var idField = Project.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(project, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        User user = new User("testuser", "test@example.com", "hash");

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(overrideRepository.findByUserAndProject(user, project)).thenReturn(Optional.empty());

        ResolvedProjectResponse resolved = projectService.getResolved(1L, user);

        assertEquals(projectDefault.trim(), resolved.localLogFolder(),
                "Resolved localLogFolder should equal project default when no override");
        assertFalse(resolved.overriddenLogFolder(),
                "overriddenLogFolder should be false when no override");
    }

    // Feature: default-log-location-setting, Property 4: Resolution Logic (override with null logFolder)
    // **Validates: Requirements 4.1, 4.2**
    @Property(tries = 100)
    void resolution_usesProjectDefault_whenOverrideHasNullLogFolder(
            @ForAll("validLogFolderStrings") String projectDefault
    ) {
        setupMocks();

        Project project = new Project("TestProject", "/remote", "/local/apk", projectDefault.trim());
        try {
            var idField = Project.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(project, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        User user = new User("testuser", "test@example.com", "hash");
        UserProjectOverride override = new UserProjectOverride(user, project, "/local/apk");
        override.setLocalLogFolder(null); // Override exists but localLogFolder is null

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(overrideRepository.findByUserAndProject(user, project)).thenReturn(Optional.of(override));

        ResolvedProjectResponse resolved = projectService.getResolved(1L, user);

        assertEquals(projectDefault.trim(), resolved.localLogFolder(),
                "Resolved localLogFolder should equal project default when override has null logFolder");
        assertFalse(resolved.overriddenLogFolder(),
                "overriddenLogFolder should be false when override has null logFolder");
    }

    // ─── Generators ──────────────────────────────────────────────────────────────

    @Provide
    Arbitrary<String> validLogFolderStrings() {
        // Generate non-blank strings with trimmed length between 1 and 1024
        // May include leading/trailing whitespace to test trimming
        Arbitrary<String> core = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars('/', '\\', '-', '_', '.')
                .ofMinLength(1)
                .ofMaxLength(1024)
                .filter(s -> !s.isBlank());

        Arbitrary<String> padding = Arbitraries.of("", " ", "  ", "\t", " \t ");

        return Combinators.combine(padding, core, padding)
                .as((leading, content, trailing) -> {
                    String result = leading + content + trailing;
                    // Ensure trimmed length doesn't exceed 1024
                    if (result.trim().length() > 1024) {
                        return content.substring(0, Math.min(content.length(), 1024));
                    }
                    return result;
                })
                .filter(s -> !s.isBlank() && s.trim().length() <= 1024);
    }

    @Provide
    Arbitrary<String> validShortLogFolderStrings() {
        // Generate non-blank strings with trimmed length between 1 and 500 (for override)
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars('/', '\\', '-', '_', '.')
                .ofMinLength(1)
                .ofMaxLength(500)
                .filter(s -> !s.isBlank());
    }

    @Provide
    Arbitrary<String> whitespaceOnlyStrings() {
        // Generate strings composed entirely of whitespace characters
        return Arbitraries.strings()
                .withChars(' ', '\t', '\n', '\r')
                .ofMinLength(1)
                .ofMaxLength(50);
    }

    @Provide
    Arbitrary<String> overLengthProjectStrings() {
        // Generate strings whose trimmed length exceeds 1024
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .withChars('/', '-', '_')
                .ofMinLength(1025)
                .ofMaxLength(2048)
                .filter(s -> s.trim().length() > 1024);
    }

    @Provide
    Arbitrary<String> overLengthOverrideStrings() {
        // Generate strings whose trimmed length exceeds 500
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .withChars('/', '-', '_')
                .ofMinLength(501)
                .ofMaxLength(1000)
                .filter(s -> s.trim().length() > 500);
    }
}
