package androidtoolkit.backend.service;

import androidtoolkit.app.ConnectedDevice;
import androidtoolkit.app.DeviceCatalog;
import androidtoolkit.app.DeviceDiscoveryRequest;
import androidtoolkit.app.DeviceDiscoveryResult;
import androidtoolkit.domain.DeviceInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Polls for device changes every 3 seconds and broadcasts updates via WebSocket.
 * Clients subscribe to /topic/devices and receive the full device list whenever it changes.
 *
 * In SaaS mode, scheduled polling is disabled — device broadcasts are triggered
 * by AgentWebSocketHandler when DeviceList messages arrive from agents.
 */
@Service
public class DeviceMonitorService {

    private final DeviceCatalog deviceCatalog;
    private final SimpMessagingTemplate messagingTemplate;
    private final LogcatStreamManager logcatStreamManager;
    private final String deploymentMode;
    private List<String> lastKnownSerials = new ArrayList<>();

    public DeviceMonitorService(DeviceCatalog deviceCatalog, SimpMessagingTemplate messagingTemplate,
                                LogcatStreamManager logcatStreamManager,
                                @Value("${deployment.mode:standalone}") String deploymentMode) {
        this.deviceCatalog = deviceCatalog;
        this.messagingTemplate = messagingTemplate;
        this.logcatStreamManager = logcatStreamManager;
        this.deploymentMode = deploymentMode;
    }

    @Scheduled(fixedDelay = 3000)
    public void checkForDeviceChanges() {
        // In SaaS mode, device broadcasts are triggered by AgentWebSocketHandler
        // when DeviceList messages arrive — no local adb polling needed.
        if ("saas".equalsIgnoreCase(deploymentMode)) {
            return;
        }

        try {
            DeviceDiscoveryResult result = deviceCatalog.discoverDevices(new DeviceDiscoveryRequest());
            List<String> currentSerials = result.getSerials();

            if (!currentSerials.equals(lastKnownSerials)) {
                lastKnownSerials = new ArrayList<>(currentSerials);
                // Broadcast the full device list to all connected WebSocket clients
                messagingTemplate.convertAndSend("/topic/devices", result);
            }

            // Notify LogcatStreamManager of current devices (always, since PID may change without serial list changing)
            try {
                List<DeviceInfo> currentDevices = result.getDevices().stream()
                        .map(ConnectedDevice::getDeviceInfo)
                        .toList();
                logcatStreamManager.onDevicesChanged(currentDevices);
            } catch (Exception e) {
                System.err.println("Logcat stream notification failed: " + e.getMessage());
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

            // Notify LogcatStreamManager of current devices
            try {
                List<DeviceInfo> currentDevices = result.getDevices().stream()
                        .map(ConnectedDevice::getDeviceInfo)
                        .toList();
                logcatStreamManager.onDevicesChanged(currentDevices);
            } catch (Exception e) {
                System.err.println("Logcat stream notification failed: " + e.getMessage());
            }
        } catch (RuntimeException e) {
            System.err.println("Device broadcast failed: " + e.getMessage());
        }
    }
}
