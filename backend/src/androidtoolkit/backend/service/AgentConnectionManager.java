package androidtoolkit.backend.service;

import androidtoolkit.domain.agent.AgentCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages connected Agent WebSocket sessions and routes commands to agents.
 */
@Service
public class AgentConnectionManager {

    private record AgentInfo(Long tenantId, Long userId, WebSocketSession session,
                             Map<String, String> deviceSerials) {}

    private final ConcurrentHashMap<String, AgentInfo> agents = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> deviceToAgent = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void registerAgent(String agentId, Long tenantId, Long userId, WebSocketSession session) {
        agents.put(agentId, new AgentInfo(tenantId, userId, session, new ConcurrentHashMap<>()));
    }

    public void registerDeviceForAgent(String agentId, String deviceSerial) {
        AgentInfo info = agents.get(agentId);
        if (info != null) {
            info.deviceSerials().put(deviceSerial, agentId);
            deviceToAgent.put(tenantDeviceKey(info.tenantId(), deviceSerial), agentId);
        }
    }

    public void unregisterAgent(String agentId) {
        AgentInfo info = agents.remove(agentId);
        if (info != null) {
            info.deviceSerials().keySet().forEach(serial ->
                    deviceToAgent.remove(tenantDeviceKey(info.tenantId(), serial)));
        }
    }

    public void sendToAgent(String agentId, AgentCommand command) {
        AgentInfo info = agents.get(agentId);
        if (info != null && info.session().isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(command);
                info.session().sendMessage(new TextMessage(json));
            } catch (IOException e) {
                throw new RuntimeException("Failed to send command to agent: " + agentId, e);
            }
        }
    }

    public Optional<String> findAgentForDevice(Long tenantId, String deviceSerial) {
        return Optional.ofNullable(deviceToAgent.get(tenantDeviceKey(tenantId, deviceSerial)));
    }

    public boolean isAgentConnected(String agentId) {
        AgentInfo info = agents.get(agentId);
        return info != null && info.session().isOpen();
    }

    private String tenantDeviceKey(Long tenantId, String deviceSerial) {
        return tenantId + ":" + deviceSerial;
    }
}
