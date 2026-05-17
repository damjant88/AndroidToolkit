# Requirements Document

## Introduction

The Agent Device Relay feature bridges the gap between the SaaS-deployed backend (running in Docker on Azure without USB/adb access) and the agent modules running on client machines where Android devices are physically connected. In SaaS mode, the backend must read device state from agent-reported data via WebSocket and relay device commands to the appropriate agent, rather than calling `adb` locally. In local/desktop mode, the existing direct `adb` path continues to work unchanged. The frontend remains agnostic to the device data source.

## Glossary

- **Backend**: The Spring Boot server application deployed in Docker on Azure (SaaS mode) or running locally (desktop mode)
- **Agent**: A Java process running on a client machine with physical USB access to Android devices, connected to the Backend via WebSocket
- **DeviceController**: The REST controller (`/api/devices`) that serves device data and accepts device commands from the frontend
- **DeviceMonitorService**: The scheduled service that polls for device changes and broadcasts updates via the `/topic/devices` WebSocket topic
- **AgentConnectionManager**: The service that tracks connected Agent WebSocket sessions and their reported devices
- **DeviceProvider**: An abstraction that supplies device discovery results and executes device commands, with implementations for local adb and agent-relayed modes
- **Deployment_Mode**: A configuration property (`deployment.mode`) that determines whether the Backend operates in `saas` mode (agent-relayed) or `standalone` mode (direct adb)
- **Command_Relay**: The mechanism by which the Backend forwards a device command to the appropriate Agent via WebSocket and awaits the result
- **AgentCommand**: A sealed interface representing commands sent from the Backend to an Agent (StartLogcat, StopLogcat, PullLogs, InstallApk, UninstallApp, CaptureScreenshot)
- **AgentMessage**: A sealed interface representing messages sent from an Agent to the Backend (LogcatLine, DeviceList, OperationResult, LogArchiveReady)

## Requirements

### Requirement 1: Device Provider Abstraction

**User Story:** As a backend developer, I want a unified abstraction for device discovery and command execution, so that the DeviceController and DeviceMonitorService can operate identically regardless of deployment mode.

#### Acceptance Criteria

1. THE DeviceProvider SHALL expose a method to discover connected devices that returns a DeviceDiscoveryResult containing a list of device serials and a list of ConnectedDevice objects
2. THE DeviceProvider SHALL expose methods to execute device commands (reboot, uninstall, screenshot, pull-logs, wifi-debug, firebase-debug) that return the same result types as the existing DeviceActionManager (DeviceMessageResult, UninstallAppResult, WifiDebugResult, ScreenshotCaptureResponse)
3. IF the Deployment_Mode configuration is set to `standalone`, THEN THE Backend SHALL instantiate a DeviceProvider implementation that delegates discovery to the existing DeviceCatalog and commands to the existing DeviceActionManager using direct adb
4. IF the Deployment_Mode configuration is set to `saas`, THEN THE Backend SHALL instantiate a DeviceProvider implementation that reads device state from AgentConnectionManager and relays commands to Agents via WebSocket
5. IF the DeviceProvider in saas mode cannot relay a command because no agent is connected for the target device, THEN THE DeviceProvider SHALL return an error result indicating the device is unreachable within 5 seconds of the command request
6. WHEN DeviceController or DeviceMonitorService invokes any DeviceProvider method, THE system SHALL produce identical response types and observable behavior regardless of which DeviceProvider implementation is active

### Requirement 2: Agent-Reported Device Discovery in SaaS Mode

**User Story:** As a SaaS user, I want to see my Android devices in the dashboard, so that I can manage them remotely through the web interface.

#### Acceptance Criteria

1. WHILE the Deployment_Mode is `saas`, THE DeviceController SHALL return device data sourced from AgentConnectionManager instead of calling DeviceCatalog
2. WHEN an Agent sends a DeviceList message, THE AgentConnectionManager SHALL replace the entire device registry for that Agent with the reported device list, removing any previously registered devices for that Agent that are no longer present in the message
3. WHEN an Agent disconnects, THE AgentConnectionManager SHALL remove all devices associated with that Agent from the registry
4. THE AgentConnectionManager SHALL associate each device with the tenant of the Agent that reported the device
5. WHEN the DeviceController receives a GET request for devices, THE DeviceProvider SHALL return only devices belonging to the requesting user's tenant
6. WHEN an Agent sends a DeviceList message, THE AgentConnectionManager SHALL store at minimum the device serial and device model for each reported device
7. IF an Agent sends a DeviceList message containing zero devices, THEN THE AgentConnectionManager SHALL clear all previously registered devices for that Agent from the registry

### Requirement 3: Device Change Broadcasting in SaaS Mode

**User Story:** As a SaaS user, I want real-time device status updates in my dashboard, so that I see devices appear and disappear without refreshing.

#### Acceptance Criteria

1. WHILE the Deployment_Mode is `saas`, THE DeviceMonitorService SHALL rely exclusively on Agent-reported DeviceList messages for device state and SHALL NOT poll adb directly
2. WHEN an Agent sends a DeviceList message whose set of device serial numbers differs from that Agent's previously recorded serial set, THE Backend SHALL broadcast the aggregated device list for the Agent's tenant to the `/topic/devices` WebSocket topic within 2 seconds of receiving the message
3. THE Backend SHALL broadcast device changes only to WebSocket subscribers belonging to the same tenant as the Agent that reported the change
4. WHEN an Agent disconnects, THE Backend SHALL remove that Agent's devices from the tenant's aggregated device list and broadcast the updated list to the tenant's WebSocket subscribers within 2 seconds of detecting the disconnection
5. WHILE the Deployment_Mode is `saas` and multiple Agents are connected for the same tenant, THE Backend SHALL maintain a per-Agent device set and broadcast the union of all connected Agents' device sets as the tenant's device list

### Requirement 4: Command Relay to Agent

**User Story:** As a SaaS user, I want to execute device commands (reboot, uninstall, screenshot, pull-logs) from the web interface, so that I can manage remote devices without physical access.

#### Acceptance Criteria

1. WHILE the Deployment_Mode is `saas`, WHEN the DeviceController receives a command request for a device, THE Command_Relay SHALL forward the command to the Agent that owns the device via the Agent's WebSocket connection
2. WHEN the Command_Relay needs to identify the owning Agent, THE Command_Relay SHALL use AgentConnectionManager to look up the Agent by tenant ID and device serial
3. IF no Agent is connected that owns the target device, THEN THE Command_Relay SHALL return an error response indicating the device is unreachable, including the device serial and the reason no agent was found
4. WHEN the Command_Relay sends a command to an Agent, THE Command_Relay SHALL wait for an OperationResult message from the Agent and return the operation success status and detail message to the caller
5. IF the Agent does not respond with an OperationResult within 30 seconds, THEN THE Command_Relay SHALL return a timeout error to the caller indicating the device serial and that the operation timed out
6. THE Command_Relay SHALL support relaying the following commands: reboot, uninstall (with package name), screenshot capture, and log pull
7. IF the command request specifies a command type not in the supported set (reboot, uninstall, screenshot capture, log pull), THEN THE Command_Relay SHALL return an error response indicating the command type is not supported
8. IF the WebSocket connection to the Agent is lost after a command is sent but before an OperationResult is received, THEN THE Command_Relay SHALL return an error response to the caller indicating the agent connection was lost during command execution

### Requirement 5: Logcat Streaming via Agent Relay

**User Story:** As a SaaS user, I want to stream live logcat output from my remote devices, so that I can debug applications in real time.

#### Acceptance Criteria

1. WHILE the Deployment_Mode is `saas`, WHEN a client requests to start logcat for a device, THE Backend SHALL locate the Agent that owns the device and send a StartLogcat command to that Agent within 2 seconds
2. WHEN the Agent streams a LogcatLine message, THE Backend SHALL forward a message containing the device serial, the log line text, and a server-received timestamp to the `/topic/logcat/{serial}` WebSocket topic within 500 milliseconds of receipt
3. WHEN a client requests to stop logcat for a device, THE Backend SHALL send a StopLogcat command to the owning Agent and remove the stream from the active streams registry
4. IF the Agent disconnects while logcat is streaming, THEN THE Backend SHALL send a stream-ended notification to the `/topic/logcat/{serial}` WebSocket topic indicating the stream has terminated due to agent disconnection, within 5 seconds of detecting the disconnection
5. IF no Agent is connected that owns the requested device when a client requests to start logcat, THEN THE Backend SHALL reject the request with an HTTP 503 Service Unavailable status and a message indicating no agent is available for that device
6. WHILE a logcat stream is active, THE Backend SHALL forward log lines with a maximum individual line length of 4096 characters, truncating any line that exceeds this limit

### Requirement 6: Local Mode Backward Compatibility

**User Story:** As a desktop user, I want the application to continue working with locally connected devices via adb, so that my existing workflow is unaffected.

#### Acceptance Criteria

1. WHILE the Deployment_Mode is `standalone`, THE DeviceController SHALL use DeviceCatalog for device discovery via direct adb calls
2. WHILE the Deployment_Mode is `standalone`, THE DeviceMonitorService SHALL poll adb every 3 seconds (fixed delay) for device changes, where a device change is defined as a difference in the set of connected device serial numbers compared to the previously known set
3. WHILE the Deployment_Mode is `standalone`, THE DeviceController SHALL execute device commands (reboot, uninstall, wifi-debug, firebase-debug, screenshot) via DeviceActionManager using direct adb calls
4. THE Backend SHALL default to `standalone` mode when the `deployment.mode` property is not explicitly set to `saas`
5. IF adb is unavailable or returns an error WHILE the Deployment_Mode is `standalone`, THEN THE DeviceController SHALL return an empty device list without propagating the error to the caller
6. WHEN the DeviceMonitorService detects a device change WHILE the Deployment_Mode is `standalone`, THE DeviceMonitorService SHALL broadcast the full updated device list to all connected WebSocket clients on the `/topic/devices` channel

### Requirement 7: Agent Authentication and Tenant Isolation

**User Story:** As a SaaS platform operator, I want agents to be authenticated and tenant-isolated, so that one tenant's devices are never visible to or controllable by another tenant.

#### Acceptance Criteria

1. WHEN an Agent connects via WebSocket, THE AgentWebSocketHandler SHALL authenticate the Agent using a JWT token provided as a query parameter named "token"
2. IF the JWT token is missing, malformed, has an invalid signature, or is expired, THEN THE AgentWebSocketHandler SHALL reject the connection with a policy violation close status without registering the Agent session
3. IF the JWT token is structurally valid but does not contain both a tenantId and a userId claim, THEN THE AgentWebSocketHandler SHALL reject the connection with a policy violation close status
4. WHEN the JWT token is successfully validated, THE AgentConnectionManager SHALL associate the Agent session with the tenantId extracted from the JWT claims before processing any messages from that Agent
5. THE Backend SHALL enforce that device queries and commands return only devices belonging to the authenticated user's tenantId, such that a request containing a valid tenant A token never returns or affects devices registered under tenant B
6. IF a command request targets a device belonging to a different tenant than the authenticated user's tenantId, THEN THE Backend SHALL reject the request with an authorization error response and SHALL NOT forward the command to any Agent
7. IF the JWT token expires while an Agent WebSocket session is active, THEN THE AgentWebSocketHandler SHALL close the connection within 60 seconds of token expiry

### Requirement 8: Agent Reconnection Resilience

**User Story:** As a SaaS user, I want my devices to reappear automatically when my agent reconnects after a network interruption, so that I do not need to manually intervene.

#### Acceptance Criteria

1. WHEN an Agent successfully reconnects after a disconnection, THE Agent SHALL re-send its current DeviceList to the Backend within 5 seconds of the connection being established
2. WHEN the Backend receives a DeviceList from a reconnected Agent, THE AgentConnectionManager SHALL replace any previous device registry entries for that Agent with the devices reported in the new DeviceList
3. WHILE an Agent is disconnected, THE Backend SHALL exclude that Agent's devices from available device listings and SHALL reject operations targeting those devices with an error indicating the device is unavailable
4. THE Agent SHALL use exponential backoff with an initial delay of 1 second, a multiplier of 2, and a maximum delay of 60 seconds when attempting to reconnect to the Backend
5. IF the Agent fails to send the DeviceList after reconnection, THEN THE Agent SHALL retry sending the DeviceList on the next successful connection attempt without requiring manual intervention
