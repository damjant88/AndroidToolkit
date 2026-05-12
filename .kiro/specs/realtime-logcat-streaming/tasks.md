# Implementation Plan: Real-Time Logcat Streaming

## Overview

This plan implements per-device real-time logcat streaming for the AndroidToolkit backend. A new `LogcatStreamManager` service observes device changes from the existing `DeviceMonitorService` and manages background `adb logcat` processes. A stateless `LogcatParser` extracts structured data (environment, client version, server product version, access token) from OkHttp log lines using regex. Extracted data is pushed to the frontend via the existing STOMP/SockJS WebSocket infrastructure on per-device topics (`/topic/logcat/{serial}`). All new code lives in the `backend` module; the `core` module remains unchanged.

## Tasks

- [x] 1. Define data models, enums, and internal value objects
  - [x] 1.1 Create `LogcatData` DTO
    - Create `backend/src/androidtoolkit/backend/dto/LogcatData.java`
    - Fields: `serial` (String), `environment` (String, nullable), `clientVersion` (String, nullable), `serverProductVersion` (String, nullable), `accessToken` (String, nullable)
    - Include getters, setters, and a constructor accepting serial
    - _Requirements: 3.2_

  - [x] 1.2 Create `ParsedField` record and `FieldType` enum
    - Create `backend/src/androidtoolkit/backend/service/ParsedField.java` as a record: `ParsedField(FieldType type, String value)`
    - Create `backend/src/androidtoolkit/backend/service/FieldType.java` enum with values: `ENVIRONMENT`, `CLIENT_VERSION`, `SERVER_PRODUCT_VERSION`, `ACCESS_TOKEN`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

  - [x] 1.3 Create `StreamState` enum and `LogcatSession` class
    - Create `backend/src/androidtoolkit/backend/service/StreamState.java` enum with values: `RUNNING`, `STOPPED`, `ERRORED`
    - Create `backend/src/androidtoolkit/backend/service/LogcatSession.java` with fields: `serial`, `pid`, `process` (Process), `readerThread` (Thread), `state` (StreamState), `restartAttempts` (int), `currentData` (LogcatData)
    - _Requirements: 1.5, 4.1, 4.2_

- [x] 2. Implement LogcatParser
  - [x] 2.1 Implement `LogcatParser` component
    - Create `backend/src/androidtoolkit/backend/service/LogcatParser.java`
    - Annotate with `@Component`
    - Implement `Optional<ParsedField> parseLine(String line)` method
    - Define compiled regex patterns as static final fields:
      - Environment: `-->\s+\w+\s+https?://([^/]+)/`
      - Client Version: `User-Agent:\s+SafePath\s+(\S+)\s+Android`
      - Server Product Version: `x-safepath-product-version:\s+(\S+)`
      - Access Token (response body): `"accessToken"\s*:\s*"([^"]+)"`
      - Access Token (request header): `Authorization:\s+Bearer\s+(\S+)`
    - Return the first matching `ParsedField` or `Optional.empty()`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

  - [ ]* 2.2 Write property test for environment parsing (Property 3)
    - **Property 3: Environment parsing**
    - Create `backend/test/androidtoolkit/backend/service/LogcatParserProperties.java`
    - Generate random valid HTTP methods and URLs with hostnames
    - Verify parser extracts exactly the hostname from `--> {METHOD} {URL}` lines
    - **Validates: Requirements 2.1**

  - [ ]* 2.3 Write property test for client version parsing (Property 4)
    - **Property 4: Client version parsing**
    - Add test to `LogcatParserProperties.java`
    - Generate random version strings (alphanumeric, dots, dashes, plus signs)
    - Verify parser extracts exactly the version from `User-Agent: SafePath {version} Android` lines
    - **Validates: Requirements 2.2**

  - [ ]* 2.4 Write property test for server product version parsing (Property 5)
    - **Property 5: Server product version parsing**
    - Add test to `LogcatParserProperties.java`
    - Generate random non-empty version strings
    - Verify parser extracts exactly the version from `x-safepath-product-version: {version}` lines
    - **Validates: Requirements 2.3**

  - [ ]* 2.5 Write property test for access token parsing (Property 6)
    - **Property 6: Access token parsing**
    - Add test to `LogcatParserProperties.java`
    - Generate random token strings (no quotes or whitespace)
    - Verify parser extracts the token from both `"accessToken":"{token}"` and `Authorization: Bearer {token}` formats
    - **Validates: Requirements 2.4, 2.5**

  - [ ]* 2.6 Write unit tests for LogcatParser
    - Create `backend/test/androidtoolkit/backend/service/LogcatParserTest.java`
    - Test specific example log lines for each pattern
    - Test lines that don't match any pattern return empty
    - Test edge cases: malformed URLs, empty version strings, multi-line JSON
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 3. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Implement LogcatStreamManager core lifecycle
  - [x] 4.1 Implement `LogcatStreamManager` service skeleton
    - Create `backend/src/androidtoolkit/backend/service/LogcatStreamManager.java`
    - Annotate with `@Service`
    - Inject `LogcatParser` and `SimpMessagingTemplate`
    - Maintain `ConcurrentHashMap<String, LogcatSession>` for active sessions
    - Implement `getStreamState(String serial)` method
    - Implement `@PreDestroy shutdown()` to terminate all active processes and clear sessions
    - _Requirements: 1.5, 5.3, 5.4_

  - [x] 4.2 Implement `startStream(String serial, String pid)` method
    - Build command: `adb -s {serial} logcat -s OkHttp:I --pid={pid}`
    - Launch process via `ProcessBuilder`
    - Create a virtual thread (`Thread.ofVirtual()`) to read stdout line-by-line
    - For each line, call `LogcatParser.parseLine()` and update `LogcatSession.currentData`
    - On data change, broadcast `LogcatData` via `SimpMessagingTemplate` to `/topic/logcat/{serial}`
    - Set session state to `RUNNING`
    - Ensure at most one process per device (stop existing before starting new)
    - _Requirements: 1.1, 1.4, 2.6, 3.1, 3.2, 3.3, 5.1, 5.2, 5.4_

  - [x] 4.3 Implement `stopStream(String serial)` method
    - Destroy the process (`process.destroyForcibly()`)
    - Interrupt the reader thread
    - Set session state to `STOPPED`
    - Remove session from the map
    - _Requirements: 1.2, 5.3_

  - [x] 4.4 Implement `onDevicesChanged(List<DeviceInfo> currentDevices)` method
    - Diff current devices against active sessions:
      - New devices with valid PID → call `startStream`
      - Removed devices → call `stopStream`
      - Devices with changed PID → call `stopStream` then `startStream` with new PID
    - _Requirements: 1.1, 1.2, 1.3_

  - [ ]* 4.5 Write property test for device lifecycle diffing (Property 1)
    - **Property 1: Device lifecycle diffing**
    - Create `backend/test/androidtoolkit/backend/service/LogcatStreamManagerProperties.java`
    - Generate random previous and current device lists with serials and PIDs
    - Verify: new devices get started, removed devices get stopped, PID changes trigger restart, exactly one stream per device in current list
    - **Validates: Requirements 1.1, 1.2, 1.3**

  - [ ]* 4.6 Write property test for command format construction (Property 2)
    - **Property 2: Command format construction**
    - Add test to `LogcatStreamManagerProperties.java`
    - Generate random serial strings and PID strings
    - Verify constructed command equals `adb -s {serial} logcat -s OkHttp:I --pid={pid}`
    - **Validates: Requirements 1.4, 5.1, 5.2**

  - [ ]* 4.7 Write property test for at most one process per device (Property 9)
    - **Property 9: At most one process per device**
    - Add test to `LogcatStreamManagerProperties.java`
    - Generate random sequences of device change events
    - Verify no device serial ever has more than one active session
    - **Validates: Requirements 5.4**

- [x] 5. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Implement retry logic and graceful degradation
  - [x] 6.1 Implement retry on unexpected process exit
    - In the reader thread, detect when the process exits unexpectedly (non-zero exit or premature EOF while state is RUNNING)
    - If `restartAttempts == 0`: log warning, schedule restart after 5-second delay using `ScheduledExecutorService`, increment `restartAttempts`
    - If `restartAttempts == 1`: log error, set state to `ERRORED`, do not retry further
    - _Requirements: 4.1, 4.2_

  - [x] 6.2 Implement error isolation
    - Ensure all exceptions in logcat reader threads are caught and logged without propagating
    - If `startStream` fails to launch the process, log warning, mark session as `ERRORED`, continue with other devices
    - Ensure `onDevicesChanged` never throws — wrap in try-catch with logging
    - _Requirements: 4.3, 4.4_

  - [ ]* 6.3 Write unit tests for retry and error handling
    - Create `backend/test/androidtoolkit/backend/service/LogcatStreamManagerTest.java`
    - Test: unexpected exit triggers one restart after 5s delay
    - Test: second failure marks stream as ERRORED with no further retries
    - Test: PID change on ERRORED stream resets and starts fresh
    - Test: failure in one device stream doesn't affect others
    - Test: shutdown terminates all processes
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 5.3_

  - [ ]* 6.4 Write property test for shutdown terminates all (Property 10)
    - **Property 10: Shutdown terminates all**
    - Add test to `LogcatStreamManagerProperties.java`
    - Generate random sets of active sessions
    - Verify calling shutdown results in zero active processes and all sessions stopped
    - **Validates: Requirements 5.3**

- [x] 7. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Implement LogcatData state updates and broadcast
  - [x] 8.1 Implement LogcatData state update logic
    - In `LogcatStreamManager`, when `LogcatParser` returns a `ParsedField`, update the corresponding field in `LogcatSession.currentData`:
      - `ENVIRONMENT` → `setEnvironment(value)`
      - `CLIENT_VERSION` → `setClientVersion(value)`
      - `SERVER_PRODUCT_VERSION` → `setServerProductVersion(value)`
      - `ACCESS_TOKEN` → `setAccessToken(value)`
    - Only broadcast if the value actually changed (compare with previous value)
    - _Requirements: 2.6, 3.1_

  - [ ]* 8.2 Write property test for LogcatData state update — last write wins (Property 7)
    - **Property 7: LogcatData state update — last write wins**
    - Create `backend/test/androidtoolkit/backend/service/LogcatDataTest.java`
    - Generate random sequences of ParsedField values
    - Verify final state of each field equals the last value of that type in the sequence, and unset fields remain null
    - **Validates: Requirements 2.6**

  - [ ]* 8.3 Write property test for broadcast completeness (Property 8)
    - **Property 8: Broadcast completeness**
    - Add test to `LogcatDataTest.java`
    - Generate random LogcatData states with various null/non-null combinations
    - Verify serialized JSON always contains all five keys: serial, environment, clientVersion, serverProductVersion, accessToken
    - **Validates: Requirements 3.2**

- [ ] 9. Integrate with DeviceMonitorService
  - [x] 9.1 Add LogcatStreamManager notification to DeviceMonitorService
    - Inject `LogcatStreamManager` into `DeviceMonitorService`
    - In `checkForDeviceChanges()`, after broadcasting device updates, call `logcatStreamManager.onDevicesChanged(...)` with the current device list mapped to `DeviceInfo` objects
    - Ensure the call is wrapped in try-catch so logcat failures never affect device monitoring
    - _Requirements: 1.1, 4.4, 6.3_

  - [x] 9.2 Add LogcatStreamManager notification to `broadcastDeviceUpdate()`
    - Also call `logcatStreamManager.onDevicesChanged(...)` in the `broadcastDeviceUpdate()` method for consistency
    - _Requirements: 1.1, 1.3_

  - [ ]* 9.3 Write unit tests for DeviceMonitorService integration
    - Test that `onDevicesChanged` is called after each poll cycle
    - Test that exceptions from `LogcatStreamManager` do not propagate to the scheduler
    - Use mock `LogcatStreamManager` to verify invocation
    - _Requirements: 4.4, 6.3_

- [x] 10. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document using jqwik (already in build.gradle)
- Unit tests validate specific examples and edge cases
- The `core` module is NOT modified — all new code lives in `backend`
- Virtual threads (Java 21) are used for async line reading to avoid blocking the scheduler thread pool
- The existing `DeviceMonitorService` receives a minimal one-line addition to notify `LogcatStreamManager`

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3"] },
    { "id": 1, "tasks": ["2.1"] },
    { "id": 2, "tasks": ["2.2", "2.3", "2.4", "2.5", "2.6"] },
    { "id": 3, "tasks": ["4.1"] },
    { "id": 4, "tasks": ["4.2", "4.3", "4.4"] },
    { "id": 5, "tasks": ["4.5", "4.6", "4.7", "6.1", "6.2"] },
    { "id": 6, "tasks": ["6.3", "6.4", "8.1"] },
    { "id": 7, "tasks": ["8.2", "8.3", "9.1", "9.2"] },
    { "id": 8, "tasks": ["9.3"] }
  ]
}
```
