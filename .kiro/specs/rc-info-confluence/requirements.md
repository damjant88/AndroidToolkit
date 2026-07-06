# Requirements Document

## Introduction

This document defines requirements for two enhancements to the RC Info feature in AndroidToolkit:
1. Admin-configurable Confluence page IDs per project, replacing hardcoded frontend mappings
2. An Amazon Bedrock LLM periodic job that monitors Confluence pages for changes, extracts artifact information, and caches parsed results for fast frontend access

## Glossary

- **Admin_Panel**: The administrative interface where project settings are configured
- **Project**: A software project entity stored in the database with associated metadata
- **Confluence_Page_ID**: A unique identifier for a page in Atlassian Confluence
- **Parent_Page_ID**: The Confluence page ID of a "Releases" parent page whose children represent individual release pages
- **Artifacts_Page_ID**: A direct Confluence page ID override pointing to a specific artifacts page
- **Cached_Artifacts**: A database table storing LLM-extracted artifact data from Confluence pages
- **Confluence_Monitor_Service**: A scheduled backend service that periodically checks Confluence for page changes
- **Bedrock_Extractor**: A service that calls Amazon Bedrock LLM to extract structured artifact data from Confluence HTML
- **Artifact**: A parsed component entry containing version, S3 paths, and descriptive text extracted from a Confluence page
- **Page_Version**: An integer version number assigned by Confluence to track page edits

## Requirements

### Requirement 1: Project Confluence Page ID Storage

**User Story:** As an administrator, I want to configure Confluence page IDs per project in the database, so that the system can dynamically resolve which Confluence page to fetch for each project without hardcoded values.

#### Acceptance Criteria

1. THE Project entity SHALL include a `confluenceParentPageId` field with a maximum length of 50 characters
2. THE Project entity SHALL include a `confluenceArtifactsPageId` field with a maximum length of 50 characters
3. WHEN a project is created with Confluence page IDs, THE ProjectService SHALL persist both page ID fields to the database
4. WHEN a project is updated with new Confluence page IDs, THE ProjectService SHALL update the stored values in the database
5. WHEN neither Confluence page ID is provided for a project, THE Project entity SHALL store null values for both fields
6. WHEN only one Confluence page ID field is provided (e.g., only `confluenceParentPageId`), THE ProjectService SHALL persist the provided field and store null for the other field independently
7. WHEN a Confluence page ID field is updated to an empty string, THE ProjectService SHALL treat it as null and store null in the database

### Requirement 2: Project API DTO Changes

**User Story:** As a frontend developer, I want the project API to include Confluence page IDs in request and response payloads, so that the admin panel can read and write these values.

#### Acceptance Criteria

1. THE ProjectRequest DTO SHALL include nullable `confluenceParentPageId` and `confluenceArtifactsPageId` String fields that accept values up to 50 characters in length
2. THE ProjectResponse DTO SHALL include nullable `confluenceParentPageId` and `confluenceArtifactsPageId` String fields that return null when no value is configured for the project
3. THE ResolvedProjectResponse DTO SHALL include nullable `confluenceParentPageId` and `confluenceArtifactsPageId` String fields matching the same contract as ProjectResponse
4. WHEN a project is fetched via the API, THE ProjectResponse SHALL contain the Confluence page ID values as currently stored in the database at the time of the request
5. IF a ProjectRequest contains a `confluenceParentPageId` or `confluenceArtifactsPageId` value exceeding 50 characters, THEN THE Backend SHALL reject the request with an error response indicating the field length constraint violation

### Requirement 3: Admin Panel Confluence Configuration UI

**User Story:** As an administrator, I want input fields in the admin panel to configure Confluence page IDs per project, so that I can set up RC Info sources without code changes.

#### Acceptance Criteria

1. WHEN an administrator views a project in the Admin_Panel, THE Admin_Panel SHALL display a text input field labeled "Confluence Parent Page ID" with a maximum input length of 50 characters
2. WHEN an administrator views a project in the Admin_Panel, THE Admin_Panel SHALL display a text input field labeled "Confluence Artifacts Page ID" with a maximum input length of 50 characters
3. WHEN an administrator saves project settings with Confluence page IDs, THE Admin_Panel SHALL send the values to the backend API and display a success confirmation upon receiving a successful response
4. IF the backend API returns an error when saving Confluence page IDs, THEN THE Admin_Panel SHALL display an error message indicating the save failed and preserve the field values so the administrator can retry
5. WHEN a project has existing Confluence page IDs, THE Admin_Panel SHALL pre-populate the input fields with the current values
6. WHEN an administrator clears a Confluence page ID field and saves, THE Admin_Panel SHALL send a null value for that field to the backend API

### Requirement 4: Dynamic Confluence Artifact Resolution

**User Story:** As a user, I want the RC Info feature to resolve Confluence pages from the database configuration, so that artifact information is fetched from the correct project-specific page.

#### Acceptance Criteria

1. WHEN the artifacts endpoint receives a request with a project ID, THE Backend SHALL read the Confluence page IDs from the project database record
2. IF the provided project ID does not exist in the database, THEN THE Backend SHALL return an error response indicating that the project was not found
3. WHEN a project has `confluenceArtifactsPageId` set, THE Backend SHALL fetch that page directly from Confluence
4. WHEN a project has only `confluenceParentPageId` set, THE Backend SHALL fetch children of that page and select the child page with the most recent creation date
5. IF a project has only `confluenceParentPageId` set and the parent page has no child pages, THEN THE Backend SHALL return an error response indicating no release pages were found
6. WHEN a project has both `confluenceArtifactsPageId` and `confluenceParentPageId` set, THE Backend SHALL use `confluenceArtifactsPageId` as the priority override
7. IF a project has neither Confluence page ID configured, THEN THE Backend SHALL return an error response indicating missing Confluence configuration for the project
8. IF the Confluence API is unreachable or returns an error during artifact page fetching, THEN THE Backend SHALL return an error response indicating that Confluence is unavailable

### Requirement 5: Remove Hardcoded Frontend Page IDs

**User Story:** As a developer, I want the hardcoded `RC_PARENT_PAGES` map removed from the frontend, so that Confluence page resolution is fully driven by backend configuration.

#### Acceptance Criteria

1. THE Frontend source code SHALL NOT contain any Confluence page ID string constants or static mappings between project names and Confluence page IDs
2. WHEN the frontend needs to resolve a Confluence page for a project, THE Frontend SHALL use the `id` field from the corresponding backend project record to call the backend artifacts endpoint
3. WHEN a user clicks "RC Info" for a selected project, THE Frontend SHALL call the backend artifacts endpoint (`/api/confluence/artifacts?projectId={id}`) using the backend project's numeric ID
4. IF the selected project has no corresponding backend project record loaded, THEN THE Frontend SHALL display an error message indicating that the project is not found in the backend
5. IF the backend project has neither `confluenceParentPageId` nor `confluenceArtifactsPageId` configured, THEN THE Frontend SHALL display an error message indicating that no Confluence page ID is configured and directing the administrator to set it in the Admin Panel

### Requirement 6: Cached Artifacts Database Schema

**User Story:** As a system architect, I want a dedicated table for cached artifact data, so that LLM-extracted results can be stored and served quickly without repeated Confluence API calls.

#### Acceptance Criteria

1. THE Database SHALL contain a `cached_artifacts` table with columns: `id` (auto-increment primary key), `project_id` (NOT NULL, references the projects table), `confluence_page_id` (VARCHAR, max 50 characters), `confluence_page_version` (integer), `page_title` (VARCHAR, max 500 characters), `artifacts_json` (TEXT), `last_checked_at` (TIMESTAMP), `last_updated_at` (TIMESTAMP), and `llm_model` (VARCHAR, max 100 characters)
2. THE `cached_artifacts` table SHALL enforce a unique constraint on `project_id` to allow only one cached entry per project, and SHALL define `project_id` as a foreign key referencing the `projects` table
3. THE `cached_artifacts` table SHALL store the Confluence page version number as an integer to enable change detection by comparing the stored value against the version fetched from the Confluence API
4. THE `cached_artifacts` table SHALL store the `artifacts_json` field as TEXT to hold the full JSON array of parsed components
5. THE `cached_artifacts` table SHALL store `last_checked_at` (updated each time the monitor checks the project) and `last_updated_at` (updated only when new artifact data is extracted and written) as TIMESTAMP columns

### Requirement 7: Confluence Monitor Scheduled Job

**User Story:** As a system operator, I want an automated job that periodically checks Confluence pages for changes, so that cached artifact data stays up-to-date without manual intervention.

#### Acceptance Criteria

1. THE Confluence_Monitor_Service SHALL execute on a fixed schedule of every 6 hours
2. WHEN the scheduled job runs, THE Confluence_Monitor_Service SHALL iterate over all projects that have at least one Confluence page ID configured (either `confluenceParentPageId` or `confluenceArtifactsPageId` is non-null)
3. WHEN checking a project, THE Confluence_Monitor_Service SHALL resolve the target Confluence page using the same priority rules as the artifacts endpoint (use `confluenceArtifactsPageId` if set, otherwise discover the latest child of `confluenceParentPageId`) and fetch the current page version from the Confluence REST API
4. WHEN the fetched page version matches the stored version in `cached_artifacts`, THE Confluence_Monitor_Service SHALL skip processing and update only the `last_checked_at` timestamp
5. WHEN the fetched page version differs from the stored version, or no `cached_artifacts` entry exists for the project, THE Confluence_Monitor_Service SHALL fetch the full page content, trigger LLM extraction, and upon successful extraction persist the extracted JSON, new page version, `last_checked_at`, and `last_updated_at` to the `cached_artifacts` table
6. IF the Confluence API returns an error or fails to respond within 30 seconds during a check, THEN THE Confluence_Monitor_Service SHALL log the error with the project identifier and continue processing the remaining projects

### Requirement 8: Amazon Bedrock LLM Artifact Extraction

**User Story:** As a system operator, I want an LLM to extract structured artifact data from Confluence page HTML, so that the system provides parsed, searchable artifact information instead of raw page content.

#### Acceptance Criteria

1. WHEN triggered with page content, THE Bedrock_Extractor SHALL send the content to Amazon Bedrock with a prompt requesting structured JSON extraction within a timeout of 30 seconds
2. THE Bedrock_Extractor SHALL use the configured model ID from application properties (defaulting to Claude 3 Haiku)
3. WHEN the LLM returns a valid JSON response containing an array of objects, THE Bedrock_Extractor SHALL parse it into a structured list of artifacts where each artifact contains at minimum a component name and at least one of: version, S3 path, or artifact text
4. IF the LLM returns a response that is not valid JSON or does not contain a JSON array of artifact objects, THEN THE Bedrock_Extractor SHALL treat the response as unparseable, log the error including the raw response, and retain the previous cached data unchanged
5. IF Amazon Bedrock is unavailable due to connection timeout, HTTP error response, or service exception, THEN THE Bedrock_Extractor SHALL log the error and retain the previous cached data unchanged for that project
6. IF the page content exceeds 100,000 characters, THEN THE Bedrock_Extractor SHALL truncate the content to 100,000 characters before sending it to Amazon Bedrock

### Requirement 9: Cached Artifacts API Endpoint

**User Story:** As a frontend developer, I want a cached artifacts endpoint, so that the UI can display artifact information instantly without waiting for live Confluence API calls.

#### Acceptance Criteria

1. WHEN the cached endpoint receives a request with a project ID, THE Backend SHALL return the stored `artifacts_json` from the `cached_artifacts` table along with the `last_updated_at` timestamp
2. IF cached data exists for the project and `last_updated_at` is less than 6 hours before the current time, THEN THE Backend SHALL return the cached response directly without calling the Confluence API
3. IF cached data is missing or `last_updated_at` is 6 hours or more before the current time, THEN THE Backend SHALL fall back to a live Confluence fetch and return the live result
4. IF the project ID does not exist or has no Confluence page ID configured, THEN THE Backend SHALL return an error response indicating the project is not found or not configured for Confluence
5. IF both the cache is unavailable and the live Confluence fetch fails, THEN THE Backend SHALL return an error response indicating that artifact data could not be retrieved
6. THE cached endpoint response SHALL include the `last_updated_at` timestamp so the frontend can determine data freshness

### Requirement 10: Frontend Cache Integration

**User Story:** As a user, I want RC Info to load instantly from cached data when available, so that I don't have to wait for Confluence API calls on every click.

#### Acceptance Criteria

1. WHEN a user clicks "RC Info", THE Frontend SHALL first request data from the cached artifacts endpoint with a timeout of 5 seconds
2. WHEN the cached endpoint returns fresh data (less than 6 hours old based on the `last_updated_at` timestamp), THE Frontend SHALL display the cached artifacts
3. WHEN the cached endpoint returns stale or empty data, THE Frontend SHALL fall back to the live Confluence fetch endpoint
4. IF the cached endpoint request fails or times out, THEN THE Frontend SHALL fall back to the live Confluence fetch endpoint
5. WHEN displaying cached data, THE Frontend SHALL show a "Last updated: X hours ago" indicator with hour-level granularity (e.g., "Last updated: 2 hours ago")

### Requirement 11: AWS Bedrock Configuration

**User Story:** As a system administrator, I want configurable AWS Bedrock settings, so that the LLM feature can be enabled/disabled and tuned without code changes.

#### Acceptance Criteria

1. THE Application SHALL support a `bedrock.region` property defaulting to "us-east-1", overridable via the `AWS_REGION` environment variable
2. THE Application SHALL support a `bedrock.model-id` property defaulting to "anthropic.claude-3-haiku-20240307-v1:0", overridable via the `BEDROCK_MODEL_ID` environment variable
3. THE Application SHALL support a `bedrock.enabled` property defaulting to false, overridable via the `BEDROCK_ENABLED` environment variable
4. WHILE the `bedrock.enabled` property is set to false, THE Confluence_Monitor_Service SHALL skip LLM extraction entirely but SHALL still check page versions and update `last_checked_at`
5. WHILE the `bedrock.enabled` property is set to true, THE Confluence_Monitor_Service SHALL perform LLM extraction when a version change is detected
6. IF `bedrock.enabled` is true but the configured Bedrock model or region is unreachable at runtime, THE Application SHALL log the configuration error and treat the extraction as failed without crashing
