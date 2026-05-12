# Implementation Plan: Project Profiles (Phase 1)

## Overview

Implement Project entity management with CRUD operations, user-specific path overrides, role-based access control, and frontend admin/user panels. The backend follows existing Spring Boot patterns (JPA entities, Spring Data repos, REST controllers with JWT auth). The frontend follows existing React + Axios patterns.

## Tasks

- [ ] 1. Create backend data models and repositories
  - [-] 1.1 Create the Project JPA entity
    - Create `backend/src/androidtoolkit/backend/entity/Project.java`
    - Fields: id (Long, auto-generated), name (unique, non-null), remoteApkLocation (non-null), localApkFolder (non-null), createdAt (Instant)
    - Use `@Entity`, `@Table(name = "projects")`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`
    - _Requirements: 1.1_

  - [-] 1.2 Create the UserProjectOverride JPA entity
    - Create `backend/src/androidtoolkit/backend/entity/UserProjectOverride.java`
    - Fields: id (Long, auto-generated), user (ManyToOne to User), project (ManyToOne to Project), localApkFolder (non-null)
    - Add `@UniqueConstraint(columnNames = {"user_id", "project_id"})` on the table
    - _Requirements: 3.1, 3.2_

  - [x] 1.3 Create the ProjectRepository interface
    - Create `backend/src/androidtoolkit/backend/repository/ProjectRepository.java`
    - Extend `JpaRepository<Project, Long>`
    - Add `boolean existsByName(String name)` and `Optional<Project> findByName(String name)`
    - _Requirements: 1.1, 1.7_

  - [x] 1.4 Create the UserProjectOverrideRepository interface
    - Create `backend/src/androidtoolkit/backend/repository/UserProjectOverrideRepository.java`
    - Extend `JpaRepository<UserProjectOverride, Long>`
    - Add `Optional<UserProjectOverride> findByUserAndProject(User user, Project project)`
    - Add `void deleteAllByProject(Project project)`
    - Add `List<UserProjectOverride> findAllByUser(User user)`
    - _Requirements: 3.1, 3.2, 3.3_

- [x] 2. Create backend DTOs and service layer
  - [x] 2.1 Create DTO record classes
    - Create `backend/src/androidtoolkit/backend/dto/ProjectRequest.java` — record(String name, String remoteApkLocation, String localApkFolder)
    - Create `backend/src/androidtoolkit/backend/dto/ProjectResponse.java` — record(Long id, String name, String remoteApkLocation, String localApkFolder, Instant createdAt)
    - Create `backend/src/androidtoolkit/backend/dto/ResolvedProjectResponse.java` — record(Long id, String name, String remoteApkLocation, String localApkFolder, boolean overridden)
    - Create `backend/src/androidtoolkit/backend/dto/OverrideRequest.java` — record(String localApkFolder)
    - Create `backend/src/androidtoolkit/backend/dto/OverrideResponse.java` — record(Long id, Long projectId, String localApkFolder)
    - _Requirements: 1.2, 1.3, 3.3, 3.7_

  - [x] 2.2 Create ProjectService with CRUD operations
    - Create `backend/src/androidtoolkit/backend/service/ProjectService.java`
    - Implement `findAll()`, `findById(Long id)`, `create(ProjectRequest)`, `update(Long id, ProjectRequest)`, `delete(Long id)`
    - Implement validation: reject blank/null fields with 400, reject duplicate names with 409, throw 404 for missing IDs
    - Cascade delete: remove all UserProjectOverride records when deleting a project
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9_

  - [x] 2.3 Add override management methods to ProjectService
    - Implement `getOverride(Long projectId, User user)` — returns override or throws 404
    - Implement `setOverride(Long projectId, User user, OverrideRequest)` — upserts override, validates non-blank
    - Implement `deleteOverride(Long projectId, User user)` — removes override if exists
    - Implement `getResolved(Long projectId, User user)` — returns project config with user override applied
    - _Requirements: 3.3, 3.4, 3.5, 3.6, 3.7_

- [x] 3. Create backend REST controller and security
  - [x] 3.1 Create ProjectController
    - Create `backend/src/androidtoolkit/backend/controller/ProjectController.java`
    - Map to `/api/projects`
    - Implement all endpoints: GET list, GET by id, POST create, PUT update, DELETE, GET/PUT/DELETE overrides/me, GET resolved
    - Add `requireAdmin()` check on POST, PUT, DELETE project endpoints (return 403 for non-admin)
    - Add `getAuthenticatedUser()` helper using SecurityContextHolder
    - _Requirements: 1.2, 1.3, 1.4, 1.5, 1.6, 2.1, 3.3, 3.4, 3.6, 3.7_

  - [x] 3.2 Update SecurityConfig to require authentication for project endpoints
    - Modify `backend/src/androidtoolkit/backend/config/SecurityConfig.java`
    - Add `.requestMatchers("/api/projects/**").authenticated()` BEFORE the existing `.requestMatchers("/api/**").permitAll()` line
    - This ensures unauthenticated requests get 401
    - _Requirements: 2.2, 2.3_

- [x] 4. Checkpoint - Backend verification
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Create frontend API layer and admin panel
  - [x] 5.1 Create projectApi.js
    - Create `frontend/src/api/projectApi.js`
    - Create Axios instance with baseURL `/api/projects` and JWT interceptor (same pattern as AllowedUsersPanel)
    - Export functions: list, getById, create, update, delete, getMyOverride, setMyOverride, deleteMyOverride, getResolved
    - _Requirements: 1.5, 3.3, 3.4, 3.7, 4.3, 4.5, 5.4_

  - [x] 5.2 Create AdminProjectsPanel component
    - Create `frontend/src/components/AdminProjectsPanel.js`
    - Display table of all projects (name, remote APK location, local APK folder)
    - Add create form with three input fields and submit button
    - Add edit button per row that toggles inline edit mode with pre-populated fields
    - Add delete button per row with `window.confirm()` confirmation
    - Display error messages from API responses without clearing the form
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7_

  - [x] 5.3 Create UserProjectsPanel component
    - Create `frontend/src/components/UserProjectsPanel.js`
    - List all projects using resolved endpoint
    - Display project name and remote APK location as read-only
    - Show local APK folder as editable input (pre-filled with resolved value)
    - Add Save button to PUT override, Reset button to DELETE override
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

  - [x] 5.4 Integrate panels into App.js
    - Import AdminProjectsPanel and UserProjectsPanel
    - Add "Manage Projects" toggle button for ADMIN users (same pattern as "Allowed Users" button)
    - Add "Projects" toggle button for all authenticated users
    - Conditionally render AdminProjectsPanel and UserProjectsPanel based on state
    - Guard admin panel: only render for `user?.role === 'ADMIN'`
    - _Requirements: 4.8, 5.1_

- [x] 6. Checkpoint - Full integration verification
  - Ensure all tests pass, ask the user if questions arise.

- [ ]* 7. Property-based tests for backend
  - [ ]* 7.1 Write property test for project creation round-trip
    - **Property 1: Project creation round-trip**
    - Create a project with valid data via service, retrieve by ID, verify all fields match
    - **Validates: Requirements 1.1, 1.2, 1.5, 1.6**

  - [ ]* 7.2 Write property test for project update preserves identity
    - **Property 2: Project update preserves identity and modifies fields**
    - Create a project, update with new valid data, verify ID unchanged and fields updated
    - **Validates: Requirements 1.3**

  - [ ]* 7.3 Write property test for project deletion cascades to overrides
    - **Property 3: Project deletion cascades to overrides**
    - Create a project with overrides, delete project, verify both project and overrides removed
    - **Validates: Requirements 1.4**

  - [ ]* 7.4 Write property test for duplicate project name rejection
    - **Property 4: Duplicate project name rejection**
    - Create a project, attempt to create another with same name, verify 409 response
    - **Validates: Requirements 1.7**

  - [ ]* 7.5 Write property test for invalid project data rejection
    - **Property 5: Invalid project data rejection**
    - Attempt to create/update with blank/null fields, verify 400 response and no state change
    - **Validates: Requirements 1.8**

  - [ ]* 7.6 Write property test for role-based write access control
    - **Property 6: Role-based write access control**
    - Verify USER role gets 403 on write endpoints, unauthenticated gets 401, any authenticated gets 200 on GET
    - **Validates: Requirements 2.1, 2.2, 2.3**

  - [ ]* 7.7 Write property test for override upsert round-trip
    - **Property 7: Override upsert round-trip**
    - Set override, retrieve it, verify match; update override, verify updated not duplicated
    - **Validates: Requirements 3.1, 3.2, 3.3, 3.4**

  - [ ]* 7.8 Write property test for override deletion
    - **Property 8: Override deletion removes override**
    - Create override, delete it, verify GET returns 404
    - **Validates: Requirements 3.6**

  - [ ]* 7.9 Write property test for resolved config
    - **Property 9: Resolved config applies user override when present**
    - With override: resolved returns override value; without: returns project default; name/remote always match project
    - **Validates: Requirements 3.7**

- [x] 8. Final checkpoint
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- The backend uses the existing pattern of manual role checks (not @PreAuthorize) consistent with the codebase
- Frontend follows the AllowedUsersPanel pattern: inline Axios instance with JWT interceptor

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["1.3", "1.4", "2.1"] },
    { "id": 2, "tasks": ["2.2", "2.3"] },
    { "id": 3, "tasks": ["3.1", "3.2"] },
    { "id": 4, "tasks": ["5.1"] },
    { "id": 5, "tasks": ["5.2", "5.3"] },
    { "id": 6, "tasks": ["5.4"] },
    { "id": 7, "tasks": ["7.1", "7.2", "7.3", "7.4", "7.5", "7.6", "7.7", "7.8", "7.9"] }
  ]
}
```
