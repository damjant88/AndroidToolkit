package androidtoolkit.agent;

import androidtoolkit.agent.connection.ServerConnection;
import androidtoolkit.domain.DeviceInfo;
import androidtoolkit.domain.agent.AgentMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Periodically discovers connected Android devices via adb and sends
 * the device list to the backend via WebSocket.
 */
@Component
@EnableScheduling
public class DeviceDiscoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(DeviceDiscoveryScheduler.class);

    private final ServerConnection serverConnection;

    public DeviceDiscoveryScheduler(ServerConnection serverConnection) {
        this.serverConnection = serverConnection;
        // Also register as the onReconnected callback so devices are re-sent on reconnect
        serverConnection.setOnReconnected(this::sendDeviceList);
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 2000)
    public void sendDeviceList() {
        if (!serverConnection.isConnected()) {
            return;
        }

        try {
            List<DeviceInfo> devices = discoverDevices();
            serverConnection.send(new AgentMessage.DeviceList(devices));
        } catch (Exception e) {
            log.warn("Failed to send device list: {}", e.getMessage());
        }
    }

    private List<DeviceInfo> discoverDevices() {
        List<DeviceInfo> devices = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder("adb", "devices", "-l");
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("device") && !line.startsWith("List")) {
                        DeviceInfo device = parseDeviceLine(line);
                        if (device != null) {
                            devices.add(device);
                        }
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            log.warn("adb devices failed: {}", e.getMessage());
        }
        return devices;
    }

    private DeviceInfo parseDeviceLine(String line) {
        // Format: "SERIAL    device usb:X product:Y model:Z device:W transport_id:N"
        String[] parts = line.trim().split("\\s+");
        if (parts.length < 2 || !"device".equals(parts[1])) {
            return null;
        }

        String serial = parts[0];
        String model = "";
        String product = "";

        for (int i = 2; i < parts.length; i++) {
            if (parts[i].startsWith("model:")) {
                model = parts[i].substring(6).replace('_', ' ');
            } else if (parts[i].startsWith("product:")) {
                product = parts[i].substring(8);
            }
        }

        // Gather rich device info via adb shell commands
        String manufacturer = runAdbShell(serial, "getprop ro.product.manufacturer").trim();
        String osVersion = runAdbShell(serial, "getprop ro.build.version.release").trim();
        String wifiIp = runAdbShell(serial, "ip addr show wlan0 | grep 'inet '");
        wifiIp = extractIp(wifiIp);
        String mobileIp = runAdbShell(serial, "ip addr show rmnet_data0 | grep 'inet '");
        mobileIp = extractIp(mobileIp);
        String ipAddress = wifiIp.isEmpty() ? mobileIp : wifiIp;

        // Check for installed app (SafePath package pattern)
        String safePathPackage = findSafePathPackage(serial);
        boolean appInstalled = !safePathPackage.isEmpty();
        String pid = "";
        if (appInstalled) {
            pid = runAdbShell(serial, "pidof -s " + safePathPackage).trim();
        }

        return new DeviceInfo(serial, manufacturer, model, osVersion,
                wifiIp, mobileIp, ipAddress, safePathPackage, appInstalled, pid);
    }

    private String findSafePathPackage(String serial) {
        String packages = runAdbShell(serial, "pm list packages");
        for (String line : packages.split("\n")) {
            String pkg = line.replace("package:", "").trim();
            if (pkg.contains("safepath") || pkg.contains("familymode") || 
                pkg.contains("securefamily") || pkg.contains("safeandfound")) {
                return pkg;
            }
        }
        return "";
    }

    private String runAdbShell(String serial, String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("adb", "-s", serial, "shell", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            return output;
        } catch (Exception e) {
            return "";
        }
    }

    private String extractIp(String ifconfigOutput) {
        // Extract IP from "inet 192.168.1.100/24 ..." format
        if (ifconfigOutput == null || ifconfigOutput.isEmpty()) return "";
        for (String part : ifconfigOutput.trim().split("\\s+")) {
            if (part.contains(".") && part.contains("/")) {
                return part.split("/")[0];
            }
        }
        return "";
    }
}
