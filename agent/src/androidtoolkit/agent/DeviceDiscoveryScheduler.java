package androidtoolkit.agent;

import androidtoolkit.agent.connection.ServerConnection;
import androidtoolkit.domain.DeviceInfo;
import androidtoolkit.domain.agent.AgentMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.*;

/**
 * Fast device discovery using two-phase approach:
 * 1. Instant detection via "adb track-devices" (push-based, ~0ms latency on change)
 * 2. Background enrichment of device info (packages, IPs, PID)
 *
 * When a device connects/disconnects, a basic DeviceList is sent immediately
 * (serial + model from adb devices -l). Rich info is gathered asynchronously
 * and an updated DeviceList is sent when ready.
 */
@Component
@EnableScheduling
public class DeviceDiscoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(DeviceDiscoveryScheduler.class);

    private final ServerConnection serverConnection;
    private final ExecutorService enrichmentExecutor = Executors.newSingleThreadExecutor();
    private final ConcurrentHashMap<String, DeviceInfo> deviceCache = new ConcurrentHashMap<>();

    private volatile Process trackProcess;
    private volatile boolean running = true;
    private volatile Set<String> lastKnownSerials = Set.of();

    public DeviceDiscoveryScheduler(ServerConnection serverConnection) {
        this.serverConnection = serverConnection;
        serverConnection.setOnReconnected(this::sendCurrentDeviceList);
    }

    @PostConstruct
    public void start() {
        // Start the device tracking thread
        Thread tracker = new Thread(this::trackDevices, "adb-track-devices");
        tracker.setDaemon(true);
        tracker.start();

        // Also do an immediate scan after a short delay (connection may not be ready at @PostConstruct time)
        enrichmentExecutor.submit(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException e) { return; }
            onDeviceChangeDetected();
        });
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (trackProcess != null) {
            trackProcess.destroyForcibly();
        }
        enrichmentExecutor.shutdownNow();
    }

    /**
     * Listens for device changes using fast 1-second polling of "adb devices".
     * This is nearly instant since adb devices just queries the local adb server.
     */
    private void trackDevices() {
        while (running) {
            try {
                onDeviceChangeDetected();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                log.debug("Device tracking error: {}", e.getMessage());
                try { Thread.sleep(2000); } catch (InterruptedException ie) { break; }
            }
        }
    }

    /**
     * Called when adb track-devices detects a change. Immediately sends a basic
     * device list, then enriches in background.
     */
    private void onDeviceChangeDetected() {
        try {
            // Phase 1: Fast scan — just serial + model (instant, no shell commands)
            List<DeviceInfo> basicDevices = fastScan();
            Set<String> currentSerials = new HashSet<>();
            for (DeviceInfo d : basicDevices) {
                currentSerials.add(d.getSerialNumber());
            }

            // Only send if the serial set actually changed
            if (!currentSerials.equals(lastKnownSerials)) {
                lastKnownSerials = currentSerials;

                // Remove disconnected devices from cache
                deviceCache.keySet().removeIf(s -> !currentSerials.contains(s));

                // Send basic list immediately (devices appear in UI within ~1s)
                sendDeviceList(basicDevices);

                // Phase 2: Enrich in background, then send updated list
                enrichmentExecutor.submit(() -> enrichAndSend(basicDevices));
            }
        } catch (Exception e) {
            log.warn("Device change handling failed: {}", e.getMessage());
        }
    }

    /**
     * Fast scan: runs "adb devices -l" which returns instantly with serial + model.
     * No shell commands to the device — just the host-side device list.
     */
    private List<DeviceInfo> fastScan() {
        List<DeviceInfo> devices = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder("adb", "devices", "-l");
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("\tdevice") || (line.contains("device") && !line.startsWith("List") && line.contains("model:"))) {
                        DeviceInfo device = parseBasicDeviceLine(line);
                        if (device != null) {
                            devices.add(device);
                        }
                    }
                }
            }
            process.waitFor(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Fast scan failed: {}", e.getMessage());
        }
        return devices;
    }

    /**
     * Parse basic device info from "adb devices -l" output.
     * Only extracts serial, model, and product — no shell commands needed.
     */
    private DeviceInfo parseBasicDeviceLine(String line) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length < 2) return null;

        String serial = parts[0];
        // Skip non-device lines
        if (serial.equals("List") || serial.isEmpty()) return null;
        if (!parts[1].equals("device")) return null;

        String model = "";
        for (int i = 2; i < parts.length; i++) {
            if (parts[i].startsWith("model:")) {
                model = parts[i].substring(6).replace('_', ' ');
            }
        }

        // Return cached enriched info if available, otherwise basic info
        DeviceInfo cached = deviceCache.get(serial);
        if (cached != null) {
            return cached;
        }

        return new DeviceInfo(serial, "", model, "", "", "", "", "", false, "");
    }

    /**
     * Enriches device info with shell commands (manufacturer, OS, IP, packages, PID)
     * then sends the updated device list.
     */
    private void enrichAndSend(List<DeviceInfo> basicDevices) {
        try {
            List<DeviceInfo> enrichedDevices = new ArrayList<>();
            for (DeviceInfo basic : basicDevices) {
                String serial = basic.getSerialNumber();
                DeviceInfo enriched = enrichDevice(serial, basic.getModel());
                deviceCache.put(serial, enriched);
                enrichedDevices.add(enriched);
            }
            sendDeviceList(enrichedDevices);
        } catch (Exception e) {
            log.warn("Enrichment failed: {}", e.getMessage());
        }
    }

    /**
     * Gathers full device info via adb shell commands.
     */
    private DeviceInfo enrichDevice(String serial, String model) {
        String manufacturer = runAdbShell(serial, "getprop ro.product.manufacturer").trim();
        String osVersion = runAdbShell(serial, "getprop ro.build.version.release").trim();
        String wifiIp = extractIp(runAdbShell(serial, "ip addr show wlan0 | grep 'inet '"));
        String mobileIp = extractIp(runAdbShell(serial, "ip addr show rmnet_data0 | grep 'inet '"));
        String ipAddress = wifiIp.isEmpty() ? mobileIp : wifiIp;

        String safePathPackage = findSafePathPackage(serial);
        boolean appInstalled = !safePathPackage.isEmpty();
        String pid = "";
        if (appInstalled) {
            pid = runAdbShell(serial, "pidof -s " + safePathPackage).trim();
        }

        if (model.isEmpty()) {
            model = runAdbShell(serial, "getprop ro.product.model").trim().replace('_', ' ');
        }

        return new DeviceInfo(serial, manufacturer, model, osVersion,
                wifiIp, mobileIp, ipAddress, safePathPackage, appInstalled, pid);
    }

    /**
     * Periodic refresh of rich device info (every 10s) to keep IPs, PIDs current.
     * This does NOT do detection — just refreshes cached info for already-known devices.
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 5000)
    public void refreshDeviceInfo() {
        if (!serverConnection.isConnected() || deviceCache.isEmpty()) return;

        try {
            List<DeviceInfo> refreshed = new ArrayList<>();
            for (String serial : deviceCache.keySet()) {
                DeviceInfo cached = deviceCache.get(serial);
                DeviceInfo updated = enrichDevice(serial, cached.getModel());
                deviceCache.put(serial, updated);
                refreshed.add(updated);
            }
            sendDeviceList(refreshed);
        } catch (Exception e) {
            log.debug("Refresh failed: {}", e.getMessage());
        }
    }

    /**
     * Sends the current cached device list. Used on reconnection.
     */
    private void sendCurrentDeviceList() {
        if (!serverConnection.isConnected()) return;
        List<DeviceInfo> devices = new ArrayList<>(deviceCache.values());
        if (devices.isEmpty()) {
            // No cache yet — do a fast scan + enrich
            List<DeviceInfo> basic = fastScan();
            if (!basic.isEmpty()) {
                sendDeviceList(basic);
                enrichmentExecutor.submit(() -> enrichAndSend(basic));
            }
        } else {
            sendDeviceList(devices);
        }
    }

    private void sendDeviceList(List<DeviceInfo> devices) {
        if (!serverConnection.isConnected()) return;
        try {
            serverConnection.send(new AgentMessage.DeviceList(devices));
        } catch (Exception e) {
            log.warn("Failed to send device list: {}", e.getMessage());
        }
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
            process.waitFor(5, TimeUnit.SECONDS);
            return output;
        } catch (Exception e) {
            return "";
        }
    }

    private String extractIp(String ifconfigOutput) {
        if (ifconfigOutput == null || ifconfigOutput.isEmpty()) return "";
        for (String part : ifconfigOutput.trim().split("\\s+")) {
            if (part.contains(".") && part.contains("/")) {
                return part.split("/")[0];
            }
        }
        return "";
    }
}
