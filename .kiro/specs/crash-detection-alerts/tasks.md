# Implementation Plan: Crash Detection Alerts

## Overview

This plan implements real-time crash detection and alerting for the Android Toolkit. The implementation follows the existing Spring Boot service layer patterns, using JPA for persistence, ObjectStorageService for file storage, STOMP WebSocket for real-time UI updates, and React for frontend components. Tasks are ordered to build foundational components first (domain models, detection engine), then layer on persistence, notifications, and UI.

## Tasks

- [x] 1. Define domain models, enums, and core interfaces
  - [x] 1.1 Create SeverityLevel enum and NotificationPreference enum
    - Create `SeverityLevel.java` with values FATAL, ANR, WARNING
    - Create `NotificationPreference.java` with values BROWSER, IN_APP, BOTH, NONE
    - _Requirements: 1.3, 8.1_

  - [x] 1.2 Create CrashEvent record and CrashEventFilter record
    - Create `CrashEvent.java` record with fields: id, timestamp, deviceSerial, deviceName, packageName, crashType, severity, stackTraceSnippet, crashLogPath, projectId, tenantId
    - Create `CrashEventFilter.java` record with fields: deviceSerial, fromDate, toDate, severity, crashType, acknowledged
    - Create `DailyCrashCount.java` record with fields: date, groupKey, count
    - _Requirements: 1.7, 7.3, 7.5_

  - [x] 1.3 Create JPA entities: CrashEventEntity, AlertConfigurationEntity, CrashPatternEntity
    - Create `CrashEventEntity.java` with all fields, indexes, and tenant filter annotation
    - Create `AlertConfigurationEntity.java` with project association and default values
    - Create `CrashPatternEntity.java` with regex, severity, isDefault, and enabled fields
    - _Requirements: 6.1, 6.2, 6.3, 8.1, 9.1_

  - [x] 1.4 Create JPA repositories for CrashEventEntity, AlertConfigurationEntity, and CrashPatternEntity
    - Create `CrashEventRepository.java` with custom query methods for filtering and aggregation
    - Create `AlertConfigurationRepository.java` with findByProjectId
    - Create `CrashPatternRepository.java` with findByProjectIdAndEnabled
    - _Requirements: 6.1, 7.1, 7.3, 8.2_

- [x] 2. Implement LineBuffer and CrashDetector core logic
  - [x] 2.1 Implement LineBuffer class
    - Create `LineBuffer.java` with configurable capacity, add(), snapshot(), size(), and getCapacity() methods
    - Use ArrayDeque internally with eviction of oldest lines when capacity is reached
    - _Requirements: 2.1_

  - [x]* 2.2 Write property test for LineBuffer (Property 6)
    - **Property 6: Rolling buffer invariant**
    - Test that for any sequence of N lines added to a buffer with capacity C, the buffer contains exactly min(N, C) lines and they are the most recent lines in insertion order
    - **Validates: Requirements 2.1**

  - [x] 2.3 Implement CrashDetector service
    - Create `CrashDetector.java` with analyze(), isDuplicate(), extractPackageName(), and classifySeverity() methods
    - Implement regex-based pattern matching against active patterns for the device's project
    - Implement 5-second deduplication window per device using a ConcurrentHashMap of last crash timestamps
    - Implement package name extraction from logcat line context
    - Implement severity classification based on matched pattern
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_

  - [x]* 2.4 Write property test for crash detection severity classification (Property 1)
    - **Property 1: Crash detection with correct severity classification**
    - Test that for any logcat line containing a registered crash pattern, the CrashDetector emits a CrashEvent with correct severity
    - **Validates: Requirements 1.1, 1.3**

  - [x]* 2.5 Write property test for package name extraction (Property 2)
    - **Property 2: Package name extraction**
    - Test that for any logcat crash line containing a package name in standard Android format, the CrashDetector extracts it correctly
    - **Validates: Requirements 1.4**

  - [x]* 2.6 Write property test for stack trace snippet bounded to 20 lines (Property 3)
    - **Property 3: Stack trace snippet bounded to 20 lines**
    - Test that for any crash detection followed by N subsequent lines, the snippet contains at most 20 lines
    - **Validates: Requirements 1.5**

  - [x]* 2.7 Write property test for deduplication (Property 4)
    - **Property 4: Crash event deduplication within time window**
    - Test that for any sequence of crash pattern matches on the same device within 5 seconds, exactly one CrashEvent is emitted
    - **Validates: Requirements 1.6**

  - [x]* 2.8 Write property test for CrashEvent serialization round-trip (Property 5)
    - **Property 5: CrashEvent serialization round-trip**
    - Test that for any valid CrashEvent, serializing to JSON and deserializing back produces an equivalent CrashEvent
    - **Validates: Requirements 1.7**

- [~] 3. Checkpoint - Core detection engine
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Implement AlertConfigurationService
  - [x] 4.1 Implement AlertConfigurationService
    - Create `AlertConfigurationService.java` with getForProject(), update(), addCustomPattern(), removeCustomPattern(), getActivePatterns(), validateRegex(), and validateBufferSize() methods
    - Return default configuration when no explicit config exists for a project
    - Validate buffer size is between 50 and 2000 inclusive
    - Validate regex compiles without errors before saving
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 9.1, 9.2, 9.3, 9.4, 9.5_

  - [x]* 4.2 Write property test for buffer size validation (Property 15)
    - **Property 15: Buffer size validation**
    - Test that for any integer value, buffer size validation accepts values in [50, 2000] and rejects all others
    - **Validates: Requirements 8.4**

  - [x]* 4.3 Write property test for regex pattern validation (Property 16)
    - **Property 16: Regex pattern validation**
    - Test that for any string, regex validation accepts valid Java regex patterns and rejects invalid ones with descriptive errors
    - **Validates: Requirements 9.2**

  - [x]* 4.4 Write property test for custom crash pattern round-trip (Property 17)
    - **Property 17: Custom crash pattern round-trip**
    - Test that for any valid regex string and severity level, adding a custom pattern then retrieving the list includes the added pattern with original values preserved
    - **Validates: Requirements 9.6**

- [x] 5. Implement CrashLogCaptureService
  - [x] 5.1 Implement CrashLogCaptureService
    - Create `CrashLogCaptureService.java` with captureCrashLog(), formatCrashLog(), and buildStoragePath() methods
    - Format crash log with metadata header (timestamp, device serial, package name, crash type, severity)
    - Upload to ObjectStorageService with retry logic (3 retries, exponential backoff)
    - Implement local fallback storage at `data/crash-logs-fallback/` when all retries fail
    - Construct storage path as `crash-logs/{tenantId}/{projectId}/{deviceSerial}/{timestamp}.log`
    - _Requirements: 2.2, 2.3, 2.4, 2.5, 2.6_

  - [x]* 5.2 Write property test for crash log capture completeness (Property 7)
    - **Property 7: Crash log capture completeness**
    - Test that for any CrashEvent with non-empty buffer and post-crash lines, the formatted log contains metadata header followed by buffer contents and up to 50 post-crash lines
    - **Validates: Requirements 2.2, 2.6**

  - [x]* 5.3 Write property test for crash log storage path format (Property 8)
    - **Property 8: Crash log storage path format**
    - Test that for any combination of tenantId, projectId, deviceSerial, and timestamp, the path matches `crash-logs/{tenantId}/{projectId}/{deviceSerial}/{timestamp}.log`
    - **Validates: Requirements 2.3**

- [x] 6. Implement CrashHistoryService and CrashNotificationService
  - [x] 6.1 Implement CrashHistoryService
    - Create `CrashHistoryService.java` with persist(), findByProject(), findById(), and getFrequencyByDay() methods
    - Implement paginated querying with filter support (device serial, date range, severity, crash type, acknowledged)
    - Implement daily crash count aggregation grouped by severity or device
    - Ensure multi-tenant isolation via tenant filter
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 7.1, 7.3, 7.4, 7.5, 10.1, 10.2, 10.3, 10.4_

  - [x]* 6.2 Write property test for CrashEvent database persistence round-trip (Property 11)
    - **Property 11: CrashEvent database persistence round-trip**
    - Test that for any valid CrashEvent, persisting and reading back produces an equivalent entity with all metadata preserved
    - **Validates: Requirements 6.1, 6.2, 6.4**

  - [x]* 6.3 Write property test for multi-tenant data isolation (Property 12)
    - **Property 12: Multi-tenant data isolation**
    - Test that for any two distinct tenants, querying as tenant A never returns events belonging to tenant B
    - **Validates: Requirements 6.3**

  - [x]* 6.4 Write property test for crash history filter correctness (Property 13)
    - **Property 13: Crash history filter correctness**
    - Test that for any set of crash events and valid filter criteria, returned results contain only events matching ALL criteria, ordered by timestamp descending
    - **Validates: Requirements 7.1, 7.3, 7.4**

  - [x]* 6.5 Write property test for daily frequency aggregation completeness (Property 14)
    - **Property 14: Daily frequency aggregation completeness**
    - Test that for any date range and set of events, aggregation returns one data point per day with correct counts including zero for days with no crashes
    - **Validates: Requirements 7.5, 10.1, 10.2, 10.3**

  - [x] 6.6 Implement CrashNotificationService
    - Create `CrashNotificationService.java` with notifyCrash() and buildPayload() methods
    - Send crash notification via STOMP to `/topic/crash/{deviceSerial}` and `/topic/crash-count/{projectId}`
    - Build notification payload with device name, crash type, and package name
    - Respect per-project notification preferences from AlertConfiguration
    - _Requirements: 4.1, 4.4_

  - [x]* 6.7 Write property test for notification payload correctness (Property 9)
    - **Property 9: Notification payload correctness**
    - Test that for any CrashEvent where notifications are enabled, the payload contains device name, crash type, and package name, and is only sent when configuration permits
    - **Validates: Requirements 4.1, 4.4**

- [x] 7. Implement AutoPullService
  - [x] 7.1 Implement AutoPullService
    - Create `AutoPullService.java` with triggerLogPull() and isPullInProgress() methods
    - Use ConcurrentHashMap to track in-progress pulls per device serial
    - Trigger log pull via agent WebSocket command when auto-pull is enabled
    - Associate pulled log file with the corresponding CrashEvent
    - Record failure in CrashEvent metadata if pull fails or times out (60s timeout)
    - Release pull lock on completion or failure
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

  - [x]* 7.2 Write property test for concurrent auto-pull prevention (Property 10)
    - **Property 10: Concurrent auto-pull prevention**
    - Test that for any device with an in-progress pull, subsequent crash events do not trigger additional pulls until the current one completes
    - **Validates: Requirements 5.3**

- [x] 8. Checkpoint - Backend services complete
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Integrate CrashDetector into LogcatStreamManager and wire event listeners
  - [x] 9.1 Integrate CrashDetector into LogcatStreamManager
    - Modify `LogcatStreamManager` to pass each incoming logcat line to `CrashDetector.analyze()`
    - Add LineBuffer to each logcat session with configurable capacity from AlertConfiguration
    - Publish CrashEvent via Spring ApplicationEventPublisher when crash is detected
    - _Requirements: 1.1, 2.1, 8.3_

  - [x] 9.2 Create event listeners for CrashEvent
    - Create `@EventListener` methods that invoke CrashHistoryService.persist(), CrashLogCaptureService.captureCrashLog(), CrashNotificationService.notifyCrash(), and AutoPullService.triggerLogPull()
    - Ensure each listener handles errors independently without blocking others
    - _Requirements: 2.2, 4.1, 5.1, 6.1_

  - [x] 9.3 Implement hot-reload of AlertConfiguration for active sessions
    - When AlertConfiguration is updated, apply new settings (buffer size, enabled patterns) to active logcat sessions within 5 seconds
    - _Requirements: 8.3, 9.4_

- [x] 10. Implement REST API endpoints
  - [x] 10.1 Create CrashController with crash history and detail endpoints
    - Implement GET `/api/projects/{id}/crashes` with pagination and filter support
    - Implement GET `/api/projects/{id}/crashes/{crashId}` for single crash detail
    - Implement GET `/api/projects/{id}/crashes/{crashId}/log` for crash log download
    - Implement POST `/api/projects/{id}/crashes/{crashId}/acknowledge` for acknowledging a crash
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

  - [x] 10.2 Create CrashTrendController for frequency trend data
    - Implement GET `/api/projects/{id}/crash-trends` with date range and groupBy parameters
    - Support preset ranges: 7, 14, 30 days
    - _Requirements: 10.1, 10.2, 10.3, 10.4_

  - [x] 10.3 Create AlertConfigController for alert configuration management
    - Implement GET `/api/projects/{id}/alert-config` for retrieving configuration
    - Implement PUT `/api/projects/{id}/alert-config` for updating configuration
    - Implement POST `/api/projects/{id}/alert-config/patterns` for adding custom patterns
    - Implement DELETE `/api/projects/{id}/alert-config/patterns/{patternId}` for removing patterns
    - Implement PUT `/api/projects/{id}/alert-config/patterns/{patternId}` for updating patterns
    - _Requirements: 8.1, 8.4, 9.1, 9.2, 9.3, 9.5_

- [x] 11. Checkpoint - Backend API complete
  - Ensure all tests pass, ask the user if questions arise.

- [x] 12. Implement frontend components
  - [x] 12.1 Implement CrashIndicator component on DeviceCard
    - Create `CrashIndicator` React component that renders a red highlight border and crash badge on DeviceCard
    - Subscribe to STOMP topic `/topic/crash/{deviceSerial}` for real-time crash events
    - Display crash type and timestamp in the badge
    - Show unacknowledged crash count badge
    - Persist highlight for 30 seconds or until acknowledged
    - Navigate to crash detail view on badge click
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

  - [x] 12.2 Implement browser notification handling
    - Request browser notification permission on app load
    - Display browser notification with device name, crash type, and package name on crash event
    - Focus tab and navigate to crash detail on notification click
    - Fall back to in-app toast when notifications are denied or unsupported
    - _Requirements: 4.1, 4.2, 4.3_

  - [x] 12.3 Implement CrashHistoryPanel component
    - Create `CrashHistoryPanel` React component with paginated crash event list
    - Implement filter controls for device serial, date range, severity, and crash type
    - Display crash events ordered by timestamp descending
    - _Requirements: 7.1, 7.2, 7.3_

  - [x] 12.4 Implement CrashDetailView component
    - Create `CrashDetailView` React component showing full crash event details
    - Display stack trace snippet, metadata, and crash log download link
    - Include acknowledge button
    - _Requirements: 7.2_

  - [x] 12.5 Implement AlertConfigPanel component
    - Create `AlertConfigPanel` React component for per-project alert configuration
    - Include toggles for crash detection enabled, auto-pull enabled, notification preferences
    - Include buffer size input with validation (50-2000)
    - Include custom pattern management (add, remove, update) with regex validation feedback
    - _Requirements: 8.1, 8.4, 9.1, 9.2, 9.3, 9.5_

  - [x] 12.6 Implement CrashTrendChart component
    - Create `CrashTrendChart` React component displaying daily crash frequency
    - Support grouping by severity level or device
    - Include preset date range selectors (7, 14, 30 days)
    - _Requirements: 10.1, 10.2, 10.3, 10.4_

- [x] 13. Final checkpoint - All components integrated
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The project uses jqwik 1.9.1 for property-based testing (already in build.gradle)
- Frontend components extend the existing React + STOMP client architecture
- All backend services follow existing Spring Boot patterns with tenant filtering

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["1.3", "1.4"] },
    { "id": 2, "tasks": ["2.1", "2.3"] },
    { "id": 3, "tasks": ["2.2", "2.4", "2.5", "2.6", "2.7", "2.8", "4.1"] },
    { "id": 4, "tasks": ["4.2", "4.3", "4.4", "5.1"] },
    { "id": 5, "tasks": ["5.2", "5.3", "6.1", "6.6"] },
    { "id": 6, "tasks": ["6.2", "6.3", "6.4", "6.5", "6.7", "7.1"] },
    { "id": 7, "tasks": ["7.2", "9.1"] },
    { "id": 8, "tasks": ["9.2", "9.3"] },
    { "id": 9, "tasks": ["10.1", "10.2", "10.3"] },
    { "id": 10, "tasks": ["12.1", "12.2", "12.3", "12.4", "12.5", "12.6"] }
  ]
}
```
