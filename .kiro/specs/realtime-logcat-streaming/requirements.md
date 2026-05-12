# Requirements Document

## Introduction

This feature adds real-time logcat streaming per connected Android device in the AndroidToolkit. For each device running the SafePath app, a background `adb logcat` process monitors OkHttp log output, parses key data points (environment, client version, server version, access token) in real-time, and pushes extracted information to the frontend via WebSocket. This gives developers and testers immediate visibility into which environment a device is connected to, what app version is running, and the current authentication state — without manually reading raw logs.

## Glossary

- **Logcat_Stream_Manager**: The backend service responsible for managing per-device logcat streaming processes, coordinating lifecycle events (start, stop, restart)
- **Logcat_Parser**: The component that receives raw logcat lines and extracts structured data points using regex pattern matching against OkHttp log output
- **Logcat_Data**: A data object holding the extracted information for a single device: environment, client version, server product version, and access token
- **Device_Serial**: The unique serial identifier of an Android device connected via ADB (e.g., "emulator-5554" or "R5CR1234567")
- **PID**: The process identifier of the SafePath app running on a connected device, used to filter logcat output to only that process
- **OkHttp_Tag**: The Android log tag "OkHttp" used by the SafePath app's HTTP client library to log request and response details
- **Environment**: The base URL hostname of the API server the device is communicating with (e.g., "palm.safepath.cloud")
- **Client_Version**: The SafePath app version installed on the device, extracted from the User-Agent header (e.g., "8.4.2-SNAPSHOT+g86758ed")
- **Server_Product_Version**: The backend server version reported in the `x-safepath-product-version` response header (e.g., "8.4.1+g6fd868ad9")
- **Access_Token**: The current JWT authentication token used by the device, extracted from login response bodies or Authorization request headers
- **DeviceMonitorService**: The existing backend service that polls for device changes every 3 seconds and broadcasts updates via WebSocket on `/topic/devices`

## Requirements

### Requirement 1: Automatic Logcat Stream Lifecycle

**User Story:** As a developer, I want logcat streaming to start automatically when a device with SafePath is discovered, so that I get real-time data without manual intervention.

#### Acceptance Criteria

1. WHEN the DeviceMonitorService discovers a device with an installed SafePath app and a valid PID, THE Logcat_Stream_Manager SHALL start a background logcat process for that device
2. WHEN a device disconnects or is no longer reported by ADB, THE Logcat_Stream_Manager SHALL terminate the logcat process for that device and release associated resources
3. WHEN the PID of the SafePath app changes on a connected device (app restart or crash), THE Logcat_Stream_Manager SHALL terminate the existing logcat process and start a new one with the updated PID
4. THE Logcat_Stream_Manager SHALL execute the logcat command: `adb -s {serial} logcat -s OkHttp:I --pid={pid}` for each monitored device
5. WHILE a logcat process is running for a device, THE Logcat_Stream_Manager SHALL track the process state (running, stopped, errored) for that device

### Requirement 2: OkHttp Log Line Parsing

**User Story:** As a developer, I want the system to extract key data from OkHttp log lines, so that I can see environment, version, and auth info at a glance.

#### Acceptance Criteria

1. WHEN a logcat line contains an OkHttp request line matching the pattern `--> {METHOD} {URL}`, THE Logcat_Parser SHALL extract the base URL hostname as the Environment value
2. WHEN a logcat line contains a User-Agent header matching the pattern `SafePath {version} Android`, THE Logcat_Parser SHALL extract the version string as the Client_Version value
3. WHEN a logcat line contains a response header matching `x-safepath-product-version: {value}`, THE Logcat_Parser SHALL extract the value as the Server_Product_Version
4. WHEN a logcat line contains a JSON response body with an `accessToken` field, THE Logcat_Parser SHALL extract the token value as the Access_Token
5. WHEN a logcat line contains an Authorization header matching `Authorization: Bearer {token}`, THE Logcat_Parser SHALL extract the token value as the Access_Token
6. WHEN a new value is extracted for any data point, THE Logcat_Parser SHALL update the corresponding field in the Logcat_Data for that device, replacing the previous value

### Requirement 3: WebSocket Data Push

**User Story:** As a frontend developer, I want extracted logcat data pushed via WebSocket, so that the UI updates in real-time without polling.

#### Acceptance Criteria

1. WHEN the Logcat_Parser extracts a new or changed data point for a device, THE Logcat_Stream_Manager SHALL broadcast the updated Logcat_Data to the WebSocket topic `/topic/logcat/{serial}`
2. THE Logcat_Stream_Manager SHALL send the complete Logcat_Data object (all four fields) in each WebSocket message, using null for fields not yet extracted
3. THE Logcat_Stream_Manager SHALL use the existing STOMP/SockJS WebSocket infrastructure configured in WebSocketConfig

### Requirement 4: Graceful Degradation

**User Story:** As a tester, I want logcat streaming failures to not affect existing device monitoring, so that the tool remains usable even if streaming encounters issues.

#### Acceptance Criteria

1. IF the logcat process for a device exits unexpectedly, THEN THE Logcat_Stream_Manager SHALL log a warning and attempt to restart the process once after a 5-second delay
2. IF the restart attempt also fails, THEN THE Logcat_Stream_Manager SHALL log an error and mark the stream as errored without retrying further until the next PID change or device reconnection
3. IF the logcat process cannot be started for a device, THEN THE Logcat_Stream_Manager SHALL log a warning and continue monitoring other devices without interruption
4. THE Logcat_Stream_Manager SHALL operate independently from the DeviceMonitorService polling loop, ensuring that logcat failures do not delay or block device discovery broadcasts

### Requirement 5: Resource Management

**User Story:** As a system administrator, I want logcat streaming to use minimal resources, so that the tool remains responsive with multiple connected devices.

#### Acceptance Criteria

1. THE Logcat_Stream_Manager SHALL filter logcat output to only the OkHttp tag at INFO level using the `-s OkHttp:I` flag to minimize data volume
2. THE Logcat_Stream_Manager SHALL filter logcat output by the SafePath app PID using the `--pid={pid}` flag to exclude other processes
3. WHEN the backend application shuts down, THE Logcat_Stream_Manager SHALL terminate all active logcat processes and release all associated resources
4. THE Logcat_Stream_Manager SHALL maintain at most one logcat process per connected device at any time

### Requirement 6: Backend Module Constraint

**User Story:** As a developer, I want all logcat streaming code in the backend module, so that the core module remains unchanged and reusable.

#### Acceptance Criteria

1. THE Logcat_Stream_Manager SHALL reside in the `backend` module package structure
2. THE Logcat_Parser SHALL reside in the `backend` module package structure
3. THE Logcat_Stream_Manager SHALL use the existing `DeviceInfo` domain object from the core module to obtain device serial numbers and PIDs without modifying the core module
