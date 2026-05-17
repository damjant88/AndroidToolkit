# Implementation Plan: Default Log Location Setting

## Overview

Add a `localLogFolder` setting to the project management system, mirroring the existing `localApkFolder` pattern. This involves entity changes, DTO changes, service validation/resolution logic, log collection integration, and frontend UI updates for both admin and user panels.

## Tasks

- [x] 1. Extend entities and DTOs with `localLogFolder` field
  - [x] 1.1 Add `localLogFolder` field to `Project` entity
    - Add `@Column(nullable = false, length = 1024) private String localLogFolder;` to `Project.java`
    - Add getter and setter methods
    - Update the constructor to accept `localLogFolder` parameter
    - _Requirements: 1.1_

  - [x] 1.2 Add `localLogFolder` field to `UserProjectOverride` entity
    - Add `@Column(length = 500) private String localLogFolder;` (nullable) to `UserProjectOverride.java`
    - Add getter and setter methods
    - _Requirements: 3.1_

  - [x] 1.3 Add `localLogFolder` field to all DTOs
    - Add `String localLogFolder` to `ProjectRequest` record
    - Add `String localLogFolder` to `ProjectResponse` record (include after `localApkFolder`)
    - Add `String localLogFolder` to `OverrideRequest` record
    - Add `String localLogFolder` to `OverrideResponse` record
    - Add `String localLogFolder` and `boolean overriddenLogFolder` to `ResolvedProjectResponse` record
    - _Requirements: 1.5, 4.3, 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 2. Extend `ProjectService` with validation and resolution logic
  - [x] 2.1 Add `localLogFolder` validation to `validateRequest()`
    - Add null/blank check: throw 400 "Local log folder is required" if null, empty, or whitespace-only
    - Add length check: throw 400 "Local log folder exceeds the maximum allowed length" if trimmed length > 1024
    - _Requirements: 2.1, 2.2, 2.4_

  - [x] 2.2 Update `create()` and `update()` to persist `localLogFolder`
    - In `create()`: call `project.setLocalLogFolder(request.localLogFolder().trim())`
    - In `update()`: call `project.setLocalLogFolder(request.localLogFolder().trim())`
    - _Requirements: 1.2, 1.3, 2.3_

  - [x] 2.3 Update `setOverride()` to handle `localLogFolder` override
    - Add validation: throw 400 "Local log folder is required" if `localLogFolder` is null or blank
    - Add length check: throw 400 if trimmed length > 500
    - Persist trimmed `localLogFolder` on the override entity
    - _Requirements: 3.2, 3.4, 3.5_

  - [x] 2.4 Update `getResolved()` to resolve `localLogFolder` with override-first logic
    - Resolve `localLogFolder`: use override's `localLogFolder` if non-null, else project's default
    - Compute `overriddenLogFolder` boolean: true when override exists with non-null `localLogFolder`
    - Return both fields in the `ResolvedProjectResponse`
    - _Requirements: 4.1, 4.2, 4.3_

  - [x] 2.5 Update `toResponse()` and `toOverrideResponse()` mappings
    - Include `localLogFolder` in `ProjectResponse` construction
    - Include `localLogFolder` in `OverrideResponse` construction
    - _Requirements: 1.4, 7.2, 7.4, 7.6_

  - [x]* 2.6 Write property tests for `ProjectService` log folder validation and resolution
    - **Property 1: Persistence Round-Trip** — For any valid non-blank string ≤1024 chars, creating a project and retrieving it returns the trimmed value
    - **Property 2: Blank Input Rejection** — For any whitespace-only string, create/update/setOverride is rejected with 400
    - **Property 3: Length Validation** — For any string with trimmed length > 1024 (project) or > 500 (override), the operation is rejected with 400
    - **Property 4: Resolution Logic** — Resolved `localLogFolder` equals override when present, project default otherwise
    - **Validates: Requirements 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 3.4, 3.5, 4.1, 4.2**

  - [x]* 2.7 Write unit tests for `ProjectService` log folder operations
    - Test create with valid `localLogFolder` returns it in response
    - Test update with valid `localLogFolder` persists trimmed value
    - Test override CRUD for `localLogFolder`
    - Test resolution with and without override
    - Test `deleteOverride` resets to project default
    - _Requirements: 1.2, 1.3, 3.2, 3.3, 4.1, 4.2_

- [x] 3. Checkpoint - Ensure all backend entity/service tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Integrate `localLogFolder` into `LogCollectionService`
  - [x] 4.1 Update `collectLogs()` to resolve `localLogFolder` as base directory
    - Accept an authenticated `User` parameter (or retrieve from security context)
    - Resolve the project's effective `localLogFolder` for the user (override-first, then project default)
    - Use the resolved path as `baseLogsDir` instead of `appServices.storagePaths().logsDir().getPath()`
    - Fall back to `appServices.storagePaths().logsDir().getPath()` when resolved path is empty or no project is associated
    - Ensure directory creation (`Files.createDirectories`) for the resolved path
    - Preserve existing subdirectory structure: `{flavor}/{deviceName}_{serial}/{date}/`
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

  - [x]* 4.2 Write property tests for log collection path resolution
    - **Property 5: Log Collection Uses Resolved Path** — For any project/user/override combination, the base directory equals the resolved `localLogFolder`, falling back to app default when empty or no project
    - **Property 6: Subdirectory Structure Preservation** — For any resolved base directory, logs are stored under `{baseDir}/{flavor}/{deviceName}_{serial}/{date}/`
    - **Validates: Requirements 8.1, 8.2, 8.3, 8.5**

  - [x]* 4.3 Write unit tests for `LogCollectionService` log folder integration
    - Test that resolved `localLogFolder` is used as base directory
    - Test fallback to default when resolved path is empty
    - Test fallback to default when no project is associated
    - Test directory creation when path doesn't exist
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

- [x] 5. Checkpoint - Ensure all backend tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Update `AdminProjectsPanel` frontend component
  - [x] 6.1 Add `localLogFolder` to admin create and edit forms
    - Add `localLogFolder` state variable for create form
    - Add `editLocalLogFolder` state variable for edit form
    - Add "Local Log Folder" input field in create form with placeholder "Local Log Folder"
    - Add "Local Log Folder" input field in edit form
    - Include `localLogFolder` in create API call payload
    - Include `localLogFolder` in update API call payload
    - Prevent form submission if `localLogFolder` is empty/blank (same pattern as existing fields)
    - Populate `editLocalLogFolder` from project data when entering edit mode
    - _Requirements: 5.1, 5.3, 5.4, 5.5, 5.6_

  - [x] 6.2 Add "Local Log Folder" column to projects table
    - Add `<th>Local Log Folder</th>` to table header
    - Add `<td>{p.localLogFolder}</td>` to table body rows
    - Add edit input cell in the editing row
    - _Requirements: 5.2_

- [x] 7. Update `UserProjectsPanel` frontend component
  - [x] 7.1 Display effective `localLogFolder` and add override controls
    - Display the effective `localLogFolder` from resolved response for each project
    - Add a separate input field for log folder override (alongside existing APK folder override)
    - Add Save button for log folder override
    - Add Reset button for log folder override
    - Prevent save if log folder override input is empty/blank
    - Show visual indicator when log folder override is active (e.g., label or styling based on `overriddenLogFolder` boolean)
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

- [x] 8. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The implementation mirrors the existing `localApkFolder` pattern throughout all layers
- No new API endpoints are needed — existing endpoints already handle the extended DTOs
- The `ProjectController` does not need modification since it delegates to `ProjectService` and uses the DTO records directly

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3"] },
    { "id": 1, "tasks": ["2.1", "2.2", "2.3", "2.4", "2.5"] },
    { "id": 2, "tasks": ["2.6", "2.7"] },
    { "id": 3, "tasks": ["4.1"] },
    { "id": 4, "tasks": ["4.2", "4.3"] },
    { "id": 5, "tasks": ["6.1", "6.2", "7.1"] }
  ]
}
```
