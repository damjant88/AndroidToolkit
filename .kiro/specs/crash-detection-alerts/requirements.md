# Requirements Document

## Introduction

This feature adds real-time crash detection and alerting to the Android Toolkit. The system monitors the existing logcat stream (already flowing via STOMP WebSocket from devices) for crash patterns such as FATAL EXCEPTION, ANR, signal crashes, and uncaught exceptions. When a crash is detected, the system highlights the affected device, auto-saves a crash log to Object Storage, notifies the user, and records the event for historical analysis. Users can view crash history per project/device, configure alert preferences, and define custom crash patterns.

## Glossary

- **Crash_Detector**: The backend service component that analyzes incoming logcat lines for crash patterns and emits crash events
- **Crash_Event**: A recorded occurrence of a crash, containing metadata such as timestamp, device serial, package name, crash type, severity, and stack trace snippet
- **Crash_Log**: A saved buffer of the last N logcat lines surrounding a crash, stored as a file in Object Storage
- **Crash_Pattern**: A regex pattern used to identify crash signatures in logcat output (e.g., `FATAL EXCEPTION`, `ANR in`)
- **Alert_Configuration**: Per-project settings that control crash detection behavior including enabled patterns, notification preferences, and auto-pull settings
- **Severity_Level**: A classification of crash importance: FATAL, ANR, or WARNING
- **Crash_History_Service**: The backend component responsible for persisting, querying, and aggregating crash events
- **Notification_Service**: The component responsible for delivering browser notifications to users when crashes are detected
- **Line_Buffer**: A configurable rolling buffer (default 500 lines) of recent logcat lines maintained per device session
- **Object_Storage**: The storage backend (MinIO/S3 or local filesystem) used to persist crash log files
- **LogcatStreamManager**: The existing service that manages per-device logcat streaming processes and reads logcat output line-by-line
- **DeviceCard**: The React UI component that displays device information and actions

## Requirements

### Requirement 1: Crash Pattern Detection

**User Story:** As a QA engineer, I want the system to automatically detect crashes in the real-time logcat stream, so that I am immediately aware when an application crashes on any connected device.

#### Acceptance Criteria

1. WHEN a logcat line matches a registered Crash_Pattern, THE Crash_Detector SHALL emit a Crash_Event within 1 second of receiving the line
2. THE Crash_Detector SHALL support the following default Crash_Patterns: `FATAL EXCEPTION`, `ANR in`, `Process crashed`, `java.lang.RuntimeException`, `SIGABRT`, and `SIGSEGV`
3. WHEN a Crash_Event is emitted, THE Crash_Detector SHALL classify the Severity_Level as FATAL for `FATAL EXCEPTION`, `SIGABRT`, and `SIGSEGV` patterns, as ANR for `ANR in` patterns, and as WARNING for all other patterns
4. THE Crash_Detector SHALL extract the crashing package name from the logcat line context when available
5. THE Crash_Detector SHALL extract a stack trace snippet of up to 20 lines following the crash signature line
6. WHEN multiple crash patterns match within a 5-second window for the same device, THE Crash_Detector SHALL group them into a single Crash_Event to avoid duplicate alerts
7. FOR ALL valid logcat lines, detecting a crash then serializing and deserializing the Crash_Event SHALL produce an equivalent Crash_Event (round-trip property)

### Requirement 2: Crash Log Capture

**User Story:** As a developer, I want the system to automatically save the logcat context surrounding a crash, so that I have the information needed to diagnose the issue without manually pulling logs.

#### Acceptance Criteria

1. THE LogcatStreamManager SHALL maintain a rolling Line_Buffer of the last N logcat lines per device session, where N is configurable with a default of 500
2. WHEN a Crash_Event is emitted, THE Crash_Detector SHALL capture the current Line_Buffer contents plus up to 50 additional lines after the crash as the Crash_Log
3. WHEN a Crash_Log is captured, THE Crash_Detector SHALL upload the Crash_Log to Object_Storage with a path structure of `crash-logs/{tenantId}/{projectId}/{deviceSerial}/{timestamp}.log`
4. WHEN the Object_Storage upload fails, THE Crash_Detector SHALL retry the upload up to 3 times with exponential backoff
5. IF the Object_Storage upload fails after all retry attempts, THEN THE Crash_Detector SHALL log the failure and store the Crash_Log locally as a fallback
6. THE Crash_Log file SHALL include a header containing the Crash_Event metadata (timestamp, device serial, package name, crash type, severity)

### Requirement 3: Real-Time UI Crash Indication

**User Story:** As a QA engineer, I want to immediately see which device has crashed in the UI, so that I can quickly identify problematic devices during testing sessions.

#### Acceptance Criteria

1. WHEN a Crash_Event is received via WebSocket, THE DeviceCard SHALL display a red highlight border that persists for 30 seconds or until the user acknowledges the crash
2. WHEN a Crash_Event is received, THE DeviceCard SHALL display the crash type and timestamp in a crash indicator badge
3. WHILE a device has an unacknowledged Crash_Event, THE DeviceCard SHALL display a crash count badge showing the number of unacknowledged crashes
4. WHEN the user clicks the crash indicator badge, THE DeviceCard SHALL navigate to the crash detail view for that event

### Requirement 4: Browser Notifications

**User Story:** As a QA engineer, I want to receive browser notifications when a crash occurs, so that I am alerted even when the Android Toolkit tab is not in focus.

#### Acceptance Criteria

1. WHEN a Crash_Event is received and browser notifications are enabled, THE Notification_Service SHALL display a browser notification containing the device name, crash type, and package name
2. WHEN the user clicks a browser notification, THE Notification_Service SHALL focus the Android Toolkit tab and navigate to the crash detail view
3. IF the browser does not support notifications or the user has denied permission, THEN THE Notification_Service SHALL fall back to an in-app toast notification
4. THE Notification_Service SHALL respect the per-project Alert_Configuration notification preferences

### Requirement 5: Automatic Log Pull

**User Story:** As a developer, I want the system to optionally trigger a full log pull from the device when a crash is detected, so that I have comprehensive diagnostic data without manual intervention.

#### Acceptance Criteria

1. WHERE auto-pull is enabled in the Alert_Configuration, THE Crash_Detector SHALL trigger a log pull command for the crashed device within 10 seconds of crash detection
2. WHEN an automatic log pull is triggered, THE Crash_Detector SHALL associate the pulled log file with the corresponding Crash_Event
3. WHILE an automatic log pull is in progress for a device, THE Crash_Detector SHALL not trigger additional log pulls for that device until the current pull completes
4. IF the automatic log pull fails, THEN THE Crash_Detector SHALL record the failure in the Crash_Event metadata without blocking other crash processing

### Requirement 6: Crash Event Persistence

**User Story:** As a QA engineer, I want crash events stored in the database with full metadata, so that I can review crash history and identify recurring issues.

#### Acceptance Criteria

1. WHEN a Crash_Event is emitted, THE Crash_History_Service SHALL persist the event with the following metadata: timestamp, device serial, device name, package name, crash type, Severity_Level, stack trace snippet, Crash_Log storage path, and project association
2. THE Crash_History_Service SHALL associate each Crash_Event with the correct tenant and project based on the device-project mapping
3. THE Crash_History_Service SHALL enforce multi-tenant data isolation so that users can only access Crash_Events belonging to their tenant
4. FOR ALL persisted Crash_Events, serializing to the database and reading back SHALL produce an equivalent Crash_Event (round-trip property)

### Requirement 7: Crash History View

**User Story:** As a QA engineer, I want to view crash history in the project detail panel, so that I can track crash patterns and identify regression issues.

#### Acceptance Criteria

1. THE Crash_History_Service SHALL provide a paginated list of Crash_Events for a given project, ordered by timestamp descending
2. WHEN the user selects a Crash_Event from the history list, THE Crash_History_Service SHALL display the full crash detail including stack trace snippet and a link to download the Crash_Log
3. THE Crash_History_Service SHALL support filtering Crash_Events by device serial, date range, Severity_Level, and crash type
4. WHEN a date range filter is applied, THE Crash_History_Service SHALL return only Crash_Events with timestamps within the specified range (inclusive)
5. THE Crash_History_Service SHALL provide crash frequency aggregation data grouped by day for the selected time period

### Requirement 8: Alert Configuration

**User Story:** As a project administrator, I want to configure crash detection settings per project, so that I can customize alerting behavior for different testing scenarios.

#### Acceptance Criteria

1. THE Alert_Configuration SHALL support the following per-project settings: crash detection enabled/disabled, auto-pull enabled/disabled, notification preferences (browser, in-app, or both), and buffer size (50 to 2000 lines)
2. WHEN a project has no explicit Alert_Configuration, THE Crash_Detector SHALL use default settings: crash detection enabled, auto-pull disabled, browser notifications enabled, buffer size 500
3. WHEN the Alert_Configuration is updated, THE Crash_Detector SHALL apply the new settings to active logcat sessions for that project within 5 seconds without restarting the stream
4. THE Alert_Configuration SHALL validate that buffer size is between 50 and 2000 lines inclusive

### Requirement 9: Custom Crash Patterns

**User Story:** As a QA engineer, I want to define custom crash patterns per project, so that I can detect application-specific error conditions beyond the default patterns.

#### Acceptance Criteria

1. THE Alert_Configuration SHALL allow adding custom Crash_Patterns as regex strings with an associated Severity_Level
2. WHEN a custom Crash_Pattern is added, THE Crash_Detector SHALL validate that the regex compiles without errors before saving
3. IF an invalid regex is provided, THEN THE Crash_Detector SHALL reject the pattern and return a descriptive error message indicating the regex syntax issue
4. WHEN a custom Crash_Pattern is saved, THE Crash_Detector SHALL begin matching against the pattern within 5 seconds without restarting the logcat stream
5. THE Alert_Configuration SHALL support removing and updating existing custom Crash_Patterns
6. FOR ALL valid regex strings, adding a custom pattern then retrieving the pattern list SHALL include the added pattern with its original regex and severity (round-trip property)

### Requirement 10: Crash Frequency Trends

**User Story:** As a project administrator, I want to see crash frequency trends over time, so that I can assess application stability and identify regressions after releases.

#### Acceptance Criteria

1. THE Crash_History_Service SHALL compute daily crash counts grouped by Severity_Level for a specified date range
2. THE Crash_History_Service SHALL compute daily crash counts grouped by device for a specified date range
3. WHEN the user requests trend data, THE Crash_History_Service SHALL return data points for each day in the range, including days with zero crashes
4. THE Crash_History_Service SHALL support trend aggregation for the last 7, 14, and 30 days as preset ranges
