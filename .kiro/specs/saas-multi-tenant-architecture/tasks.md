# Implementation Plan: SaaS Multi-Tenant Architecture

## Overview

This plan transforms the Android Toolkit from a standalone single-machine application into a cloud-agnostic, multi-tenant SaaS product. Implementation proceeds incrementally: core tenant infrastructure first, then feature gating, then Agent split, then migration and audit capabilities. Each step builds on the previous, ensuring no orphaned code.

## Tasks

- [x] 1. Set up project structure and core interfaces
  - [x] 1.1 Create the `agent` Gradle module with basic project structure
    - Add `agent/build.gradle` with Spring Boot WebSocket client dependencies
    - Create package structure: `androidtoolkit/agent/`, `androidtoolkit/agent/connection/`, `androidtoolkit/agent/adb/`, `androidtoolkit/agent/stream/`
    - Create `AgentApplication.java` main class
    - Add `agent` to root `settings.gradle`
    - _Requirements: 2.1, 2.3_

  - [x] 1.2 Create the `core` shared module interfaces and DTOs
    - Define `AgentCommand` sealed interface with records: `StartLogcat`, `StopLogcat`, `PullLogs`, `InstallApk`, `UninstallApp`, `CaptureScreenshot`
    - Define `AgentMessage` sealed interface with records: `LogcatLine`, `DeviceList`, `OperationResult`, `LogArchiveReady`
    - Define `SubscriptionTier` enum (FREE, PRO, ENTERPRISE)
    - Define `TenantRole` enum (OWNER, ADMIN, USER)
    - Define `AuditAction` and `AuditOutcome` enums
    - _Requirements: 2.1, 3.1, 4.3_

  - [x] 1.3 Add new dependencies to `backend/build.gradle`
    - Add PostgreSQL driver (`org.postgresql:postgresql`)
    - Add AWS SDK v2 S3 client (`software.amazon.awssdk:s3`)
    - Add Testcontainers for PostgreSQL and MinIO (test scope)
    - _Requirements: 5.2, 5.3_

  - [x] 1.4 Define `ObjectStorageService` interface in backend
    - Methods: `upload(tenantId, projectPrefix, filename, data, size)`, `download(objectKey)`, `archive(objectKey)`, `getTenantStorageUsage(tenantId)`
    - _Requirements: 1.5, 5.3_

  - [x] 1.5 Define `TierEnforcer` interface in backend
    - Methods: `checkDeviceLimit(tenantId)`, `checkUserLimit(tenantId)`, `checkAnalysisLimit(tenantId)`, `checkStorageLimit(tenantId, additionalBytes)`
    - Define `TierLimitExceededException` extending `RuntimeException` with limit details
    - _Requirements: 3.1, 3.11_

  - [x] 1.6 Define `AuditLogService` interface in backend
    - Methods: `log(action, actorUserId, tenantId, targetResource, outcome, metadata)`, `query(tenantId, filter, pageable)`
    - Define `AuditLogFilter` record with date range, actor, and action type fields
    - _Requirements: 9.4, 9.6_

- [x] 2. Implement tenant context and data isolation
  - [x] 2.1 Implement `TenantContext` ThreadLocal holder
    - Create `TenantContext` class with static `setTenantId`, `getTenantId`, `clear` methods
    - Create `TenantContextInterceptor` (HandlerInterceptor) that extracts `tenant_id` from JWT claims and sets it in `TenantContext`
    - Register interceptor in WebMvc configuration
    - Clear context in `afterCompletion`
    - _Requirements: 1.2_

  - [x] 2.2 Create `Tenant` JPA entity and repository
    - Fields: `id`, `name`, `tier` (SubscriptionTier), `createdAt`, `storagePrefix`
    - Create `TenantRepository` extending `JpaRepository`
    - _Requirements: 1.1_

  - [x] 2.3 Add `tenant_id` column to existing entities
    - Add `tenant_id` FK to `Project`, `LogUploadMetadata`, `AnalysisReport`, `AnalysisJobExecution`
    - Add Hibernate `@FilterDef` and `@Filter` annotations for tenant scoping
    - Create a `TenantScoped` base class or interface with the filter annotations
    - _Requirements: 1.1, 1.3_

  - [x] 2.4 Implement Hibernate tenant filter activation
    - Create `TenantFilterAspect` or `OpenEntityManagerInViewFilter` extension that enables the `tenantFilter` on every session using `TenantContext.getTenantId()`
    - Ensure filter is applied before any repository query executes
    - _Requirements: 1.3_

  - [x] 2.5 Implement cross-tenant access guard
    - Create `TenantAccessGuard` service that validates resource ownership before returning data
    - Return HTTP 403 and log to `AuditLogService` when cross-tenant access is detected
    - _Requirements: 1.4_

  - [ ]* 2.6 Write property test for tenant data isolation
    - **Property 1: Tenant Data Isolation**
    - Generate random tenant IDs and entity sets; verify queries only return entities matching the active TenantContext
    - **Validates: Requirements 1.3, 8.1, 8.3**

  - [ ]* 2.7 Write property test for cross-tenant access rejection
    - **Property 2: Cross-Tenant Access Rejection**
    - Generate random tenant pairs (A ≠ B); verify accessing tenant A's resource from tenant B's context returns 403
    - **Validates: Requirements 1.4**

- [x] 3. Checkpoint - Ensure tenant isolation compiles and tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Implement Object Storage service
  - [x] 4.1 Implement `S3ObjectStorageService`
    - Implement `ObjectStorageService` interface using AWS SDK v2 S3 client
    - Configure via environment variables: `STORAGE_ENDPOINT`, `STORAGE_BUCKET`, `STORAGE_ACCESS_KEY`, `STORAGE_SECRET_KEY`
    - Scope all keys with tenant prefix: `tenants/{tenantId}/projects/{projectPrefix}/{filename}`
    - Implement `getTenantStorageUsage` using S3 ListObjects with prefix
    - _Requirements: 1.5, 5.3, 5.5_

  - [x] 4.2 Implement `LocalFilesystemStorageService` for standalone mode
    - Implement `ObjectStorageService` interface using local filesystem
    - Store files under configurable base directory
    - Activate when `DEPLOYMENT_MODE=standalone`
    - _Requirements: 11.7_

  - [x] 4.3 Create storage configuration class
    - Create `StorageConfig` that conditionally creates `S3ObjectStorageService` or `LocalFilesystemStorageService` bean based on `DEPLOYMENT_MODE`
    - _Requirements: 5.3, 5.4, 11.7_

  - [ ]* 4.4 Write property test for tenant-scoped storage keys
    - **Property 3: Tenant-Scoped Storage Keys**
    - Generate random tenant IDs and filenames; verify all storage keys contain the tenant's prefix and no two tenants share a prefix
    - **Validates: Requirements 1.5, 8.2**

- [x] 5. Implement subscription tier enforcement
  - [x] 5.1 Implement `TierEnforcerImpl` service
    - Implement `TierEnforcer` interface with tier limit checks
    - Query current usage counts from repositories (device count, user count, monthly analysis count, storage bytes)
    - Compare against tier limits: FREE (1 user, 2 devices, 5 analyses/month, 500MB), PRO (20 users, unlimited devices, unlimited analyses, 50GB), ENTERPRISE (no limits)
    - Throw `TierLimitExceededException` when limits exceeded
    - _Requirements: 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 3.10_

  - [x] 5.2 Create `TierLimitExceptionHandler` in `GlobalExceptionHandler`
    - Map `TierLimitExceededException` to HTTP 429 response with limit details in body
    - Response format: `{ "error": "TIER_LIMIT_EXCEEDED", "message": "...", "details": { "limit", "current", "tier", "resource" } }`
    - _Requirements: 3.11_

  - [x] 5.3 Integrate tier checks into existing controllers
    - Add `tierEnforcer.checkDeviceLimit()` before device registration
    - Add `tierEnforcer.checkUserLimit()` before user invitation
    - Add `tierEnforcer.checkAnalysisLimit()` before AI analysis execution
    - Add `tierEnforcer.checkStorageLimit()` before file uploads
    - _Requirements: 3.1_

  - [ ]* 5.4 Write property test for tier limit enforcement
    - **Property 4: Tier Limit Enforcement**
    - Generate random tiers and usage levels; verify operations exceeding limits are rejected and operations within limits are allowed
    - **Validates: Requirements 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 3.10**

- [x] 6. Implement user and role management
  - [x] 6.1 Create `TenantMembership` entity and repository
    - Fields: `id`, `tenantId`, `userId`, `role` (TenantRole), `joinedAt`
    - Create `TenantMembershipRepository` with queries for finding members by tenant and checking role
    - _Requirements: 4.3_

  - [x] 6.2 Create `PendingInvite` entity and repository
    - Fields: `id`, `tenantId`, `email`, `role`, `inviteToken`, `invitedBy`, `createdAt`, `expiresAt`, `accepted`
    - _Requirements: 4.1_

  - [x] 6.3 Implement `UserManagementService`
    - `inviteUser(tenantId, email, role)`: Create pending invitation, enforce user limit via TierEnforcer
    - `acceptInvitation(inviteToken)`: Create user account within the inviting tenant
    - `assignRole(tenantId, userId, newRole)`: Update role, record in AuditLog
    - `listMembers(tenantId)`: Return all tenant members
    - _Requirements: 4.1, 4.2, 4.4_

  - [x] 6.4 Implement role-based access control annotations
    - Create `@RequiresRole(TenantRole.ADMIN)` annotation and aspect
    - Apply to project creation/deletion endpoints
    - Apply to user invitation and role management endpoints
    - _Requirements: 4.5, 4.6_

  - [ ]* 6.5 Write property test for role-based access control
    - **Property 5: Role-Based Access Control**
    - Generate random roles and operations; verify USER role is rejected for privileged operations and OWNER/ADMIN are allowed
    - **Validates: Requirements 4.5, 4.6, 8.4**

  - [ ]* 6.6 Write property test for invitation tenant scoping
    - **Property 6: Invitation Tenant Scoping**
    - Generate random tenants and invitations; verify invitations are associated with the correct tenant and accepted users join that tenant
    - **Validates: Requirements 4.1, 4.2**

- [x] 7. Checkpoint - Ensure tier enforcement and RBAC tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Implement Agent WebSocket communication
  - [x] 8.1 Implement `AgentConnectionManager` on the Server
    - Track connected agents in a concurrent map: `agentId → (tenantId, userId, session)`
    - `registerAgent`: Store session, register devices from initial `DeviceList` message
    - `unregisterAgent`: Mark devices as OFFLINE, remove session
    - `sendToAgent`: Serialize `AgentCommand` and send via WebSocket session
    - `findAgentForDevice`: Look up which agent manages a given device serial within a tenant
    - _Requirements: 2.2, 2.6_

  - [x] 8.2 Create `AgentWebSocketHandler` on the Server
    - Extend `TextWebSocketHandler`
    - Authenticate Agent JWT on connection (device-scoped token)
    - Deserialize incoming `AgentMessage` types and route to appropriate services
    - Handle `LogcatLine`: relay to STOMP topic `/topic/logcat/{serial}`
    - Handle `DeviceList`: update device registry
    - Handle `OperationResult`: complete pending operation futures
    - Handle `LogArchiveReady`: trigger file upload flow
    - _Requirements: 2.1, 2.4, 7.3_

  - [x] 8.3 Implement Agent WebSocket client with reconnection
    - Create `ServerConnection` class in agent module using Spring WebSocket client
    - Authenticate with device-scoped JWT
    - Implement exponential backoff reconnection on disconnect (initial 1s, max 60s, factor 2x)
    - Buffer pending messages during disconnection
    - _Requirements: 2.1, 2.7_

  - [x] 8.4 Implement Agent ADB command execution
    - Create `AdbCommandExecutor` in agent module
    - Handle `StartLogcat`: start ADB logcat process, stream lines as `LogcatLine` messages
    - Handle `StopLogcat`: terminate ADB logcat process
    - Handle `PullLogs`: execute adb pull, send `LogArchiveReady` when complete
    - Handle `InstallApk`: download APK from URL, execute adb install
    - Handle `UninstallApp`: execute adb uninstall
    - Handle `CaptureScreenshot`: execute adb screencap, upload result
    - _Requirements: 2.3, 7.2, 11.2, 11.3_

  - [x] 8.5 Move logcat parsing logic to Agent
    - Copy `LogcatStreamManager` parsing logic to `agent/src/androidtoolkit/agent/stream/`
    - Ensure same token-type detection and field parsing as current standalone implementation
    - Agent parses lines locally and sends structured `LogcatLine` messages to Server
    - _Requirements: 7.6_

  - [ ]* 8.6 Write property test for Agent reconnection backoff
    - **Property 11: Agent Reconnection Backoff**
    - Generate random sequences of N consecutive failures; verify each successive retry delay is ≥ the previous delay
    - **Validates: Requirements 2.7**

  - [ ]* 8.7 Write property test for logcat parser equivalence
    - **Property 12: Logcat Parser Behavioral Equivalence**
    - Generate random logcat lines; verify Agent parser produces same output as standalone LogcatStreamManager parser
    - **Validates: Requirements 7.6**

- [x] 9. Implement real-time logcat streaming end-to-end
  - [x] 9.1 Create logcat streaming REST and STOMP endpoints on Server
    - POST `/api/devices/{serial}/logcat/start`: Forward `StartLogcat` command to Agent via `AgentConnectionManager`
    - POST `/api/devices/{serial}/logcat/stop`: Forward `StopLogcat` command to Agent
    - STOMP topic `/topic/logcat/{serial}`: Relay `LogcatLine` messages from Agent to browser
    - Validate device belongs to requesting user's tenant
    - _Requirements: 7.1, 7.3, 7.4_

  - [x] 9.2 Support multiple concurrent logcat streams
    - Track active streams per tenant in `AgentConnectionManager`
    - Allow multiple devices streaming simultaneously within same tenant
    - Clean up streams when Agent disconnects
    - _Requirements: 7.5_

- [x] 10. Implement audit logging
  - [x] 10.1 Create `AuditLogEntry` entity and repository
    - Fields: `id`, `tenantId`, `actorUserId`, `timestamp`, `actionType`, `targetResource`, `outcome`, `metadata` (JSON string)
    - Create `AuditLogEntryRepository` with query methods for filtering by date range, actor, and action type
    - _Requirements: 9.4, 9.5_

  - [x] 10.2 Implement `AuditLogServiceImpl`
    - Check tenant tier before recording: only log for ENTERPRISE tenants
    - Record entries for: authentication events, role/permission changes, data export/migration operations
    - Implement `query` method with pagination and filtering
    - _Requirements: 9.1, 9.2, 9.3_

  - [x] 10.3 Create audit log query API endpoint
    - GET `/api/audit-logs`: Query audit entries with filters (dateFrom, dateTo, actorId, actionType)
    - Restrict access to OWNER role
    - Return paginated results
    - _Requirements: 9.6_

  - [x] 10.4 Integrate audit logging into existing services
    - Log authentication events (login, logout, token refresh, failed login) in `AuthController`
    - Log role changes in `UserManagementService`
    - Log cross-tenant access violations in `TenantAccessGuard`
    - Log data export and migration operations
    - _Requirements: 9.1, 9.2, 9.3_

  - [ ]* 10.5 Write property test for audit log completeness
    - **Property 9: Audit Log Completeness (Enterprise Only)**
    - Generate random security-relevant actions and tiers; verify ENTERPRISE tenants get audit entries with all required fields and non-ENTERPRISE tenants get none
    - **Validates: Requirements 9.1, 9.2, 9.3, 9.4**

  - [ ]* 10.6 Write property test for audit log query filtering
    - **Property 10: Audit Log Query Filtering**
    - Generate random audit entries and filter combinations; verify query returns exactly matching entries
    - **Validates: Requirements 9.6**

- [x] 11. Checkpoint - Ensure Agent communication and audit logging tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 12. Implement data migration from standalone mode
  - [x] 12.1 Create migration endpoint and `MigrationServiceImpl`
    - POST `/api/migration/import`: Accept export archive (ZIP), create new Tenant, import data
    - Parse archive: extract projects, users, log metadata, analysis reports, log files
    - Create tenant with appropriate tier
    - Import users: preserve usernames and emails, mark for password reset
    - Import projects: create with new tenant_id, allocate storage prefix
    - Upload log files to new tenant's Object Storage prefix, update metadata references
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

  - [x] 12.2 Implement conflict detection and partial import
    - Detect conflicts: duplicate usernames/emails, duplicate project names
    - Skip conflicting records without aborting
    - Return `MigrationResult` with counts and conflict list
    - Return HTTP 207 for partial success with conflicts
    - _Requirements: 6.5_

  - [ ]* 12.3 Write property test for migration data preservation
    - **Property 7: Migration Data Preservation (Round-Trip)**
    - Generate random standalone data exports; verify all project names, usernames, emails are preserved and files stored under new tenant prefix
    - **Validates: Requirements 6.2, 6.3, 6.4**

  - [ ]* 12.4 Write property test for migration conflict resilience
    - **Property 8: Migration Conflict Resilience**
    - Generate random archives with conflicting records; verify migration completes, non-conflicting records are imported, and conflicts are reported
    - **Validates: Requirements 6.5**

- [x] 13. Implement project management with tenant scope
  - [x] 13.1 Update `ProjectController` for multi-tenant operations
    - Scope all CRUD operations to `TenantContext`
    - On create: associate with tenant, allocate Object Storage prefix
    - On delete: archive Object Storage files instead of permanent deletion
    - Enforce OWNER/ADMIN role for create/update/delete via `@RequiresRole`
    - Allow all tenant users to read projects
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 14. Implement cloud-agnostic deployment configuration
  - [x] 14.1 Create multi-profile application configuration
    - Create `application-saas.properties` for cloud deployment (PostgreSQL, S3)
    - Update `application.properties` for standalone mode (H2, local filesystem)
    - Externalize all config via environment variables: `DATABASE_URL`, `STORAGE_ENDPOINT`, `STORAGE_BUCKET`, `STORAGE_ACCESS_KEY`, `STORAGE_SECRET_KEY`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `SMTP_HOST`, `SSO_PROVIDER_URL`
    - _Requirements: 5.2, 5.3, 5.6_

  - [x] 14.2 Create Dockerfile for Server
    - Multi-stage build: Gradle build → JRE 21 runtime image
    - Include all dependencies
    - Expose port 8080
    - Configure via environment variables
    - _Requirements: 5.1, 5.4_

  - [x] 14.3 Implement CORS configuration via environment variables
    - Update `SecurityConfig` to read `CORS_ALLOWED_ORIGINS` from environment
    - Support comma-separated list of allowed origins
    - Default to `*` for development
    - _Requirements: 10.5_

  - [x] 14.4 Implement standalone fallback mode
    - When `DEPLOYMENT_MODE=standalone`: use H2 embedded database, local filesystem storage, skip tenant resolution (single implicit tenant)
    - Ensure all existing functionality works without PostgreSQL or S3
    - _Requirements: 11.7_

- [x] 15. Implement SSO integration for Enterprise tier
  - [x] 15.1 Add OIDC/SAML authentication support
    - Add Spring Security OAuth2 client dependency
    - Configure OIDC provider via `SSO_PROVIDER_URL` environment variable
    - Gate SSO behind ENTERPRISE tier check
    - Map SSO user attributes to local user accounts within tenant
    - _Requirements: 4.7_

- [x] 16. Implement frontend deployment flexibility
  - [x] 16.1 Update frontend authentication for multi-deployment modes
    - Ensure JWT-based auth flow works identically in browser and Electron modes
    - Detect runtime mode (browser vs Electron) and adjust Agent connectivity
    - Browser mode: connect to remote Agent via Server relay
    - Electron mode: manage local Agent process lifecycle (start on launch, stop on close)
    - _Requirements: 10.2, 10.3, 10.4_

- [x] 17. Preserve existing functionality
  - [x] 17.1 Ensure device operations work through Agent relay
    - Verify device listing, detail retrieval, and status monitoring route through Agent
    - Verify APK install/uninstall commands route through Agent
    - Verify screenshot capture routes through Agent
    - _Requirements: 11.1, 11.2, 11.3_

  - [x] 17.2 Update scheduled AI analysis for multi-tenant context
    - Ensure scheduled analysis jobs run with correct tenant context
    - Track monthly execution count per tenant for tier enforcement
    - Preserve configurable cron expressions
    - _Requirements: 11.4_

  - [x] 17.3 Update log collection for Object Storage
    - Modify log collection to upload ZIP archives to Object Storage instead of local filesystem
    - Preserve existing ZIP archiving logic
    - Update metadata references to use Object Storage keys
    - _Requirements: 11.6_

- [x] 18. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document (jqwik, 100+ iterations each)
- Unit tests validate specific examples and edge cases
- The project uses Java 21, Spring Boot 3.4, and jqwik 1.9.1 for property-based testing
- All property tests should be tagged with `@Tag("Feature: saas-multi-tenant-architecture, Property N: <title>")`

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3"] },
    { "id": 1, "tasks": ["1.4", "1.5", "1.6"] },
    { "id": 2, "tasks": ["2.1", "2.2"] },
    { "id": 3, "tasks": ["2.3", "6.1", "6.2"] },
    { "id": 4, "tasks": ["2.4", "2.5", "4.1", "4.2"] },
    { "id": 5, "tasks": ["2.6", "2.7", "4.3", "4.4", "5.1"] },
    { "id": 6, "tasks": ["5.2", "5.3", "6.3"] },
    { "id": 7, "tasks": ["5.4", "6.4", "6.5", "6.6"] },
    { "id": 8, "tasks": ["8.1", "8.3", "8.5"] },
    { "id": 9, "tasks": ["8.2", "8.4", "10.1"] },
    { "id": 10, "tasks": ["8.6", "8.7", "9.1", "10.2"] },
    { "id": 11, "tasks": ["9.2", "10.3", "10.4"] },
    { "id": 12, "tasks": ["10.5", "10.6", "12.1"] },
    { "id": 13, "tasks": ["12.2", "12.3", "12.4", "13.1"] },
    { "id": 14, "tasks": ["14.1", "14.2", "14.3", "14.4"] },
    { "id": 15, "tasks": ["15.1", "16.1"] },
    { "id": 16, "tasks": ["17.1", "17.2", "17.3"] }
  ]
}
```
