package androidtoolkit.backend.controller;

import androidtoolkit.backend.security.TenantContext;
import androidtoolkit.backend.service.AgentConnectionManager;
import androidtoolkit.domain.agent.AgentCommand;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST endpoints for starting/stopping logcat streams via Agent relay.
 * STOMP topic /topic/logcat/{serial} delivers the actual lines.
 */
@RestController
@RequestMapping("/api/devices")
public class LogcatStreamingController {

    private final AgentConnectionManager connectionManager;
    private final Set<String> activeStreams = ConcurrentHashMap.newKeySet();

    public LogcatStreamingController(AgentConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @PostMapping("/{serial}/logcat/start")
    public Map<String, Object> startLogcat(@PathVariable String serial) {
        Long tenantId = TenantContext.getTenantId();
        String agentId = connectionManager.findAgentForDevice(tenantId, serial)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "No agent connected for device: " + serial));

        String requestId = UUID.randomUUID().toString();
        connectionManager.sendToAgent(agentId, new AgentCommand.StartLogcat(serial, requestId));
        activeStreams.add(tenantId + ":" + serial);

        return Map.of("status", "started", "requestId", requestId, "topic", "/topic/logcat/" + serial);
    }

    @PostMapping("/{serial}/logcat/stop")
    public Map<String, Object> stopLogcat(@PathVariable String serial) {
        Long tenantId = TenantContext.getTenantId();
        String agentId = connectionManager.findAgentForDevice(tenantId, serial)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "No agent connected for device: " + serial));

        connectionManager.sendToAgent(agentId, new AgentCommand.StopLogcat(serial));
        activeStreams.remove(tenantId + ":" + serial);

        return Map.of("status", "stopped");
    }

    @GetMapping("/streams/active")
    public Map<String, Object> getActiveStreams() {
        return Map.of("count", activeStreams.size(), "streams", activeStreams);
    }
}
