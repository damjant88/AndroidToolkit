# Requirements Document

## Introduction

Phase 1 of the Project Profiles feature introduces the concept of Projects to AndroidToolkit. A Project groups configuration for a specific Android application under test — including its name, remote APK location, and local APK folder. Admin users manage projects through a dedicated admin page, while regular users can override local path settings on a per-user basis. This phase lays the foundation for future Device Groups and Profiles features.

## Glossary

- **System**: The AndroidToolkit backend application (Spring Boot REST API)
- **Admin_Page**: The React frontend page at `/admin/projects` accessible only to ADMIN role users
- **Project**: A JPA entity representing a configured Android project with name, remote APK location, and local APK folder
- **UserProjectOverride**: A JPA entity storing per-user overrides of project path settings, linked to both User and Project
- **Admin_User**: A user with `Role.ADMIN` in the User entity
- **Regular_User**: A user with `Role.USER` in the User entity
- **Remote_APK_Location**: A network path or URL pointing to where APK builds are stored remotely
- **Local_APK_Folder**: A local filesystem path where APK files are stored on the user's machine

## Requirements

### Requirement 1: Project Entity Management

**User Story:** As an Admin_User, I want to create and manage Project entities, so that the team has a shared configuration for each Android application under test.

#### Acceptance Criteria

1.1 THE System SHALL persist Project entities with the following fields: a unique auto-generated ID, a project name (non-blank, unique), a Remote_APK_Location (non-blank string), and a Local_APK_Folder (non-blank string).

1.2 WHEN an Admin_User submits a valid project creation request via `POST /api/projects`, THE System SHALL create a new Project entity and return the created project with HTTP status 201.

1.3 WHEN an Admin_User submits a valid project update request via `PUT /api/projects/{id}`, THE System SHALL update the specified Project entity fields and return the updated project with HTTP status 200.

1.4 WHEN an Admin_User submits a delete request via `DELETE /api/projects/{id}`, THE System SHALL delete the specified Project entity and all associated UserProjectOverride records, and return HTTP status 204.

1.5 WHEN any authenticated user sends a request to `GET /api/projects`, THE System SHALL return a list of all Project entities with HTTP status 200.

1.6 WHEN any authenticated user sends a request to `GET /api/projects/{id}`, THE System SHALL return the specified Project entity with HTTP status 200.

1.7 IF a project creation or update request contains a project name that already exists, THEN THE System SHALL reject the request and return HTTP status 409 with an error message indicating the name conflict.

1.8 IF a project creation or update request contains blank or missing required fields, THEN THE System SHALL reject the request and return HTTP status 400 with a validation error message.

1.9 IF a request references a project ID that does not exist, THEN THE System SHALL return HTTP status 404 with an error message.

### Requirement 2: Admin-Only Project Write Access

**User Story:** As an Admin_User, I want project creation, update, and deletion restricted to admin users, so that regular users cannot modify shared project configuration.

#### Acceptance Criteria

2.1 WHEN a Regular_User sends a POST, PUT, or DELETE request to `/api/projects/**`, THE System SHALL reject the request and return HTTP status 403.

2.2 WHEN an unauthenticated request is sent to any `/api/projects/**` endpoint, THE System SHALL reject the request and return HTTP status 401.

2.3 THE System SHALL permit GET requests to `/api/projects` and `/api/projects/{id}` for any authenticated user regardless of role.

### Requirement 3: User Project Path Overrides

**User Story:** As a Regular_User, I want to override the local APK folder for a project on my account, so that I can point to my own local build directory without affecting other users.

#### Acceptance Criteria

3.1 THE System SHALL persist UserProjectOverride entities with the following fields: a unique auto-generated ID, a reference to the User, a reference to the Project, and a local APK folder override (non-blank string).

3.2 THE System SHALL enforce a unique constraint on the combination of User and Project in UserProjectOverride, allowing at most one override per user per project.

3.3 WHEN an authenticated user sends a PUT request to `PUT /api/projects/{projectId}/overrides/me`, THE System SHALL create or update the UserProjectOverride for the authenticated user and the specified project, and return the override with HTTP status 200.

3.4 WHEN an authenticated user sends a GET request to `GET /api/projects/{projectId}/overrides/me`, THE System SHALL return the UserProjectOverride for the authenticated user and the specified project with HTTP status 200.

3.5 IF no UserProjectOverride exists for the authenticated user and the specified project, THEN THE System SHALL return HTTP status 404 when the override is requested via GET.

3.6 WHEN an authenticated user sends a DELETE request to `DELETE /api/projects/{projectId}/overrides/me`, THE System SHALL delete the UserProjectOverride for the authenticated user and the specified project, and return HTTP status 204.

3.7 WHEN an authenticated user sends a GET request to `GET /api/projects/{projectId}/resolved`, THE System SHALL return the project configuration with the local APK folder replaced by the user's override value if one exists, or the project default if no override exists.

### Requirement 4: Admin Project Management Page

**User Story:** As an Admin_User, I want a dedicated admin page to manage projects in the browser, so that I can create, edit, and delete projects through a visual interface.

#### Acceptance Criteria

4.1 THE Admin_Page SHALL display a table listing all projects with columns for project name, Remote_APK_Location, and Local_APK_Folder.

4.2 THE Admin_Page SHALL provide a form to create a new project with input fields for project name, Remote_APK_Location, and Local_APK_Folder.

4.3 WHEN an Admin_User submits the create project form with valid data, THE Admin_Page SHALL send a POST request to the backend API and display the newly created project in the table.

4.4 THE Admin_Page SHALL provide an edit action for each project row that opens a form pre-populated with the project's current values.

4.5 WHEN an Admin_User submits the edit project form with valid data, THE Admin_Page SHALL send a PUT request to the backend API and update the project row in the table.

4.6 THE Admin_Page SHALL provide a delete action for each project row that prompts for confirmation before sending a DELETE request to the backend API.

4.7 IF the backend API returns a validation or conflict error, THEN THE Admin_Page SHALL display the error message to the Admin_User without clearing the form.

4.8 WHEN a Regular_User navigates to the `/admin/projects` route, THE Admin_Page SHALL deny access and redirect the user or display an unauthorized message.

### Requirement 5: Frontend Project Read Access for Regular Users

**User Story:** As a Regular_User, I want to view project configurations and manage my local path overrides, so that I can customize my local setup for each project.

#### Acceptance Criteria

5.1 THE System SHALL provide a user-facing project view (accessible to all authenticated users) that displays the list of projects with their resolved configuration (applying the user's overrides where applicable).

5.2 THE System SHALL provide an interface element for each project that allows the authenticated user to set or update their Local_APK_Folder override.

5.3 WHEN a Regular_User views a project, THE System SHALL display the project name and Remote_APK_Location as read-only fields.

5.4 WHEN a Regular_User updates their Local_APK_Folder override, THE System SHALL send the override to the backend API and reflect the updated value in the project view.
