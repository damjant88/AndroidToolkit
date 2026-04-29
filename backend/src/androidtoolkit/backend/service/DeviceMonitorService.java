package androidtoolkit.backend.service;

import androidtoolkit.app.DeviceCatalog;
import androidtoolkit.app.DeviceDiscoveryRequest;
import androidtoolkit.app.DeviceDiscoveryResult;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Polls for device changes every 3 seconds and broadcasts updates via WebSocket.
 * Clients subscribe to /topic/devices and receive the full device list whenever it changes.
 */
@Service
public class DeviceMonitorService {

    private final DeviceCatalog deviceCatalog;
    private final SimpMessagingTemplate messagingTemplate;
    private List<String> lastKnownSerials = new ArrayList<>();

    public DeviceMonitorService(DeviceCatalog deviceCatalog, SimpMessagingTemplate messagingTemplate) {
        this.deviceCatalog = deviceCatalog;
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedDelay = 3000)
    public void checkForDeviceChanges() {
        try {
            DeviceDiscoveryResult result = deviceCatalog.discoverDevices(new DeviceDiscoveryRequest());
            List<String> currentSerials = result.getSerials();

            if (!currentSerials.equals(lastKnownSerials)) {
                lastKnownSerials = new ArrayList<>(currentSerials);
                // Broadcast the full device list to all connected WebSocket clients
                messagingTemplate.convertAndSend("/topic/devices", result);
            }
        } catch (RuntimeException e) {
            System.err.println("Device monitor check failed: " + e.getMessage());
        }
    }

    /**
     * Force a broadcast of the current device state (called after install/uninstall/etc.)
     */
    public void broadcastDeviceUpdate() {
        try {
            DeviceDiscoveryResult result = deviceCatalog.discoverDevices(new DeviceDiscoveryRequest());
            lastKnownSerials = new ArrayList<>(result.getSerials());
            messagingTemplate.convertAndSend("/topic/devices", result);
        } catch (RuntimeException e) {
            System.err.println("Device broadcast failed: " + e.getMessage());
        }
    }
}
