# Design Document: Project Profiles (Phase 1)

## Architecture Overview

This feature adds Project entity management and user-specific path overrides to the existing Spring Boot + React application. It follows the established patterns: JPA entities, Spring Data repositories, REST controllers under `/api/*`, JWT-based auth with role checks, and React components using Axios for API calls.

### Component Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│  Frontend (React 19)                                            │
│  ┌──────────────────────┐   ┌─────────────────────────────┐    │
│  │ AdminProjectsPanel   │   │ UserProjectsPanel           │    │
│  │ (ADMIN only)         │   │ (all authenticated users)   │    │
│  │ - CRUD table/forms   │   │ - read-only project list    │    │
│  └──────────┬───────────┘   │ - override local path       │    │
│             │               └──────────────┬──────────────┘    │
│             └──────────┬───────────────────┘                   │
│                        ▼                                        │
│              ┌─────────────────┐                                │
│              │  projectApi.js  │  (Axios API layer)             │
│              └────────┬────────┘                                │
└───────────────────────┼─────────────────────────────────────────┘
                        │ HTTP (JWT Bearer)
┌───────────────────────┼─────────────────────────────────────────┐
│  Backend (Spring Boot 3.4 / Java 21)                            │
│                        ▼                                        │
│  ┌─────────────────────────────────────┐                        │
│  │ ProjectController                   │                        │
│  │ POST/PUT/DELETE → @PreAuthorize     │                        │
│  │ GET → authenticated                 │                        │
│  │ /overrides/me → authenticated       │                        │
│  │ /resolved → authenticated           │                        │
│  └────────────────┬────────────────────┘                        │
│                   ▼                                             │
│  ┌─────────────────────────────────────┐                        │
│  │ ProjectService                      │                        │
│  │ - CRUD logic + validation           │                        │
│  │ - override management               │                        │
│  │ - resolved config computation       │                        │
│  └────────────────┬────────────────────┘                        │
│                   ▼                                             │
│  ┌──────────────────────┐  ┌────────────────────────────┐      │
│  │ ProjectRepository    │  │ UserProjectOverrideRepo    │      │
│  └──────────┬───────────┘  └──────────────┬─────────────┘      │
│             └──────────────┬───────────────┘                    │
│                            ▼                                    │
│                   ┌────────────────┐                            │
│                   │  H2 / MySQL    │                            │
│                   └────────────────┘                            │
└─────────────────────────────────────────────────────────────────┘
```

## Data Models

### Project Entity

```java
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    private String remoteApkLocation;

    @Column(nullable = false)
    private String localApkFolder;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    // Constructors, getters, setters
}
```

### UserProjectOverride Entity

```java
@Entity
@Table(name = "user_project_overrides",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "project_id"}))
public class UserProjectOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String localApkFolder;

    // Constructors, getters, setters
}
```

### DTO / Request-Response Objects

```java
// Request body for create/update
public record ProjectRequest(String name, String remoteApkLocation, String localApkFolder) {}

// Response body for project data
public record ProjectResponse(Long id, String name, String remoteApkLocation, String localApkFolder, Instant createdAt) {}

// Response body for resolved config
public record ResolvedProjectResponse(Long id, String name, String remoteApkLocation, String localApkFolder, boolean overridden) {}

// Request body for override
public record OverrideRequest(String localApkFolder) {}

// Response body for override
public record OverrideResponse(Long id, Long projectId, String localApkFolder) {}
```

## API Interfaces

### ProjectController

Base path: `/api/projects`

| Method | Path | Auth | Role | Description |
|--------|------|------|------|-------------|
| GET | `/api/projects` | Required | Any | List all projects |
| GET | `/api/projects/{id}` | Required | Any | Get project by ID |
| POST | `/api/projects` | Required | ADMIN | Create project |
| PUT | `/api/projects/{id}` | Required | ADMIN | Update project |
| DELETE | `/api/projects/{id}` | Required | ADMIN | Delete project + cascaded overrides |
| GET | `/api/projects/{id}/overrides/me` | Required | Any | Get current user's override |
| PUT | `/api/projects/{id}/overrides/me` | Required | Any | Create/update current user's override |
| DELETE | `/api/projects/{id}/overrides/me` | Required | Any | Delete current user's override |
| GET | `/api/projects/{id}/resolved` | Required | Any | Get project with user's override applied |

### Controller Implementation

```java
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<ProjectResponse> listProjects() {
        return projectService.findAll();
    }

    @GetMapping("/{id}")
    public ProjectResponse getProject(@PathVariable Long id) {
        return projectService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@RequestBody ProjectRequest request) {
        return projectService.create(request);
    }

    @PutMapping("/{id}")
    public ProjectResponse updateProject(@PathVariable Long id, @RequestBody ProjectRequest request) {
        return projectService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@PathVariable Long id) {
        projectService.delete(id);
    }

    @GetMapping("/{id}/overrides/me")
    public OverrideResponse getMyOverride(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        return projectService.getOverride(id, user);
    }

    @PutMapping("/{id}/overrides/me")
    public OverrideResponse setMyOverride(@PathVariable Long id, @RequestBody OverrideRequest request) {
        User user = getAuthenticatedUser();
        return projectService.setOverride(id, user, request);
    }

    @DeleteMapping("/{id}/overrides/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyOverride(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        projectService.deleteOverride(id, user);
    }

    @GetMapping("/{id}/resolved")
    public ResolvedProjectResponse getResolvedProject(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        return projectService.getResolved(id, user);
    }

    private User getAuthenticatedUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return (User) auth.getPrincipal();
    }
}
```

## Security Configuration

The existing `SecurityConfig` needs to be updated to enforce role-based access on project write endpoints. Since the current config uses `permitAll()` for `/api/**`, we add method-level security:

```java
// In SecurityConfig or a new @EnableMethodSecurity configuration
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
    // Enables @PreAuthorize annotations
}
```

The controller uses a manual role check approach (consistent with the existing pattern where the app doesn't use `@PreAuthorize`):

```java
// In ProjectController, for write operations:
private void requireAdmin() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof User user)) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
    }
    if (user.getRole() != User.Role.ADMIN) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
    }
}
```

For unauthenticated requests, the `SecurityConfig` filter chain is updated:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers("/ws/**").permitAll()
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
    .requestMatchers("/", "/index.html", "/static/**", "/icons/**", "/manifest.json", "/favicon.ico").permitAll()
    .requestMatchers("/api/projects/**").authenticated()
    .requestMatchers("/api/**").permitAll()
    .anyRequest().authenticated()
)
```

## Service Layer

### ProjectService

```java
@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserProjectOverrideRepository overrideRepository;

    public ProjectService(ProjectRepository projectRepository,
                          UserProjectOverrideRepository overrideRepository) {
        this.projectRepository = projectRepository;
        this.overrideRepository = overrideRepository;
    }

    public List<ProjectResponse> findAll() {
        return projectRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public ProjectResponse findById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        return toResponse(project);
    }

    public ProjectResponse create(ProjectRequest request) {
        validateRequest(request);
        if (projectRepository.existsByName(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Project name already exists");
        }
        Project project = new Project();
        project.setName(request.name().trim());
        project.setRemoteApkLocation(request.remoteApkLocation().trim());
        project.setLocalApkFolder(request.localApkFolder().trim());
        return toResponse(projectRepository.save(project));
    }

    public ProjectResponse update(Long id, ProjectRequest request) {
        validateRequest(request);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        // Check name uniqueness (excluding current project)
        projectRepository.findByName(request.name().trim()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Project name already exists");
            }
        });
        project.setName(request.name().trim());
        project.setRemoteApkLocation(request.remoteApkLocation().trim());
        project.setLocalApkFolder(request.localApkFolder().trim());
        return toResponse(projectRepository.save(project));
    }

    public void delete(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        overrideRepository.deleteAllByProject(project);
        projectRepository.delete(project);
    }

    public OverrideResponse getOverride(Long projectId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        UserProjectOverride override = overrideRepository.findByUserAndProject(user, project)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No override found"));
        return toOverrideResponse(override);
    }

    public OverrideResponse setOverride(Long projectId, User user, OverrideRequest request) {
        if (request.localApkFolder() == null || request.localApkFolder().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Local APK folder is required");
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        UserProjectOverride override = overrideRepository.findByUserAndProject(user, project)
                .orElseGet(() -> {
                    UserProjectOverride o = new UserProjectOverride();
                    o.setUser(user);
                    o.setProject(project);
                    return o;
                });
        override.setLocalApkFolder(request.localApkFolder().trim());
        return toOverrideResponse(overrideRepository.save(override));
    }

    public void deleteOverride(Long projectId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        overrideRepository.findByUserAndProject(user, project)
                .ifPresent(overrideRepository::delete);
    }

    public ResolvedProjectResponse getResolved(Long projectId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        Optional<UserProjectOverride> override = overrideRepository.findByUserAndProject(user, project);
        String resolvedLocalPath = override.map(UserProjectOverride::getLocalApkFolder)
                .orElse(project.getLocalApkFolder());
        return new ResolvedProjectResponse(
                project.getId(), project.getName(), project.getRemoteApkLocation(),
                resolvedLocalPath, override.isPresent()
        );
    }

    private void validateRequest(ProjectRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project name is required");
        }
        if (request.remoteApkLocation() == null || request.remoteApkLocation().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Remote APK location is required");
        }
        if (request.localApkFolder() == null || request.localApkFolder().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Local APK folder is required");
        }
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(project.getId(), project.getName(),
                project.getRemoteApkLocation(), project.getLocalApkFolder(), project.getCreatedAt());
    }

    private OverrideResponse toOverrideResponse(UserProjectOverride override) {
        return new OverrideResponse(override.getId(), override.getProject().getId(),
                override.getLocalApkFolder());
    }
}
```

## Repository Interfaces

```java
public interface ProjectRepository extends JpaRepository<Project, Long> {
    boolean existsByName(String name);
    Optional<Project> findByName(String name);
}

public interface UserProjectOverrideRepository extends JpaRepository<UserProjectOverride, Long> {
    Optional<UserProjectOverride> findByUserAndProject(User user, Project project);
    void deleteAllByProject(Project project);
    List<UserProjectOverride> findAllByUser(User user);
}
```

## Frontend Components

### API Layer (`projectApi.js`)

```javascript
import axios from 'axios';

const api = axios.create({ baseURL: '/api/projects' });
api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export const projectApi = {
  list: () => api.get('/'),
  getById: (id) => api.get(`/${id}`),
  create: (data) => api.post('/', data),
  update: (id, data) => api.put(`/${id}`, data),
  delete: (id) => api.delete(`/${id}`),
  getMyOverride: (projectId) => api.get(`/${projectId}/overrides/me`),
  setMyOverride: (projectId, data) => api.put(`/${projectId}/overrides/me`, data),
  deleteMyOverride: (projectId) => api.delete(`/${projectId}/overrides/me`),
  getResolved: (projectId) => api.get(`/${projectId}/resolved`),
};
```

### AdminProjectsPanel Component

Located at `frontend/src/components/AdminProjectsPanel.js`. Follows the same pattern as `AllowedUsersPanel.js`:

- Table listing all projects (name, remote APK location, local APK folder)
- Create form with three input fields
- Edit button per row that opens inline edit form
- Delete button per row with `window.confirm()` prompt
- Error message display area
- Only rendered when user has ADMIN role

### UserProjectsPanel Component

Located at `frontend/src/components/UserProjectsPanel.js`:

- Lists all projects with resolved configuration
- Project name and remote APK location displayed as read-only text
- Local APK folder shown as editable input (pre-filled with resolved value)
- Save button per project to PUT the override
- Reset button to DELETE the override and revert to project default

### App.js Integration

```javascript
// In AppContent, add admin projects button (similar to AllowedUsers):
{user?.role === 'ADMIN' && (
  <button className="toolbar-small-btn" onClick={() => setShowAdminProjects(!showAdminProjects)}>
    {showAdminProjects ? 'Hide Projects' : 'Manage Projects'}
  </button>
)}

// And the user-facing projects panel for all users:
{showProjects && <UserProjectsPanel />}

// Admin panel (conditionally rendered):
{showAdminProjects && <AdminProjectsPanel />}
```

## Error Handling

The feature uses the existing `GlobalExceptionHandler` pattern:

- `ResponseStatusException(BAD_REQUEST)` → 400 for validation errors
- `ResponseStatusException(UNAUTHORIZED)` → 401 for missing auth
- `ResponseStatusException(FORBIDDEN)` → 403 for non-admin write attempts
- `ResponseStatusException(NOT_FOUND)` → 404 for missing resources
- `ResponseStatusException(CONFLICT)` → 409 for duplicate names

All error responses follow the existing format: `{ "success": false, "message": "..." }`

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Project creation round-trip

*For any* valid project data (non-blank name, non-blank remoteApkLocation, non-blank localApkFolder), creating a project via POST and then retrieving it via GET by the returned ID should yield a project with identical name, remoteApkLocation, and localApkFolder values.

**Validates: Requirements 1.1, 1.2, 1.5, 1.6**

### Property 2: Project update preserves identity and modifies fields

*For any* existing project and any valid update data, updating the project via PUT should return the same project ID with the new field values, and a subsequent GET should reflect those updated values.

**Validates: Requirements 1.3**

### Property 3: Project deletion cascades to overrides

*For any* project that has one or more associated UserProjectOverride records, deleting the project should result in both the project and all its associated overrides being removed from the database.

**Validates: Requirements 1.4**

### Property 4: Duplicate project name rejection

*For any* two project creation or update requests with the same name (case-sensitive), the system should accept the first and reject the second with HTTP 409, regardless of the other field values.

**Validates: Requirements 1.7**

### Property 5: Invalid project data rejection

*For any* project creation or update request where at least one required field (name, remoteApkLocation, localApkFolder) is blank or null, the system should reject the request with HTTP 400 and the project count should remain unchanged.

**Validates: Requirements 1.8**

### Property 6: Role-based write access control

*For any* authenticated user with Role.USER and any valid project write request (POST, PUT, DELETE), the system should reject the request with HTTP 403. For any unauthenticated request to any project endpoint, the system should reject with HTTP 401. For any authenticated user (regardless of role) and any GET request, the system should return a successful response.

**Validates: Requirements 2.1, 2.2, 2.3**

### Property 7: Override upsert round-trip

*For any* authenticated user, any existing project, and any non-blank localApkFolder string, creating or updating an override via PUT and then retrieving it via GET should yield an override with the same localApkFolder value. Performing a second PUT with a different value should update (not duplicate) the override.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4**

### Property 8: Override deletion removes override

*For any* authenticated user with an existing override on a project, deleting the override via DELETE should result in a subsequent GET returning HTTP 404 for that override.

**Validates: Requirements 3.6**

### Property 9: Resolved config applies user override when present

*For any* project and authenticated user, the resolved endpoint should return the user's override localApkFolder if one exists, or the project's default localApkFolder if no override exists. The name and remoteApkLocation should always match the project's values regardless of override presence.

**Validates: Requirements 3.7**
