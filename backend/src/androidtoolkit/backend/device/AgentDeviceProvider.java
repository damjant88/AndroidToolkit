package androidtoolkit.backend.device;

import androidtoolkit.app.DeviceDiscoveryResult;
import androidtoolkit.app.DeviceMessageResult;
import androidtoolkit.app.ScreenshotCaptureResponse;
import androidtoolkit.app.UninstallAppResult;
import androidtoolkit.app.WifiDebugResult;
import androidtoolkit.backend.service.AgentConnectionManager;
import androidtoolkit.domain.agent.AgentCommand;
import androidtoolkit.domain.agent.AgentMessage.OperationResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * SaaS-mode implementation of {@link DeviceProvider}.
 * Reads device state from {@link AgentConnectionManager} and relays commands
 * to remote agents via {@link CommandRelay}.
 *
 * <p>Active only when {@code deployment.mode} is set to {@code saas}.
 * In this mode, devices are physically connected to agent machines and
 * commands must be forwarded over WebSocket rather than executed locally via adb.
 */
@Component
@ConditionalOnProperty(name = "deployment.mode", havingValue = "saas")
public class AgentDeviceProvider implements DeviceProvider {

    private final AgentConnectionManager connectionManager;
    private final CommandRelay commandRelay;

    public AgentDeviceProvider(AgentConnectionManager connectionManager, CommandRelay commandRelay) {
        this.connectionManager = connectionManager;
        this.commandRelay = commandRelay;
    }

    @Override
    public DeviceDiscoveryResult discoverDevices(Long tenantId) {
        return connectionManager.getDevicesForTenant(tenantId);
    }

    @Override
    public DeviceMessageResult reboot(Long tenantId, String serial) {
        OperationResult result = commandRelay.execute(tenantId, serial, new AgentCommand.Reboot(serial));
        return new DeviceMessageResult(result.detail());
    }

    @Override
    public UninstallAppResult uninstall(Long tenantId, String serial, String packageName) {
        OperationResult result = commandRelay.execute(tenantId, serial,
                new AgentCommand.UninstallApp(serial, packageName));
        return new UninstallAppResult(result.success(), result.detail());
    }

    @Override
    public ScreenshotCaptureResponse screenshot(Long tenantId, String serial) {
        OperationResult result = commandRelay.execute(tenantId, serial,
                new AgentCommand.CaptureScreenshot(serial));
        return new ScreenshotCaptureResponse(result.detail(), result.detail());
    }

    @Override
    public DeviceMessageResult pullLogs(Long tenantId, String serial) {
        OperationResult result = commandRelay.execute(tenantId, serial,
                new AgentCommand.PullLogs(serial, "/tmp/logs/" + serial));
        return new DeviceMessageResult(result.detail());
    }

    @Override
    public WifiDebugResult toggleWifiDebug(Long tenantId, String serial,
                                           String ipAddress, boolean wifiDebugSession, boolean hasWifiIp) {
        OperationResult result = commandRelay.execute(tenantId, serial,
                new AgentCommand.ToggleWifiDebug(serial, ipAddress, wifiDebugSession, hasWifiIp));
        // Parse the result detail to construct WifiDebugResult
        String detail = result.detail();
        if (detail.startsWith("CONNECTED:")) {
            return new WifiDebugResult(false, true, detail.substring(10), "Disconnect");
        } else if (detail.startsWith("DISCONNECTED:")) {
            return new WifiDebugResult(false, false, "", "Connect");
        } else {
            return new WifiDebugResult(true, wifiDebugSession, ipAddress, detail);
        }
    }

    @Override
    public DeviceMessageResult enableFirebaseDebug(Long tenantId, String serial, String packageName) {
        OperationResult result = commandRelay.execute(tenantId, serial,
                new AgentCommand.EnableFirebaseDebug(serial, packageName));
        return new DeviceMessageResult(result.detail());
    }
}
