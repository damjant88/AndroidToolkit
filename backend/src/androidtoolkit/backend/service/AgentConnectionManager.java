package androidtoolkit.backend.service;

import androidtoolkit.app.ConnectedDevice;
import androidtoolkit.app.DeviceDiscoveryResult;
import androidtoolkit.domain.DeviceInfo;
import androidtoolkit.domain.agent.AgentCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages connected Agent WebSocket sessions and their reported devices.
 * Maintains a per-agent device registry with tenant scoping.
 */
@Service
public class AgentConnectionManager {

    /**
     * Internal record representing a connected agent session with its device state.
     */
    private static class AgentSession {
        private final Long tenantId;
        private final Long userId;
        private final WebSocketSession session;
        private final Instant connectedAt;
        private final Instant tokenExpiresAt;
        private volatile List<DeviceInfo> devices;
        private volatile Set<String> deviceSerials;

        AgentSession(Long tenantId, Long userId, WebSocketSession session,
                     Instant connectedAt, Instant tokenExpiresAt) {
            this.tenantId = tenantId;
            this.userId = userId;
            this.session = session;
            this.connectedAt = connectedAt;
            this.tokenExpiresAt = tokenExpiresAt;
            this.devices = List.of();
            this.deviceSerials = Set.of();
        }

        Long tenantId() { return tenantId; }
        Long userId() { return userId; }
        WebSocketSession session() { return session; }
        Instant connectedAt() { return connectedAt; }
        Instant tokenExpiresAt() { return tokenExpiresAt; }
        List<DeviceInfo> devices() { return devices; }
        Set<String> deviceSerials() { return deviceSerials; }
    }

    private final ConcurrentHashMap<String, AgentSession> agents = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> deviceToAgent = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Registers a new agent session. Called when an agent connects and authenticates.
     */
    public void registerAgent(String agentId, Long tenantId, Long userId, WebSocketSession session) {
        registerAgent(agentId, tenantId, userId, session, Instant.now(), null);
    }

    /**
     * Registers a new agent session with explicit timestamps.
     */
    public void registerAgent(String agentId, Long tenantId, Long userId, WebSocketSession session,
                              Instant connectedAt, Instant tokenExpiresAt) {
        agents.put(agentId, new AgentSession(tenantId, userId, session, connectedAt, tokenExpiresAt));
    }

    /**
     * Replaces the entire device registry for an agent with the new list.
     * Removes any previously registered devices for that agent that are no longer present.
     *
     * @param agentId the agent identifier
     * @param devices the new complete device list reported by the agent
     */
    public void updateDeviceList(String agentId, List<DeviceInfo> devices) {
        AgentSession agentSession = agents.get(agentId);
        if (agentSession == null) {
            return;
        }

        // Remove old device-to-agent mappings
        Set<String> oldSerials = agentSession.deviceSerials();
        for (String serial : oldSerials) {
            deviceToAgent.remove(tenantDeviceKey(agentSession.tenantId(), serial));
        }

        // Build new serial set and store new device list
        Set<String> newSerials = devices.stream()
                .map(DeviceInfo::getSerialNumber)
                .collect(Collectors.toUnmodifiableSet());

        agentSession.devices = List.copyOf(devices);
        agentSession.deviceSerials = newSerials;

        // Add new device-to-agent mappings
        for (String serial : newSerials) {
            deviceToAgent.put(tenantDeviceKey(agentSession.tenantId(), serial), agentId);
        }
    }

    /**
     * Returns the aggregated device list for a tenant as the union of all connected agents' devices.
     *
     * @param tenantId the tenant identifier
     * @return DeviceDiscoveryResult containing all devices for the tenant
     */
    public DeviceDiscoveryResult getDevicesForTenant(Long tenantId) {
        List<String> allSerials = new ArrayList<>();
        List<ConnectedDevice> allDevices = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, AgentSession> entry : agents.entrySet()) {
            AgentSession agentSession = entry.getValue();
            // If tenantId is null (unauthenticated/local dev), return all devices
            if (tenantId == null || agentSession.tenantId().equals(tenantId)) {
                for (DeviceInfo device : agentSession.devices()) {
                    allSerials.add(device.getSerialNumber());
                    allDevices.add(new ConnectedDevice(
                            index++,
                            device.getSerialNumber(),
                            device.getModel(),
                            device
                    ));
                }
            }
        }

        return new DeviceDiscoveryResult(allSerials, allDevices);
    }

    /**
     * Returns all agent IDs connected for a given tenant.
     *
     * @param tenantId the tenant identifier
     * @return set of agent IDs belonging to the tenant
     */
    public Set<String> getAgentsForTenant(Long tenantId) {
        return agents.entrySet().stream()
                .filter(entry -> entry.getValue().tenantId().equals(tenantId))
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Checks if the device set has changed compared to the agent's previously recorded state.
     * Compares the set of serial numbers.
     *
     * @param agentId the agent identifier
     * @param newDevices the new device list to compare against
     * @return true if the serial sets differ, false if identical
     */
    public boolean hasDeviceSetChanged(String agentId, List<DeviceInfo> newDevices) {
        AgentSession agentSession = agents.get(agentId);
        if (agentSession == null) {
            return !newDevices.isEmpty();
        }

        Set<String> newSerials = newDevices.stream()
                .map(DeviceInfo::getSerialNumber)
                .collect(Collectors.toSet());

        return !agentSession.deviceSerials().equals(newSerials);
    }

    /**
     * Returns the tenant ID for a given agent.
     *
     * @param agentId the agent identifier
     * @return the tenant ID, or null if the agent is not registered
     */
    public Long getTenantIdForAgent(String agentId) {
        AgentSession agentSession = agents.get(agentId);
        return agentSession != null ? agentSession.tenantId() : null;
    }

    /**
     * @deprecated Use {@link #updateDeviceList(String, List)} instead for full device list replacement.
     */
    @Deprecated
    public void registerDeviceForAgent(String agentId, String deviceSerial) {
        AgentSession agentSession = agents.get(agentId);
        if (agentSession != null) {
            // Add to existing device serials
            Set<String> updatedSerials = new HashSet<>(agentSession.deviceSerials());
            updatedSerials.add(deviceSerial);
            agentSession.deviceSerials = Set.copyOf(updatedSerials);
            deviceToAgent.put(tenantDeviceKey(agentSession.tenantId(), deviceSerial), agentId);
        }
    }

    /**
     * Unregisters an agent and clears all device mappings for that agent.
     */
    public void unregisterAgent(String agentId) {
        AgentSession agentSession = agents.remove(agentId);
        if (agentSession != null) {
            // Remove all device-to-agent mappings for this agent
            for (String serial : agentSession.deviceSerials()) {
                deviceToAgent.remove(tenantDeviceKey(agentSession.tenantId(), serial));
            }
        }
    }

    /**
     * Sends a command to a specific agent via its WebSocket session.
     */
    public void sendToAgent(String agentId, AgentCommand command) {
        AgentSession agentSession = agents.get(agentId);
        if (agentSession != null && agentSession.session().isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(command);
                agentSession.session().sendMessage(new TextMessage(json));
            } catch (IOException e) {
                throw new RuntimeException("Failed to send command to agent: " + agentId, e);
            }
        }
    }

    /**
     * Finds the agent that owns a specific device for a given tenant.
     */
    public Optional<String> findAgentForDevice(Long tenantId, String deviceSerial) {
        return Optional.ofNullable(deviceToAgent.get(tenantDeviceKey(tenantId, deviceSerial)));
    }

    /**
     * Checks if an agent is currently connected with an open session.
     */
    public boolean isAgentConnected(String agentId) {
        AgentSession agentSession = agents.get(agentId);
        return agentSession != null && agentSession.session().isOpen();
    }

    /**
     * Returns the token expiry time for a given agent, or null if not registered.
     */
    public Instant getTokenExpiresAt(String agentId) {
        AgentSession agentSession = agents.get(agentId);
        return agentSession != null ? agentSession.tokenExpiresAt() : null;
    }

    /**
     * Returns all registered agent IDs. Useful for scheduled token expiry checks.
     */
    public Set<String> getAllAgentIds() {
        return Set.copyOf(agents.keySet());
    }

    /**
     * Returns the WebSocket session for a given agent, or null if not registered.
     */
    public WebSocketSession getSession(String agentId) {
        AgentSession agentSession = agents.get(agentId);
        return agentSession != null ? agentSession.session() : null;
    }

    private String tenantDeviceKey(Long tenantId, String deviceSerial) {
        return tenantId + ":" + deviceSerial;
    }
}
