# Requirements Document

## Introduction

This document defines the requirements for transforming the Android Toolkit from a standalone single-machine application into a cloud-agnostic, multi-tenant SaaS product. The current system runs Spring Boot + React + ADB operations on one machine with an embedded database. The new architecture splits responsibilities between a cloud-hosted Server (auth, data, AI analysis, storage) and lightweight local Agents (ADB device operations), supporting three customer segments (Single User, Small Team, Corporation) from a single codebase.

## Glossary

- **Server**: The cloud-hosted Spring Boot application responsible for authentication, tenant management, data persistence, AI analysis, report generation, and file storage coordination.
- **Agent**: A lightweight local application running on a developer's machine that performs ADB operations (logcat streaming, log pulling, app install/uninstall, screenshots) and bridges device data to the Server via WebSocket.
- **Tenant**: An isolated organizational unit within the Server. Each tenant has its own users, projects, devices, logs, and configuration. All tenant data is isolated by a tenant identifier.
- **Subscription_Tier**: The plan level assigned to a tenant that determines feature availability and resource limits. Values: FREE, PRO, ENTERPRISE.
- **Feature_Gate**: A runtime check that enables or disables functionality based on the tenant's Subscription_Tier.
- **Object_Storage**: An S3-compatible blob storage service used for log archives, APK files, screenshots, and analysis reports. Cloud-agnostic (AWS S3, Azure Blob, GCP Cloud Storage, MinIO for on-prem).
- **Tenant_Context**: The runtime-resolved tenant identity attached to every authenticated request, used to scope all data access.
- **SSO_Provider**: An external identity provider (SAML, OIDC, LDAP) used for federated authentication in the ENTERPRISE tier.
- **Audit_Log**: An append-only record of security-relevant actions performed within a tenant (user login, role change, data export, configuration change).

## Requirements

### Requirement 1: Tenant Isolation

**User Story:** As a platform operator, I want all tenant data to be strictly isolated, so that no tenant can access another tenant's data.

#### Acceptance Criteria

1. THE Server SHALL associate every persisted entity with a tenant_id column that references the owning Tenant.
2. WHEN a request is processed, THE Server SHALL resolve the Tenant_Context from the authenticated user's JWT claims before executing any data operation.
3. THE Server SHALL append a tenant_id filter to every database query, ensuring results contain only data belonging to the resolved Tenant_Context.
4. IF a request attempts to access a resource belonging to a different Tenant, THEN THE Server SHALL return an HTTP 403 response and log the violation to the Audit_Log.
5. THE Server SHALL store each tenant's files in a dedicated Object_Storage prefix scoped by tenant_id.

### Requirement 2: Server and Agent Architecture Split

**User Story:** As a developer, I want ADB operations to run on my local machine while all other services run in the cloud, so that I can manage devices without exposing them to the internet.

#### Acceptance Criteria

1. THE Agent SHALL connect to the Server using a persistent WebSocket connection authenticated with a device-scoped JWT token.
2. WHEN the Agent establishes a connection, THE Server SHALL register the Agent's devices under the authenticated user's Tenant_Context.
3. THE Agent SHALL execute ADB commands (logcat streaming, log pulling, app install, app uninstall, screenshot capture) on locally connected Android devices.
4. WHEN the Agent captures logcat data, THE Agent SHALL stream the data to the Server over the WebSocket connection in real time.
5. WHEN the Agent completes a log pull operation, THE Agent SHALL upload the resulting archive to the Server's file upload endpoint.
6. THE Server SHALL handle authentication, tenant management, project management, AI log analysis, report generation, and Object_Storage coordination.
7. IF the WebSocket connection between Agent and Server is interrupted, THEN THE Agent SHALL buffer pending data locally and retry the connection with exponential backoff.

### Requirement 3: Subscription Tier Feature Gating

**User Story:** As a product owner, I want features to be unlocked based on subscription tier, so that the platform can serve free, pro, and enterprise customers from one codebase.

#### Acceptance Criteria

1. THE Server SHALL evaluate the tenant's Subscription_Tier before executing any tier-restricted operation.
2. WHERE the Subscription_Tier is FREE, THE Server SHALL enforce a maximum of 2 registered devices per tenant.
3. WHERE the Subscription_Tier is FREE, THE Server SHALL enforce a maximum of 1 user per tenant.
4. WHERE the Subscription_Tier is FREE, THE Server SHALL limit AI log analysis to 5 executions per calendar month.
5. WHERE the Subscription_Tier is FREE, THE Server SHALL limit Object_Storage usage to 500 MB per tenant.
6. WHERE the Subscription_Tier is PRO, THE Server SHALL allow up to 20 users per tenant.
7. WHERE the Subscription_Tier is PRO, THE Server SHALL allow unlimited registered devices.
8. WHERE the Subscription_Tier is PRO, THE Server SHALL allow unlimited AI log analysis executions.
9. WHERE the Subscription_Tier is PRO, THE Server SHALL limit Object_Storage usage to 50 GB per tenant.
10. WHERE the Subscription_Tier is ENTERPRISE, THE Server SHALL impose no limits on users, devices, AI analysis, or Object_Storage usage.
11. IF a tenant exceeds a tier limit, THEN THE Server SHALL reject the operation with an HTTP 429 response and include the limit details in the response body.

### Requirement 4: Multi-Tenant User and Role Management

**User Story:** As a team admin, I want to invite users to my tenant and assign roles, so that I can control who has access to projects and devices.

#### Acceptance Criteria

1. WHEN an ADMIN user invites a new user, THE Server SHALL create a pending invitation associated with the ADMIN's Tenant.
2. WHEN an invited user accepts the invitation, THE Server SHALL create the user account within the inviting Tenant.
3. THE Server SHALL support the following roles within a Tenant: OWNER, ADMIN, USER.
4. WHEN an OWNER or ADMIN assigns a role to a user, THE Server SHALL persist the role change and record it in the Audit_Log.
5. THE Server SHALL restrict project creation and deletion to users with OWNER or ADMIN roles.
6. THE Server SHALL restrict user invitation and role management to users with OWNER or ADMIN roles.
7. WHERE the Subscription_Tier is ENTERPRISE, THE Server SHALL support SSO_Provider integration for user authentication.

### Requirement 5: Cloud-Agnostic Deployment

**User Story:** As an enterprise customer, I want to deploy the platform on any cloud provider or on-premises, so that I am not locked into a specific vendor.

#### Acceptance Criteria

1. THE Server SHALL be packaged as a Docker container image with all dependencies included.
2. THE Server SHALL use PostgreSQL as the relational database, configurable via environment variables.
3. THE Server SHALL use an S3-compatible API for Object_Storage, configurable via environment variables for endpoint, bucket, access key, and secret key.
4. THE Server SHALL not depend on any vendor-specific cloud service beyond PostgreSQL and S3-compatible storage.
5. WHEN deployed on-premises, THE Server SHALL support MinIO as the Object_Storage backend.
6. THE Server SHALL expose all configuration (database URL, storage endpoint, JWT secret, SMTP settings) through environment variables or a mounted configuration file.

### Requirement 6: Data Migration from Standalone Mode

**User Story:** As an existing user, I want to migrate my standalone data to the cloud platform, so that I retain my projects, logs, and analysis history.

#### Acceptance Criteria

1. THE Server SHALL provide a migration endpoint that accepts an export archive from the standalone application.
2. WHEN a migration archive is uploaded, THE Server SHALL create a new Tenant and import all projects, users, log metadata, and analysis reports into that Tenant.
3. WHEN migrating user accounts, THE Server SHALL preserve usernames and email addresses while requiring password reset on first cloud login.
4. WHEN migrating log archives, THE Server SHALL upload files to the new tenant's Object_Storage prefix and update metadata references.
5. IF a migration archive contains data that conflicts with existing records, THEN THE Server SHALL report the conflicts and skip conflicting records without aborting the migration.

### Requirement 7: Real-Time Logcat Streaming via Agent

**User Story:** As a developer, I want to view real-time logcat output from my devices in the browser, so that I can debug issues without direct ADB access to the device.

#### Acceptance Criteria

1. WHEN a user requests logcat streaming for a device, THE Server SHALL forward the request to the Agent managing that device via the WebSocket connection.
2. WHEN the Agent receives a stream request, THE Agent SHALL start an ADB logcat process for the specified device and stream output lines to the Server.
3. THE Server SHALL relay logcat lines from the Agent to the requesting user's browser session via a STOMP WebSocket topic.
4. WHEN a user stops the logcat stream, THE Server SHALL notify the Agent to terminate the ADB logcat process.
5. THE Server SHALL support multiple concurrent logcat streams across different devices within the same Tenant.
6. WHILE streaming logcat data, THE Agent SHALL apply the same log parsing and token-type detection logic as the current standalone LogcatStreamManager.

### Requirement 8: Project Management with Tenant Scope

**User Story:** As a team admin, I want to manage projects within my tenant, so that team members can organize devices and logs by project.

#### Acceptance Criteria

1. THE Server SHALL scope all project operations (create, read, update, delete) to the authenticated user's Tenant_Context.
2. WHEN a project is created, THE Server SHALL associate the project with the creating user's Tenant and allocate an Object_Storage prefix for the project's files.
3. THE Server SHALL allow users within a Tenant to view all projects belonging to that Tenant.
4. THE Server SHALL restrict project modification and deletion to users with OWNER or ADMIN roles within the Tenant.
5. WHEN a project is deleted, THE Server SHALL archive the project's Object_Storage files rather than permanently deleting them.

### Requirement 9: Audit Logging for Enterprise Tenants

**User Story:** As a security officer, I want an audit trail of all significant actions, so that I can investigate incidents and demonstrate compliance.

#### Acceptance Criteria

1. WHERE the Subscription_Tier is ENTERPRISE, THE Server SHALL record an Audit_Log entry for every authentication event (login, logout, token refresh, failed login).
2. WHERE the Subscription_Tier is ENTERPRISE, THE Server SHALL record an Audit_Log entry for every role or permission change.
3. WHERE the Subscription_Tier is ENTERPRISE, THE Server SHALL record an Audit_Log entry for every data export or migration operation.
4. THE Server SHALL store Audit_Log entries with timestamp, actor user_id, tenant_id, action type, target resource, and outcome (success or failure).
5. THE Server SHALL retain Audit_Log entries for a minimum of 365 days.
6. THE Server SHALL provide an API endpoint for OWNER users to query Audit_Log entries filtered by date range, actor, and action type.

### Requirement 10: Frontend Deployment Flexibility

**User Story:** As a user, I want to access the application through a browser or as a desktop app, so that I can choose the experience that fits my workflow.

#### Acceptance Criteria

1. THE Server SHALL serve the React frontend as static assets accessible via any modern web browser.
2. THE Frontend SHALL authenticate against the Server using the same JWT-based flow regardless of deployment mode (browser or Electron).
3. WHEN deployed as an Electron application, THE Frontend SHALL bundle the Agent process and manage its lifecycle (start on app launch, stop on app close).
4. THE Frontend SHALL detect whether it is running in browser mode or Electron mode and adjust Agent connectivity accordingly (browser mode connects to remote Agent; Electron mode uses local Agent).
5. THE Server SHALL support CORS configuration via environment variables to allow frontend access from any origin.

### Requirement 11: Existing Functionality Preservation

**User Story:** As a current user, I want all existing features to continue working after the architecture change, so that the migration does not reduce my capabilities.

#### Acceptance Criteria

1. THE Server SHALL continue to support device listing, device detail retrieval, and device status monitoring through the Agent.
2. THE Agent SHALL continue to support APK installation and uninstallation on connected devices.
3. THE Agent SHALL continue to support screenshot capture from connected devices.
4. THE Server SHALL continue to support scheduled AI log analysis with configurable cron expressions.
5. THE Server SHALL continue to support bug report templates and event tracking functionality.
6. THE Server SHALL continue to support log collection with ZIP archiving and upload to Object_Storage.
7. WHEN the platform operates in standalone mode (single-user, local deployment), THE Server SHALL function without requiring external PostgreSQL or Object_Storage services by falling back to embedded H2 and local filesystem storage.
