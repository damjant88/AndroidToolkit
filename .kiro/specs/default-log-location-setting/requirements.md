# Requirements Document

## Introduction

Add a "default log location" setting to project management, mirroring the existing "default APK location" pattern. Currently, projects have `remoteApkLocation` and `localApkFolder` fields (with per-user overrides for the local APK folder). This feature introduces an analogous `localLogFolder` field at the project level, with per-user override support, so that each project can define where logs are stored locally and individual users can customize the path for their machine.

## Glossary

- **Project_Service**: The backend service responsible for managing project entities, including CRUD operations and user overrides.
- **Project_Entity**: The JPA entity representing a project, stored in the `projects` table.
- **User_Project_Override**: The JPA entity that stores per-user overrides for project settings (e.g., local folder paths).
- **Admin_Projects_Panel**: The frontend admin UI component for creating, editing, and deleting projects.
- **User_Projects_Panel**: The frontend user UI component for viewing projects and setting personal overrides.
- **Project_Controller**: The REST controller exposing project management API endpoints.
- **Resolved_Project_Response**: The DTO that returns the effective project settings for a user, combining project defaults with any user-specific overrides.

## Requirements

### Requirement 1: Store Default Log Location at Project Level

**User Story:** As an admin, I want to configure a default local log folder for each project, so that all team members have a consistent default location for storing pulled logs.

#### Acceptance Criteria

1. THE Project_Entity SHALL include a `localLogFolder` column of type String with a maximum length of 1024 characters and a NOT NULL constraint, stored in the `projects` table.
2. WHEN an admin creates a project, THE Project_Service SHALL accept the `localLogFolder` value from the ProjectRequest and persist it to the `projects` table.
3. WHEN an admin updates a project, THE Project_Service SHALL accept the updated `localLogFolder` value from the ProjectRequest and persist it to the `projects` table.
4. WHEN a project is retrieved, THE Project_Controller SHALL include the `localLogFolder` field in the ProjectResponse.
5. THE ProjectRequest DTO SHALL include a `localLogFolder` field of type String.

### Requirement 2: Validate Default Log Location Input

**User Story:** As an admin, I want the system to validate the log location input, so that projects are not saved with empty or blank log folder paths.

#### Acceptance Criteria

1. WHEN a project create request is received with a null, empty, or whitespace-only `localLogFolder`, THE Project_Service SHALL return a 400 Bad Request error with a message indicating that the local log folder is required.
2. WHEN a project update request is received with a null, empty, or whitespace-only `localLogFolder`, THE Project_Service SHALL return a 400 Bad Request error with a message indicating that the local log folder is required.
3. WHEN a valid `localLogFolder` is provided, THE Project_Service SHALL trim leading and trailing whitespace before persisting the value.
4. IF the `localLogFolder` value exceeds 1024 characters after trimming, THEN THE Project_Service SHALL return a 400 Bad Request error with a message indicating that the local log folder exceeds the maximum allowed length.

### Requirement 3: Per-User Override for Log Location

**User Story:** As a user, I want to override the default log folder for my machine, so that I can store logs in a location that suits my local environment.

#### Acceptance Criteria

1. THE User_Project_Override entity SHALL include a `localLogFolder` field of type String with a maximum length of 500 characters.
2. WHEN a user sets a log folder override, THE Project_Service SHALL create or update the `localLogFolder` value in the user's override record for the specified project, trimming leading and trailing whitespace before persisting.
3. WHEN a user deletes their override, THE Project_Service SHALL remove the `localLogFolder` override so that subsequent resolved queries return the project default `localLogFolder`.
4. IF a user sets an override with a null or blank `localLogFolder`, THEN THE Project_Service SHALL reject the request with a 400 Bad Request error indicating that the local log folder is required.
5. IF a user sets an override with a `localLogFolder` exceeding 500 characters, THEN THE Project_Service SHALL reject the request with a 400 Bad Request error indicating that the value exceeds the maximum allowed length.

### Requirement 4: Resolve Effective Log Location

**User Story:** As a user, I want to see the effective log folder for a project (my override or the project default), so that I know where logs will be stored on my machine.

#### Acceptance Criteria

1. WHEN a user requests the resolved project settings and a User_Project_Override record exists for that user and project with a non-null `localLogFolder`, THE Project_Service SHALL return the user's `localLogFolder` override value.
2. WHEN a user requests the resolved project settings and no User_Project_Override record exists (or the override's `localLogFolder` is null), THE Project_Service SHALL return the project's default `localLogFolder`.
3. THE Resolved_Project_Response SHALL include a `localLogFolder` field (String) and an `overriddenLogFolder` boolean that is true when a User_Project_Override record with a non-null `localLogFolder` exists, and false otherwise.
4. IF the requested project does not exist, THEN THE Project_Service SHALL return a 404 Not Found error.

### Requirement 5: Admin UI for Default Log Location

**User Story:** As an admin, I want to see and edit the default log folder in the project management panel, so that I can configure it alongside other project settings.

#### Acceptance Criteria

1. THE Admin_Projects_Panel SHALL display a "Local Log Folder" text input field in the project creation form with placeholder text "Local Log Folder".
2. THE Admin_Projects_Panel SHALL display the `localLogFolder` value in a "Local Log Folder" column in the projects table.
3. WHEN editing a project, THE Admin_Projects_Panel SHALL populate the "Local Log Folder" input field with the project's current `localLogFolder` value.
4. WHEN the create form is submitted, THE Admin_Projects_Panel SHALL include the trimmed `localLogFolder` value in the create API request.
5. WHEN the update form is submitted, THE Admin_Projects_Panel SHALL include the trimmed `localLogFolder` value in the update API request.
6. IF the `localLogFolder` field is empty or blank when the create or update form is submitted, THEN THE Admin_Projects_Panel SHALL prevent submission and not send the API request.

### Requirement 6: User UI for Log Location Override

**User Story:** As a user, I want to view and override the log folder for each project from my projects panel, so that I can customize where logs are stored on my machine.

#### Acceptance Criteria

1. THE User_Projects_Panel SHALL display the effective `localLogFolder` for each project.
2. THE User_Projects_Panel SHALL provide an input field allowing the user to set a personal `localLogFolder` override.
3. WHEN the user clicks Save, THE User_Projects_Panel SHALL send the override value to the API.
4. WHEN the user clicks Reset, THE User_Projects_Panel SHALL delete the override and display the project default.
5. IF the override input field is empty or blank when Save is clicked, THEN THE User_Projects_Panel SHALL prevent submission and display a validation message.
6. WHEN the override is active, THE User_Projects_Panel SHALL visually indicate that the displayed log folder is a user override rather than the project default.

### Requirement 7: API Endpoints for Log Location

**User Story:** As a developer integrating with the system, I want the project API to support the log location field, so that all clients can read and write the setting.

#### Acceptance Criteria

1. THE Project_Controller SHALL accept an optional `localLogFolder` String field in the ProjectRequest body for POST `/api/projects` and PUT `/api/projects/{id}` endpoints.
2. THE Project_Controller SHALL return `localLogFolder` as a String field (which may be null) in the ProjectResponse for GET `/api/projects` and GET `/api/projects/{id}` endpoints.
3. WHEN a user sets a log folder override via PUT `/api/projects/{id}/overrides/me`, THE Project_Controller SHALL accept `localLogFolder` as a String field in the OverrideRequest body.
4. THE Project_Controller SHALL return `localLogFolder` in the OverrideResponse for GET `/api/projects/{id}/overrides/me`.
5. THE Project_Controller SHALL return `localLogFolder` (String) and `overriddenLogFolder` (boolean indicating whether the user has a personal log folder override) in the ResolvedProjectResponse for GET `/api/projects/{id}/resolved`.
6. IF a GET request to `/api/projects/{id}/overrides/me` is made and no override exists for the authenticated user, THEN THE Project_Controller SHALL return an OverrideResponse with `localLogFolder` set to null.

### Requirement 8: Log Collection Uses Resolved Log Location

**User Story:** As a user, I want log collection to use my resolved log folder setting, so that pulled logs are saved to the correct location automatically.

#### Acceptance Criteria

1. WHEN collecting logs for a device associated with a project, THE LogCollectionService SHALL resolve the `localLogFolder` for the authenticated user by checking for a user override first, then falling back to the project's default `localLogFolder`, and use the resolved value as the base directory for local log storage.
2. IF the resolved `localLogFolder` is empty or not configured, THEN THE LogCollectionService SHALL fall back to the default logs directory provided by `appServices.storagePaths().logsDir()`.
3. IF the device is not associated with any project and no projectId is provided, THEN THE LogCollectionService SHALL use the default logs directory from application configuration as the base directory.
4. WHEN the resolved base directory does not exist on the filesystem, THE LogCollectionService SHALL create the directory and any necessary parent directories before writing log files.
5. WHEN the resolved `localLogFolder` is used as the base directory, THE LogCollectionService SHALL preserve the existing subdirectory structure (`{flavor}/{deviceName}_{serial}/{date}/`) beneath the resolved base directory.
