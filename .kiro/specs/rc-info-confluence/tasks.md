# Implementation Plan: RC Info from Confluence (Admin Config + LLM Monitoring)

## Overview

This plan implements two enhancements to the RC Info feature:
1. **Part 1** — Admin-configurable Confluence page IDs per project (DB schema, entity, DTOs, service, controller, admin UI, frontend cleanup)
2. **Part 2** — Amazon Bedrock LLM periodic job (cached_artifacts table, monitor service, Bedrock extractor, cached endpoint, frontend cache integration, AWS config)

Part 1 is implemented first for immediate value; Part 2 builds on it with the LLM monitoring layer.

## Tasks

- [ ] 1. Database schema and Project entity updates
  - [ ] 1.1 Add Confluence page ID columns to the Project entity
    - Add `confluenceParentPageId` (VARCHAR 50, nullable) and `confluenceArtifactsPageId` (VARCHAR 50, nullable) fields to `Project.java`
    - Add `@Column(length = 50)` JPA annotations
    - Create a Flyway/Liquibase migration SQL file to ALTER the `projects` table adding both columns
    - _Requirements: 1.1, 1.2, 1.5_

  - [ ] 1.2 Update ProjectRequest DTO
    - Add nullable `confluenceParentPageId` and `confluenceArtifactsPageId` String fields to `ProjectRequest.java`
    - Add `@Size(max = 50)` validation annotation on both fields
    - _Requirements: 2.1, 2.5_

  - [ ] 1.3 Update ProjectResponse and ResolvedProjectResponse DTOs
    - Add nullable `confluenceParentPageId` and `confluenceArtifactsPageId` String fields to `ProjectResponse.java`
    - Add the same fields to `ResolvedProjectResponse.java`
    - _Requirements: 2.2, 2.3, 2.4_

  - [ ] 1.4 Update ProjectService create/update methods
    - Modify `ProjectService.create()` to map Confluence page ID fields from request to entity
    - Modify `ProjectService.update()` to map Confluence page ID fields, treating empty strings as null
    - Ensure null handling: if only one field is provided, the other stays null
    - _Requirements: 1.3, 1.4, 1.6, 1.7_

  - [ ]* 1.5 Write unit tests for ProjectService Confluence field handling
    - Test create with both page IDs set
    - Test update with one page ID, other null
    - Test empty string treated as null
    - Test round-trip persistence of page IDs
    - _Requirements: 1.3, 1.4, 1.6, 1.7_
    - **Property 1: Page ID Persistence Round-Trip**
    - **Validates: Requirements 1.3, 1.4, 2.4**

- [ ] 2. Backend artifacts endpoint refactoring
  - [ ] 2.1 Update `/api/confluence/artifacts` endpoint to read page IDs from DB
    - Modify the existing artifacts controller endpoint to accept `projectId` as a request parameter
    - Look up the project by ID from the database
    - If `confluenceArtifactsPageId` is set, use it directly (priority override)
    - If only `confluenceParentPageId` is set, fetch children and pick the latest
    - Return appropriate error responses for: project not found, no page IDs configured, no children found, Confluence unavailable
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8_

  - [ ]* 2.2 Write unit tests for dynamic Confluence resolution logic
    - Test priority: artifactsPageId takes precedence over parentPageId
    - Test parent page child discovery selects most recent child
    - Test error cases: project not found, no config, no children, Confluence error
    - _Requirements: 4.2, 4.3, 4.5, 4.6, 4.7, 4.8_
    - **Property 2: Artifacts Page ID Priority Override**
    - **Validates: Requirements 4.2, 4.4**
    - **Property 3: Parent Page Latest Child Selection**
    - **Validates: Requirements 4.3**

- [ ] 3. Admin Panel UI for Confluence page IDs
  - [ ] 3.1 Add Confluence page ID input fields to AdminProjectsPanel
    - Add text input field labeled "Confluence Parent Page ID" with maxLength=50
    - Add text input field labeled "Confluence Artifacts Page ID" with maxLength=50
    - Pre-populate fields with current values from the project response
    - Wire save handler to include new fields in the project update request
    - Handle save success (show confirmation) and error (show error, preserve values)
    - Send null when fields are cleared
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [ ] 4. Remove hardcoded frontend page IDs
  - [ ] 4.1 Remove `RC_PARENT_PAGES` map and update ProjectQuickAccess
    - Delete the hardcoded `RC_PARENT_PAGES` constant/map from the frontend code
    - Update `ProjectQuickAccess` (or equivalent component) to read page IDs from `backendProjects` fetched via API
    - Update the "RC Info" click handler to call `/api/confluence/artifacts?projectId={id}` using the backend project's numeric ID
    - Add error handling for: project not found in backend, no Confluence page configured
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 5. Checkpoint - Part 1 complete
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 6. Cached artifacts database table and entity
  - [ ] 6.1 Create `cached_artifacts` table and JPA entity
    - Create a Flyway/Liquibase migration SQL file for the `cached_artifacts` table with columns: `id` (BIGINT AUTO_INCREMENT PK), `project_id` (BIGINT NOT NULL, FK to projects, UNIQUE), `confluence_page_id` (VARCHAR 50), `confluence_page_version` (INT), `page_title` (VARCHAR 500), `artifacts_json` (TEXT), `last_checked_at` (TIMESTAMP), `last_updated_at` (TIMESTAMP), `llm_model` (VARCHAR 100)
    - Create `CachedArtifact.java` JPA entity class with all fields mapped
    - Create `CachedArtifactRepository.java` Spring Data JPA repository with `findByProjectId(Long projectId)` method
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

  - [ ]* 6.2 Write unit test for cached_artifacts unique constraint
    - Test that inserting two entries for the same project_id violates the unique constraint
    - _Requirements: 6.2_
    - **Property 4: Cache Uniqueness Per Project**
    - **Validates: Requirements 6.2**

- [ ] 7. AWS Bedrock configuration
  - [ ] 7.1 Add AWS Bedrock dependency and configuration properties
    - Add AWS SDK for Bedrock Runtime dependency to `build.gradle`
    - Add configuration properties class (`BedrockProperties.java`) with `region`, `modelId`, and `enabled` fields
    - Add default values in `application.properties`: `bedrock.region=${AWS_REGION:us-east-1}`, `bedrock.model-id=${BEDROCK_MODEL_ID:anthropic.claude-3-haiku-20240307-v1:0}`, `bedrock.enabled=${BEDROCK_ENABLED:false}`
    - _Requirements: 11.1, 11.2, 11.3_

- [ ] 8. Bedrock Artifact Extractor service
  - [ ] 8.1 Create `BedrockArtifactExtractor` service
    - Create service class that calls Amazon Bedrock with Confluence page HTML content
    - Use the configured model ID from `BedrockProperties`
    - Implement the prompt template requesting structured JSON extraction (component, spVersion, version, s3Paths, artifactText)
    - Set a 30-second timeout for the Bedrock API call
    - Parse the LLM response into a `List<ComponentArtifact>` (or equivalent structured type)
    - If response is not valid JSON or not a JSON array, log the error and return empty/null to signal failure
    - If page content exceeds 100,000 characters, truncate before sending
    - Handle Bedrock unavailability (timeout, HTTP error, service exception) by logging and returning failure signal
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6_

  - [ ]* 8.2 Write unit tests for BedrockArtifactExtractor
    - Test successful JSON parsing from valid LLM response
    - Test graceful handling of malformed/non-JSON response
    - Test content truncation at 100,000 characters
    - Test timeout/error handling
    - _Requirements: 8.3, 8.4, 8.5, 8.6_
    - **Property 8: LLM Response Parsing Safety**
    - **Validates: Requirements 8.3, 8.4**

- [ ] 9. Confluence Monitor scheduled job
  - [ ] 9.1 Create `ConfluenceMonitorService` with @Scheduled
    - Create service class with `@Scheduled(fixedDelay = 6 * 60 * 60 * 1000)` annotation
    - Iterate all projects where at least one Confluence page ID is non-null
    - For each project, resolve the target page using the same priority rules (artifactsPageId > parentPageId + child discovery)
    - Fetch current page version from Confluence REST API (`/rest/api/content/{id}?expand=version`)
    - If version matches stored version: update only `last_checked_at`, skip extraction
    - If version differs or no cached entry exists: fetch full page content, call BedrockArtifactExtractor (only if `bedrock.enabled` is true), persist results
    - If `bedrock.enabled` is false: still check versions and update `last_checked_at`, but skip LLM extraction
    - Wrap each project's processing in try-catch so one failure doesn't stop others
    - Log errors with project identifier on Confluence API failures
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 11.4, 11.5, 11.6_

  - [ ]* 9.2 Write unit tests for ConfluenceMonitorService
    - Test that only projects with Confluence config are processed
    - Test version match skips extraction, updates last_checked_at
    - Test version mismatch triggers extraction and persists results
    - Test that bedrock.enabled=false skips LLM extraction but still checks versions
    - Test failure resilience: one project failure doesn't stop others
    - _Requirements: 7.2, 7.4, 7.5, 7.6, 11.4_
    - **Property 5: Monitor Processes Only Configured Projects**
    - **Validates: Requirements 7.2**
    - **Property 6: Version Change Triggers Extraction**
    - **Validates: Requirements 7.4, 7.5**
    - **Property 7: Monitor Resilience on Failure**
    - **Validates: Requirements 7.6**
    - **Property 11: Bedrock Disabled Skips Extraction**
    - **Validates: Requirements 11.4**

- [ ] 10. Cached artifacts API endpoint
  - [ ] 10.1 Create `GET /api/confluence/artifacts/cached` endpoint
    - Accept `projectId` as a request parameter
    - Look up `CachedArtifact` by project ID
    - If cached data exists and `last_updated_at` is less than 6 hours old: return cached `artifacts_json` with `last_updated_at` timestamp
    - If cached data is missing or stale (>= 6 hours): fall back to live Confluence fetch and return live result
    - Return error response if project not found or not configured for Confluence
    - Return error response if both cache and live fetch fail
    - Include `last_updated_at` in the response payload
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6_

  - [ ]* 10.2 Write unit tests for cached artifacts endpoint
    - Test fresh cache returns cached data directly
    - Test stale cache falls back to live fetch
    - Test missing cache falls back to live fetch
    - Test error cases: project not found, no config, both cache and live fail
    - _Requirements: 9.2, 9.3, 9.4, 9.5_
    - **Property 9: Cache Freshness Determination**
    - **Validates: Requirements 9.2, 9.3**

- [ ] 11. Frontend cache integration
  - [ ] 11.1 Update frontend RC Info flow to use cached endpoint first
    - When user clicks "RC Info", first call `/api/confluence/artifacts/cached?projectId={id}` with a 5-second timeout
    - If cached response is fresh (< 6 hours old): display the cached artifacts
    - If cached response is stale or empty: fall back to live `/api/confluence/artifacts?projectId={id}` endpoint
    - If cached request fails or times out: fall back to live endpoint
    - Display "Last updated: X hours ago" indicator when showing cached data (hour-level granularity)
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

- [ ] 12. Final checkpoint - All tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Part 1 (tasks 1-5) should be completed and verified before starting Part 2 (tasks 6-12)
- The backend uses Java with Spring Boot; the frontend uses JavaScript/TypeScript
- AWS Bedrock integration requires proper IAM credentials configured at runtime

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2", "1.3"] },
    { "id": 2, "tasks": ["1.4"] },
    { "id": 3, "tasks": ["1.5", "2.1"] },
    { "id": 4, "tasks": ["2.2", "3.1"] },
    { "id": 5, "tasks": ["4.1"] },
    { "id": 6, "tasks": ["6.1", "7.1"] },
    { "id": 7, "tasks": ["6.2", "8.1"] },
    { "id": 8, "tasks": ["8.2", "9.1"] },
    { "id": 9, "tasks": ["9.2", "10.1"] },
    { "id": 10, "tasks": ["10.2", "11.1"] }
  ]
}
```
