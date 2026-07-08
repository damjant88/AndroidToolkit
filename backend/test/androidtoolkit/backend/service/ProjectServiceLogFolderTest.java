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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProjectService log folder operations.
 * Validates: Requirements 1.2, 1.3, 3.2, 3.3, 4.1, 4.2
 */
class ProjectServiceLogFolderTest {

    private ProjectRepository projectRepository;
    private UserProjectOverrideRepository overrideRepository;
    private ProjectService service;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        overrideRepository = mock(UserProjectOverrideRepository.class);
        service = new ProjectService(projectRepository, overrideRepository);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private ProjectRequest validRequest(String logFolder) {
        return new ProjectRequest("TestProject", "/remote/apk", "/local/apk", logFolder, null, null, null, null, null);
    }

    private Project savedProject(Long id, String logFolder) {
        Project project = new Project("TestProject", "/remote/apk", "/local/apk", logFolder);
        // Use reflection to set the id since there's no setter
        try {
            var idField = Project.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(project, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return project;
    }

    private User testUser() {
        return new User("testuser", "test@example.com", "hashedpw");
    }

    private UserProjectOverride savedOverride(Long id, User user, Project project, String apkFolder, String logFolder) {
        UserProjectOverride override = new UserProjectOverride(user, project, apkFolder);
        override.setLocalLogFolder(logFolder);
        try {
            var idField = UserProjectOverride.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(override, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return override;
    }

    // ─── Create with valid localLogFolder ────────────────────────────────────────

    @Test
    @DisplayName("create with valid localLogFolder returns trimmed value in response")
    void create_withValidLogFolder_returnsTrimmedValueInResponse() {
        ProjectRequest request = validRequest("  /path/to/logs  ");
        when(projectRepository.existsByName("TestProject")).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project p = invocation.getArgument(0);
            // Simulate DB assigning an id
            var idField = Project.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(p, 1L);
            return p;
        });

        ProjectResponse response = service.create(request);

        assertEquals("/path/to/logs", response.localLogFolder());
    }

    // ─── Update with valid localLogFolder ────────────────────────────────────────

    @Test
    @DisplayName("update with valid localLogFolder persists trimmed value")
    void update_withValidLogFolder_persistsTrimmedValue() {
        Project existing = savedProject(1L, "/old/logs");
        ProjectRequest request = validRequest("  /new/logs/path  ");
        when(projectRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(projectRepository.findByName("TestProject")).thenReturn(Optional.of(existing));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = service.update(1L, request);

        assertEquals("/new/logs/path", response.localLogFolder());
        verify(projectRepository).save(argThat(p -> "/new/logs/path".equals(p.getLocalLogFolder())));
    }

    // ─── Override CRUD for localLogFolder ────────────────────────────────────────

    @Test
    @DisplayName("setOverride with valid localLogFolder returns override response with trimmed value")
    void setOverride_withValidLogFolder_returnsOverrideResponseWithTrimmedValue() {
        User user = testUser();
        Project project = savedProject(1L, "/project/logs");
        OverrideRequest request = new OverrideRequest("/user/apk", "  /user/logs  ");

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(overrideRepository.findByUserAndProject(user, project)).thenReturn(Optional.empty());
        when(overrideRepository.save(any(UserProjectOverride.class))).thenAnswer(invocation -> {
            UserProjectOverride o = invocation.getArgument(0);
            var idField = UserProjectOverride.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(o, 10L);
            return o;
        });

        OverrideResponse response = service.setOverride(1L, user, request);

        assertEquals("/user/logs", response.localLogFolder());
        assertEquals(1L, response.projectId());
    }

    @Test
    @DisplayName("setOverride updates existing override's localLogFolder")
    void setOverride_updatesExistingOverrideLogFolder() {
        User user = testUser();
        Project project = savedProject(1L, "/project/logs");
        UserProjectOverride existingOverride = savedOverride(10L, user, project, "/old/apk", "/old/logs");
        OverrideRequest request = new OverrideRequest("/new/apk", "/new/logs");

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(overrideRepository.findByUserAndProject(user, project)).thenReturn(Optional.of(existingOverride));
        when(overrideRepository.save(any(UserProjectOverride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OverrideResponse response = service.setOverride(1L, user, request);

        assertEquals("/new/logs", response.localLogFolder());
    }

    @Test
    @DisplayName("getOverride returns localLogFolder from existing override")
    void getOverride_returnsLogFolderFromExistingOverride() {
        User user = testUser();
        Project project = savedProject(1L, "/project/logs");
        UserProjectOverride override = savedOverride(10L, user, project, "/user/apk", "/user/logs");

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(overrideRepository.findByUserAndProject(user, project)).thenReturn(Optional.of(override));

        OverrideResponse response = service.getOverride(1L, user);

        assertEquals("/user/logs", response.localLogFolder());
    }

    // ─── Resolution with override present ────────────────────────────────────────

    @Test
    @DisplayName("getResolved with override present returns override localLogFolder and overriddenLogFolder=true")
    void getResolved_withOverride_returnsOverrideValueAndOverriddenTrue() {
        User user = testUser();
        Project project = savedProject(1L, "/project/default/logs");
        UserProjectOverride override = savedOverride(10L, user, project, "/user/apk", "/user/override/logs");

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(overrideRepository.findByUserAndProject(user, project)).thenReturn(Optional.of(override));

        ResolvedProjectResponse response = service.getResolved(1L, user);

        assertEquals("/user/override/logs", response.localLogFolder());
        assertTrue(response.overriddenLogFolder());
    }

    // ─── Resolution without override ─────────────────────────────────────────────

    @Test
    @DisplayName("getResolved without override returns project default localLogFolder and overriddenLogFolder=false")
    void getResolved_withoutOverride_returnsProjectDefaultAndOverriddenFalse() {
        User user = testUser();
        Project project = savedProject(1L, "/project/default/logs");

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(overrideRepository.findByUserAndProject(user, project)).thenReturn(Optional.empty());

        ResolvedProjectResponse response = service.getResolved(1L, user);

        assertEquals("/project/default/logs", response.localLogFolder());
        assertFalse(response.overriddenLogFolder());
    }

    @Test
    @DisplayName("getResolved with override that has null localLogFolder returns project default and overriddenLogFolder=false")
    void getResolved_withOverrideNullLogFolder_returnsProjectDefaultAndOverriddenFalse() {
        User user = testUser();
        Project project = savedProject(1L, "/project/default/logs");
        UserProjectOverride override = savedOverride(10L, user, project, "/user/apk", null);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(overrideRepository.findByUserAndProject(user, project)).thenReturn(Optional.of(override));

        ResolvedProjectResponse response = service.getResolved(1L, user);

        assertEquals("/project/default/logs", response.localLogFolder());
        assertFalse(response.overriddenLogFolder());
    }

    // ─── deleteOverride resets to project default ────────────────────────────────

    @Test
    @DisplayName("deleteOverride removes override so resolved returns project default")
    void deleteOverride_resetsToProjectDefault() {
        User user = testUser();
        Project project = savedProject(1L, "/project/default/logs");
        UserProjectOverride override = savedOverride(10L, user, project, "/user/apk", "/user/logs");

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(overrideRepository.findByUserAndProject(user, project))
                .thenReturn(Optional.of(override))  // first call: deleteOverride finds it
                .thenReturn(Optional.empty());       // second call: getResolved finds nothing

        service.deleteOverride(1L, user);

        verify(overrideRepository).delete(override);

        // After deletion, resolved should return project default
        ResolvedProjectResponse response = service.getResolved(1L, user);
        assertEquals("/project/default/logs", response.localLogFolder());
        assertFalse(response.overriddenLogFolder());
    }

    // ─── Validation edge cases ───────────────────────────────────────────────────

    @Test
    @DisplayName("setOverride with blank localLogFolder throws 400")
    void setOverride_withBlankLogFolder_throws400() {
        User user = testUser();
        OverrideRequest request = new OverrideRequest("/user/apk", "   ");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.setOverride(1L, user, request));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Local log folder is required"));
    }

    @Test
    @DisplayName("setOverride with null localLogFolder throws 400")
    void setOverride_withNullLogFolder_throws400() {
        User user = testUser();
        OverrideRequest request = new OverrideRequest("/user/apk", null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.setOverride(1L, user, request));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Local log folder is required"));
    }
}
