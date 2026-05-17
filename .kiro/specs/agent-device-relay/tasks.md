# Implementation Plan: Agent Device Relay

## Overview

This plan implements the deployment-mode-aware abstraction layer that bridges the SaaS backend and agent modules. The implementation introduces a `DeviceProvider` interface with two implementations (local and agent-relayed), enhances `AgentConnectionManager` with full device registry management, adds a `CommandRelay` for forwarding commands to agents, and introduces `LogcatRelayService` for SaaS-mode logcat streaming. Each task builds incrementally, starting with domain types and interfaces, then implementations, then wiring.

## Tasks

- [x] 1. Define domain types and DeviceProvider interface
  - [x] 1.1 Add Reboot command to AgentCommand sealed interface
    - Add `record Reboot(String serial) implements AgentCommand {}` to `core/src/androidtoolkit/domain/agent/AgentCommand.java`
    - _Requirements: 4.6_

  - [x] 1.2 Create DeviceProvider interface
    - Create `backend/src/androidtoolkit/backend/device/DeviceProvider.java` with methods: `discoverDevices`, `reboot`, `uninstall`, `screenshot`, `pullLogs`, `toggleWifiDebug`, `enableFirebaseDebug`
    - All methods accept `Long tenantId` as first parameter for tenant scoping
    - Return types match existing: `DeviceDiscoveryResult`, `DeviceMessageResult`, `UninstallAppResult`, `ScreenshotCaptureResponse`, `WifiDebugResult`
    - _Requirements: 1.1, 1.2, 1.6_

  - [x] 1.3 Create error exception classes
    - Create `backend/src/androidtoolkit/backend/device/DeviceUnreachableException.java` with `serial` and `reason` fields
    - Create `backend/src/androidtoolkit/backend/device/CommandTimeoutException.java` with `serial` and `timeoutSeconds` fields
    - Create `backend/src/androidtoolkit/backend/device/AgentDisconnectedException.java` with `serial` and `agentId` fields
    - Create `backend/src/androidtoolkit/backend/device/UnsupportedCommandException.java` with `commandType` field
    - _Requirements: 1.5, 4.3, 4.5, 4.7, 4.8_

  - [x]* 1.4 Write property test for exponential backoff calculation (Property 12)
    - **Property 12: Exponential backoff calculation**
    - Test that for any `n` consecutive failures (n ≥ 1), delay equals `min(1000 * 2^(n-1), 60000)` ms
    - Create `backend/test/androidtoolkit/agent/connection/ServerConnectionPropertyTest.java`
    - **Validates: Requirements 8.4**

- [x] 2. Implement LocalDeviceProvider (standalone mode)
  - [x] 2.1 Create LocalDeviceProvider class
    - Create `backend/src/androidtoolkit/backend/device/LocalDeviceProvider.java`
    - Annotate with `@Component` and `@ConditionalOnProperty(name = "deployment.mode", havingValue = "standalone", matchIfMissing = true)`
    - Inject `DeviceCatalog`, `DeviceActionManager`, `ScreenshotManager`
    - `discoverDevices` delegates to `deviceCatalog.discoverDevices()`, catches `RuntimeException` and returns empty result
    - `reboot`, `uninstall`, `screenshot`, `pullLogs`, `toggleWifiDebug`, `enableFirebaseDebug` delegate to existing managers
    - _Requirements: 1.3, 6.1, 6.3, 6.5_

  - [x]* 2.2 Write unit tests for LocalDeviceProvider
    - Test that adb failure returns empty device list
    - Test that each command delegates to the correct manager method
    - Create `backend/test/androidtoolkit/backend/device/LocalDeviceProviderTest.java`
    - _Requirements: 6.1, 6.3, 6.5_

- [x] 3. Enhance AgentConnectionManager for full device registry
  - [x] 3.1 Add AgentSession record and device registry methods to AgentConnectionManager
    - Refactor internal `AgentInfo` to store `List<DeviceInfo>` and `Set<String> deviceSerials` per agent
    - Add `Instant tokenExpiresAt` field to agent session data
    - Add `updateDeviceList(String agentId, List<DeviceInfo> devices)` — replaces entire device set for agent
    - Add `getDevicesForTenant(Long tenantId)` — returns `DeviceDiscoveryResult` as union of all agents' devices for tenant
    - Add `getAgentsForTenant(Long tenantId)` — returns `Set<String>` of agent IDs
    - Add `hasDeviceSetChanged(String agentId, List<DeviceInfo> newDevices)` — compares serial sets
    - Add `getTenantIdForAgent(String agentId)` — returns the tenant ID for a given agent
    - Update `unregisterAgent` to clear all device mappings for the agent
    - _Requirements: 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 3.5_

  - [x]* 3.2 Write property tests for AgentConnectionManager (Properties 1-5)
    - **Property 1: DeviceList replacement is total and data-preserving**
    - **Property 2: Agent disconnect removes all devices**
    - **Property 3: Tenant isolation on device queries**
    - **Property 4: Multi-agent device aggregation**
    - **Property 5: Device change detection triggers broadcast**
    - Create `backend/test/androidtoolkit/backend/device/AgentConnectionManagerPropertyTest.java`
    - **Validates: Requirements 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 3.2, 3.5, 8.2, 8.3**

- [x] 4. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Implement CommandRelay
  - [x] 5.1 Create CommandRelay class
    - Create `backend/src/androidtoolkit/backend/device/CommandRelay.java`
    - Annotate with `@Component` and `@ConditionalOnProperty(name = "deployment.mode", havingValue = "saas")`
    - Maintain `ConcurrentHashMap<String, PendingCommand>` for pending futures keyed by `requestId`
    - `execute(Long tenantId, String serial, AgentCommand command)` — looks up agent via `AgentConnectionManager.findAgentForDevice()`, sends command, awaits `CompletableFuture` with 30s timeout
    - `completeCommand(String requestId, OperationResult result)` — completes the pending future
    - `failCommandsForAgent(String agentId)` — completes all pending futures for that agent exceptionally with `AgentDisconnectedException`
    - Throw `DeviceUnreachableException` if no agent found, `CommandTimeoutException` on timeout
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.8_

  - [x]* 5.2 Write property tests for CommandRelay (Properties 6-9)
    - **Property 6: Command routing to correct agent**
    - **Property 7: Unreachable device returns error**
    - **Property 8: OperationResult completes pending command**
    - **Property 9: Agent disconnect fails all pending commands**
    - Create `backend/test/androidtoolkit/backend/device/CommandRelayPropertyTest.java`
    - **Validates: Requirements 1.5, 4.1, 4.3, 4.4, 4.8**

- [x] 6. Implement AgentDeviceProvider (SaaS mode)
  - [x] 6.1 Create AgentDeviceProvider class
    - Create `backend/src/androidtoolkit/backend/device/AgentDeviceProvider.java`
    - Annotate with `@Component` and `@ConditionalOnProperty(name = "deployment.mode", havingValue = "saas")`
    - Inject `AgentConnectionManager` and `CommandRelay`
    - `discoverDevices` delegates to `connectionManager.getDevicesForTenant(tenantId)`
    - `reboot` sends `AgentCommand.Reboot` via `commandRelay.execute()`
    - `uninstall` sends `AgentCommand.UninstallApp` via `commandRelay.execute()`
    - `screenshot` sends `AgentCommand.CaptureScreenshot` via `commandRelay.execute()`
    - `pullLogs` sends `AgentCommand.PullLogs` via `commandRelay.execute()`
    - `toggleWifiDebug` and `enableFirebaseDebug` throw `UnsupportedCommandException` (not supported in SaaS mode)
    - _Requirements: 1.4, 4.1, 4.6, 4.7_

  - [x]* 6.2 Write property test for cross-tenant command rejection (Property 10)
    - **Property 10: Cross-tenant command rejection**
    - Create `backend/test/androidtoolkit/backend/device/TenantIsolationPropertyTest.java`
    - **Validates: Requirements 7.5, 7.6**

- [x] 7. Implement LogcatRelayService
  - [x] 7.1 Create LogcatRelayService class
    - Create `backend/src/androidtoolkit/backend/device/LogcatRelayService.java`
    - Annotate with `@Component` and `@ConditionalOnProperty(name = "deployment.mode", havingValue = "saas")`
    - Maintain `Set<String> activeStreams` using `ConcurrentHashMap.newKeySet()`
    - `startStream(Long tenantId, String serial)` — finds agent, sends `StartLogcat` command, adds to active streams; throws `DeviceUnreachableException` if no agent
    - `stopStream(Long tenantId, String serial)` — sends `StopLogcat` command, removes from active streams
    - `forwardLine(String serial, String line, long timestamp)` — truncates line to 4096 chars, sends to `/topic/logcat/{serial}` via `SimpMessagingTemplate`
    - `onAgentDisconnected(String agentId)` — sends stream-ended notification for all active streams of that agent's devices, removes from active streams
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

  - [x]* 7.2 Write property test for logcat line truncation (Property 11)
    - **Property 11: Logcat line truncation**
    - Test that for any string of any length, forwarded line is at most 4096 characters
    - Create `backend/test/androidtoolkit/backend/device/LogcatRelayPropertyTest.java`
    - **Validates: Requirements 5.6**

- [x] 8. Update AgentWebSocketHandler for full message routing
  - [x] 8.1 Update AgentWebSocketHandler to handle DeviceList with full replacement
    - Modify `handleTextMessage` for `DeviceList` case: parse full `DeviceInfo` list from JSON, call `connectionManager.updateDeviceList(agentId, devices)`
    - If `connectionManager.hasDeviceSetChanged(agentId, devices)` returns true, broadcast updated tenant device list to `/topic/devices` via `SimpMessagingTemplate`
    - Broadcast only to the tenant's subscribers using `convertAndSendToUser` or tenant-scoped topic
    - _Requirements: 2.2, 2.6, 2.7, 3.1, 3.2, 3.3_

  - [x] 8.2 Update AgentWebSocketHandler to route OperationResult to CommandRelay
    - Modify `handleTextMessage` for `OperationResult` case: extract `requestId`, `success`, `detail` and call `commandRelay.completeCommand(requestId, result)`
    - Inject `CommandRelay` (optional dependency, only present in SaaS mode)
    - _Requirements: 4.4_

  - [x] 8.3 Update AgentWebSocketHandler to forward LogcatLine to LogcatRelayService
    - Modify `handleTextMessage` for `LogcatLine` case: extract `serial`, `line`, `timestamp` and call `logcatRelayService.forwardLine(serial, line, timestamp)`
    - Inject `LogcatRelayService` (optional dependency, only present in SaaS mode)
    - Apply 4096-char truncation in `LogcatRelayService.forwardLine()`
    - _Requirements: 5.2, 5.6_

  - [x] 8.4 Update afterConnectionClosed to trigger device broadcast and fail pending commands
    - On disconnect: call `commandRelay.failCommandsForAgent(agentId)` if CommandRelay is present
    - Call `logcatRelayService.onAgentDisconnected(agentId)` if LogcatRelayService is present
    - Broadcast updated device list for the agent's tenant after removing devices
    - _Requirements: 2.3, 3.4, 4.8, 5.4_

  - [x] 8.5 Add scheduled JWT token expiry check
    - Add a `@Scheduled(fixedDelay = 60000)` method that iterates all agent sessions
    - If `tokenExpiresAt` is in the past, close the WebSocket session with `POLICY_VIOLATION` status
    - _Requirements: 7.7_

- [x] 9. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 10. Wire DeviceProvider into DeviceController and DeviceMonitorService
  - [x] 10.1 Refactor DeviceController to use DeviceProvider
    - Replace direct `DeviceCatalog` and `DeviceActionManager` usage with `DeviceProvider` injection
    - `getConnectedDevices()` calls `deviceProvider.discoverDevices(tenantId)` — extract tenantId from security context
    - `reboot`, `uninstall`, `screenshot`, `pullLogs`, `toggleWifiDebug`, `enableFirebaseDebug` delegate to `DeviceProvider`
    - Add tenant validation: reject commands targeting devices not belonging to the authenticated user's tenant
    - _Requirements: 1.6, 2.1, 6.1, 6.3, 7.5, 7.6_

  - [x] 10.2 Refactor DeviceMonitorService for deployment-mode awareness
    - In standalone mode: keep existing `@Scheduled(fixedDelay = 3000)` polling behavior unchanged
    - In SaaS mode: disable the scheduled polling (use `@ConditionalOnProperty` or check mode in method)
    - Device broadcasts in SaaS mode are triggered by `AgentWebSocketHandler` when DeviceList messages arrive
    - _Requirements: 3.1, 6.2, 6.6_

  - [x] 10.3 Register error handlers in GlobalExceptionHandler
    - Add `@ExceptionHandler` methods for `DeviceUnreachableException` (503), `CommandTimeoutException` (504), `AgentDisconnectedException` (502), `UnsupportedCommandException` (400)
    - Return structured JSON error responses matching the design's error table
    - _Requirements: 4.3, 4.5, 4.7, 4.8_

  - [x]* 10.4 Write unit tests for DeviceController with DeviceProvider
    - Test standalone mode: verify delegation to local provider
    - Test SaaS mode: verify delegation to agent provider
    - Test cross-tenant rejection returns 403
    - Test error responses for unreachable device (503), timeout (504), disconnected agent (502)
    - _Requirements: 1.6, 7.5, 7.6_

- [x] 11. Implement agent-side reconnection and DeviceList re-send
  - [x] 11.1 Update ServerConnection to re-send DeviceList on reconnection
    - After successful reconnection (in `attemptConnection` after `consecutiveFailures` reset), trigger a callback to re-send the current DeviceList
    - Add a `Runnable onReconnected` callback that the agent application sets to re-send device state
    - Ensure DeviceList is sent within 5 seconds of connection establishment
    - _Requirements: 8.1, 8.2, 8.5_

  - [x]* 11.2 Write unit tests for agent reconnection behavior
    - Test that DeviceList is re-sent after reconnection
    - Test that exponential backoff delays are calculated correctly
    - Test that pending messages are flushed after reconnection
    - _Requirements: 8.1, 8.4, 8.5_

- [x] 12. Add deployment.mode configuration property
  - [x] 12.1 Add deployment.mode property to application properties files
    - Add `deployment.mode=standalone` to `backend/resources/application.properties` (default)
    - Add `deployment.mode=saas` to `backend/resources/application-saas.properties`
    - Ensure standalone is the default when property is not set (matchIfMissing=true on LocalDeviceProvider)
    - _Requirements: 6.4_

- [x] 13. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The design uses Java with Spring Boot — all implementations follow existing project conventions
- `@ConditionalOnProperty` ensures clean separation between standalone and SaaS beans at startup
- The existing `AgentWebSocketHandler`, `AgentConnectionManager`, and `ServerConnection` are modified in-place rather than replaced

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3", "1.4"] },
    { "id": 1, "tasks": ["2.1", "3.1", "12.1"] },
    { "id": 2, "tasks": ["2.2", "3.2", "5.1"] },
    { "id": 3, "tasks": ["5.2", "6.1", "7.1"] },
    { "id": 4, "tasks": ["6.2", "7.2", "8.1", "8.2", "8.3"] },
    { "id": 5, "tasks": ["8.4", "8.5"] },
    { "id": 6, "tasks": ["10.1", "10.2", "10.3", "11.1"] },
    { "id": 7, "tasks": ["10.4", "11.2"] }
  ]
}
```
