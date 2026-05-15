package androidtoolkit.backend.config;

import androidtoolkit.backend.security.JwtService;
import androidtoolkit.backend.service.AgentConnectionManager;
import androidtoolkit.domain.agent.AgentMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles WebSocket connections from Agents.
 * Authenticates via JWT, routes incoming AgentMessages to appropriate services.
 */
@Component
public class AgentWebSocketHandler extends TextWebSocketHandler {

    private final JwtService jwtService;
    private final AgentConnectionManager connectionManager;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> sessionToAgent = new ConcurrentHashMap<>();

    public AgentWebSocketHandler(JwtService jwtService,
                                 AgentConnectionManager connectionManager,
                                 SimpMessagingTemplate messagingTemplate) {
        this.jwtService = jwtService;
        this.connectionManager = connectionManager;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = extractToken(session);
        if (token == null || !jwtService.isValid(token)) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        Claims claims = jwtService.parseToken(token);
        String agentId = session.getId();
        Long tenantId = claims.get("tenantId", Long.class);
        Long userId = claims.get("userId", Long.class);

        if (tenantId == null || userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        connectionManager.registerAgent(agentId, tenantId, userId, session);
        sessionToAgent.put(session.getId(), agentId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String agentId = sessionToAgent.get(session.getId());
        if (agentId == null) return;

        // Parse the message type from JSON
        var node = objectMapper.readTree(message.getPayload());
        String type = node.has("type") ? node.get("type").asText() : "";

        switch (type) {
            case "LogcatLine" -> {
                String serial = node.get("serial").asText();
                String line = node.get("line").asText();
                messagingTemplate.convertAndSend("/topic/logcat/" + serial,
                        Map.of("serial", serial, "line", line, "timestamp", System.currentTimeMillis()));
            }
            case "DeviceList" -> {
                // Register devices for this agent
                if (node.has("devices")) {
                    for (var device : node.get("devices")) {
                        String serial = device.get("serial").asText();
                        connectionManager.registerDeviceForAgent(agentId, serial);
                    }
                }
            }
            case "OperationResult" -> {
                // Could complete pending futures — for now just log
            }
            case "LogArchiveReady" -> {
                // Trigger file upload flow
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String agentId = sessionToAgent.remove(session.getId());
        if (agentId != null) {
            connectionManager.unregisterAgent(agentId);
        }
    }

    private String extractToken(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query != null && query.contains("token=")) {
            return query.substring(query.indexOf("token=") + 6).split("&")[0];
        }
        return null;
    }
}
