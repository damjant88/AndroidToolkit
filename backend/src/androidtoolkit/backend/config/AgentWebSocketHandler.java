package androidtoolkit.backend.config;

import androidtoolkit.backend.device.CommandRelay;
import androidtoolkit.backend.device.LogcatRelayService;
import androidtoolkit.backend.security.JwtService;
import androidtoolkit.backend.service.AgentConnectionManager;
import androidtoolkit.domain.DeviceInfo;
import androidtoolkit.domain.agent.AgentMessage.OperationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles WebSocket connections from Agents.
 * Authenticates via JWT, routes incoming AgentMessages to appropriate services.
 */
@Component
public class AgentWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AgentWebSocketHandler.class);

    private final JwtService jwtService;
    private final AgentConnectionManager connectionManager;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> sessionToAgent = new ConcurrentHashMap<>();

    @Value("${deployment.mode:standalone}")
    private String deploymentMode;

    /**
     * Optional dependency — only present in SaaS mode (deployment.mode=saas).
     * Used to complete pending command futures when OperationResult arrives from an agent.
     */
    @Autowired(required = false)
    private CommandRelay commandRelay;

    /**
     * Optional dependency — only present in SaaS mode (deployment.mode=saas).
     * Used to forward logcat lines to the appropriate WebSocket topic with truncation.
     */
    @Autowired(required = false)
    private LogcatRelayService logcatRelayService;

    public AgentWebSocketHandler(JwtService jwtService,
                                 AgentConnectionManager connectionManager,
                                 SimpMessagingTemplate messagingTemplate) {
        this.jwtService = jwtService;
        this.connectionManager = connectionManager;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // In standalone mode, allow agent connections without authentication
        if ("standalone".equalsIgnoreCase(deploymentMode)) {
            String agentId = session.getId();
            connectionManager.registerAgent(agentId, 0L, 0L, session);
            sessionToAgent.put(session.getId(), agentId);
            return;
        }

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
            case "DeviceList" -> handleDeviceList(agentId, node);
            case "OperationResult" -> handleOperationResult(node);
            case "LogcatLine" -> handleLogcatLine(node);
            case "LogArchiveReady" -> {
                // Trigger file upload flow
            }
        }
    }

    /**
     * Handles a DeviceList message from an agent. Parses the full device list,
     * checks if the device set has changed, updates the registry, and broadcasts
     * the updated tenant device list if the set changed.
     */
    private void handleDeviceList(String agentId, com.fasterxml.jackson.databind.JsonNode node) {
        // In standalone mode, DeviceMonitorService already handles local device polling.
        // Skip agent broadcast to avoid overriding local detection results.
        if ("standalone".equalsIgnoreCase(deploymentMode)) return;

        List<DeviceInfo> devices = new java.util.ArrayList<>();
        if (node.has("devices")) {
            for (var deviceNode : node.get("devices")) {
                devices.add(parseDeviceInfo(deviceNode));
            }
        }

        // Check if device set changed before updating (compare against current state)
        boolean changed = connectionManager.hasDeviceSetChanged(agentId, devices);

        // Replace the entire device registry for this agent
        connectionManager.updateDeviceList(agentId, devices);

        // If the device set changed, broadcast updated tenant device list
        if (changed) {
            Long tenantId = connectionManager.getTenantIdForAgent(agentId);
            if (tenantId != null) {
                var discoveryResult = connectionManager.getDevicesForTenant(tenantId);
                messagingTemplate.convertAndSend(
                        "/topic/devices/tenant/" + tenantId,
                        discoveryResult
                );
                messagingTemplate.convertAndSend("/topic/devices", discoveryResult);
            }
        }
    }

    /**
     * Handles an OperationResult message from an agent. Extracts the requestId,
     * success, and detail fields and routes them to the CommandRelay to complete
     * the pending command future.
     */
    private void handleOperationResult(com.fasterxml.jackson.databind.JsonNode node) {
        if (commandRelay == null) return;

        String requestId = node.has("requestId") ? node.get("requestId").asText() : null;
        boolean success = node.has("success") && node.get("success").asBoolean();
        String detail = node.has("detail") ? node.get("detail").asText() : "";

        if (requestId != null) {
            OperationResult result = new OperationResult(requestId, success, detail);
            commandRelay.completeCommand(requestId, result);
        }
    }

    /**
     * Handles a LogcatLine message from an agent. Extracts the serial, line, and
     * timestamp fields and forwards them to the LogcatRelayService which handles
     * truncation and WebSocket broadcasting.
     */
    private void handleLogcatLine(com.fasterxml.jackson.databind.JsonNode node) {
        if (logcatRelayService == null) {
            // Fallback: forward directly if LogcatRelayService is not available (standalone mode)
            String serial = node.has("serial") ? node.get("serial").asText() : "";
            String line = node.has("line") ? node.get("line").asText() : "";
            messagingTemplate.convertAndSend("/topic/logcat/" + serial,
                    Map.of("serial", serial, "line", line, "timestamp", System.currentTimeMillis()));
            return;
        }

        String serial = node.has("serial") ? node.get("serial").asText() : "";
        String line = node.has("line") ? node.get("line").asText() : "";
        long timestamp = node.has("timestamp") ? node.get("timestamp").asLong() : System.currentTimeMillis();

        logcatRelayService.forwardLine(serial, line, timestamp);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String agentId = sessionToAgent.remove(session.getId());
        if (agentId != null) {
            // Get tenant ID BEFORE unregistering (unregister removes the agent data)
            Long tenantId = connectionManager.getTenantIdForAgent(agentId);

            // Fail any pending commands for this agent
            if (commandRelay != null) {
                commandRelay.failCommandsForAgent(agentId);
            }

            // Notify logcat relay service of agent disconnection
            if (logcatRelayService != null) {
                logcatRelayService.onAgentDisconnected(agentId);
            }

            // Unregister the agent (removes devices from registry)
            connectionManager.unregisterAgent(agentId);

            // In standalone mode, DeviceMonitorService handles device broadcasts — skip here
            if ("standalone".equalsIgnoreCase(deploymentMode)) return;

            // Broadcast updated device list for the agent's tenant
            if (tenantId != null) {
                var discoveryResult = connectionManager.getDevicesForTenant(tenantId);
                messagingTemplate.convertAndSend(
                        "/topic/devices/tenant/" + tenantId,
                        discoveryResult
                );
                messagingTemplate.convertAndSend("/topic/devices", discoveryResult);
            }
        }
    }

    /**
     * Scheduled task that checks all connected agent sessions for JWT token expiry.
     * Runs every 60 seconds. If a token has expired, the WebSocket session is closed
     * with POLICY_VIOLATION status to enforce re-authentication.
     */
    @Scheduled(fixedDelay = 60000)
    public void checkTokenExpiry() {
        Instant now = Instant.now();
        for (String agentId : connectionManager.getAllAgentIds()) {
            Instant expiresAt = connectionManager.getTokenExpiresAt(agentId);
            if (expiresAt != null && expiresAt.isBefore(now)) {
                WebSocketSession session = connectionManager.getSession(agentId);
                if (session != null && session.isOpen()) {
                    try {
                        session.close(CloseStatus.POLICY_VIOLATION);
                    } catch (IOException e) {
                        log.warn("Failed to close expired session for agent {}: {}",
                                agentId, e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Parses a DeviceInfo object from a JSON node. Handles missing fields gracefully
     * by defaulting to empty strings or false.
     */
    private DeviceInfo parseDeviceInfo(com.fasterxml.jackson.databind.JsonNode deviceNode) {
        return new DeviceInfo(
                getTextOrDefault(deviceNode, "serialNumber", ""),
                getTextOrDefault(deviceNode, "manufacturer", ""),
                getTextOrDefault(deviceNode, "model", ""),
                getTextOrDefault(deviceNode, "osVersion", ""),
                getTextOrDefault(deviceNode, "wifiIp", ""),
                getTextOrDefault(deviceNode, "mobileIp", ""),
                getTextOrDefault(deviceNode, "ipAddress", ""),
                getTextOrDefault(deviceNode, "safePathPackage", ""),
                deviceNode.has("appInstalled") && deviceNode.get("appInstalled").asBoolean(),
                getTextOrDefault(deviceNode, "pid", "")
        );
    }

    private String getTextOrDefault(com.fasterxml.jackson.databind.JsonNode node, String field, String defaultValue) {
        return node.has(field) ? node.get(field).asText() : defaultValue;
    }

    private String extractToken(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query != null && query.contains("token=")) {
            return query.substring(query.indexOf("token=") + 6).split("&")[0];
        }
        return null;
    }
}
