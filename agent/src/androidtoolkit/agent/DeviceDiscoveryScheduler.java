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
    private final AgentLogcatService logcatService;

    private volatile Process trackProcess;
    private volatile boolean running = true;
    private volatile Set<String> lastKnownSerials = Set.of();

    public DeviceDiscoveryScheduler(ServerConnection serverConnection, AgentLogcatService logcatService) {
        this.serverConnection = serverConnection;
        this.logcatService = logcatService;
        serverConnection.setOnReconnected(this::sendCurrentDeviceList);
    }

    @PostConstruct
    public void start() {
        // Start the device tracking thread
        Thread tracker = new Thread(this::trackDevices, "adb-track-devices");
        tracker.setDaemon(true);
        tracker.start();
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
     * Called when adb detects a device change. Runs fast enrichment (single shell call)
     * and sends the complete device list immediately.
     * 
     * For device removals, applies a 3-second debounce to avoid flickering during
     * WiFi debug toggle (adb tcpip causes brief USB disconnect/reconnect).
     */
    private void onDeviceChangeDetected() {
        try {
            // Fast scan — just serial + model (instant, no shell commands)
            List<DeviceInfo> basicDevices = fastScan();
            Set<String> currentSerials = new HashSet<>();
            for (DeviceInfo d : basicDevices) {
                currentSerials.add(d.getSerialNumber());
            }

            // Only process if the serial set actually changed
            if (!currentSerials.equals(lastKnownSerials)) {
                
                // Check if devices were REMOVED (not added)
                Set<String> removedSerials = new HashSet<>(lastKnownSerials);
                removedSerials.removeAll(currentSerials);
                
                if (!removedSerials.isEmpty() && !currentSerials.isEmpty()) {
                    // Devices were removed but some remain — debounce to handle WiFi debug flicker
                    // Wait 3 seconds and re-scan to see if the device comes back
                    try { Thread.sleep(3000); } catch (InterruptedException e) { return; }
                    basicDevices = fastScan();
                    currentSerials = new HashSet<>();
                    for (DeviceInfo d : basicDevices) {
                        currentSerials.add(d.getSerialNumber());
                    }
                    // If nothing actually changed after debounce, skip
                    if (currentSerials.equals(lastKnownSerials)) return;
                }

                lastKnownSerials = currentSerials;

                // Remove disconnected devices from cache
                final Set<String> finalSerials = currentSerials;
                deviceCache.keySet().removeIf(s -> !finalSerials.contains(s));

                if (currentSerials.isEmpty()) {
                    // All devices disconnected — send empty list
                    sendDeviceList(List.of());
                } else {
                    // Enrich all devices with single shell call each (~1s total)
                    // and send complete info in one shot
                    List<DeviceInfo> enrichedDevices = new ArrayList<>();
                    for (DeviceInfo basic : basicDevices) {
                        String serial = basic.getSerialNumber();
                        DeviceInfo enriched = enrichDeviceFast(serial, basic.getModel());
                        deviceCache.put(serial, enriched);
                        enrichedDevices.add(enriched);
                    }
                    sendDeviceList(enrichedDevices);
                    
                    // Start logcat monitoring for devices with installed apps
                    Map<String, String> serialToPid = new HashMap<>();
                    for (DeviceInfo d : enrichedDevices) {
                        if (d.isAppInstalled() && !d.getPid().isEmpty()) {
                            serialToPid.put(d.getSerialNumber(), d.getPid());
                        }
                    }
                    if (!serialToPid.isEmpty()) {
                        logcatService.onDevicesChanged(serialToPid);
                    }
                }
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
     * Enriches device info using a SINGLE adb shell call that gathers all data at once.
     * This is ~5x faster than running 6 separate adb shell commands sequentially.
     */
    private void enrichAndSend(List<DeviceInfo> basicDevices) {
        try {
            List<DeviceInfo> enrichedDevices = new ArrayList<>();
            for (DeviceInfo basic : basicDevices) {
                String serial = basic.getSerialNumber();
                DeviceInfo enriched = enrichDeviceFast(serial, basic.getModel());
                deviceCache.put(serial, enriched);
                enrichedDevices.add(enriched);
            }
            sendDeviceList(enrichedDevices);
        } catch (Exception e) {
            log.warn("Enrichment failed: {}", e.getMessage());
        }
    }

    /**
     * Gathers ALL device info in a single adb shell call using a combined command.
     * Output format: manufacturer|osVersion|wifiIp|mobileIp|packages (one per line after)
     */
    private DeviceInfo enrichDeviceFast(String serial, String model) {
        // Single shell call that gets everything at once
        String combined = runAdbShell(serial,
                "echo \"MANUFACTURER=$(getprop ro.product.manufacturer)\";" +
                "echo \"OS=$(getprop ro.build.version.release)\";" +
                "echo \"MODEL=$(getprop ro.product.model)\";" +
                "echo \"WIFI=$(ip addr show wlan0 2>/dev/null | grep 'inet ' | awk '{print $2}' | cut -d/ -f1)\";" +
                "echo \"MOBILE=$(ip addr show rmnet_data0 2>/dev/null | grep 'inet ' | awk '{print $2}' | cut -d/ -f1)\";" +
                "pm list packages | grep -E 'safepath|familymode|securefamily|safeandfound|wavemarket|waplauncher|safefound|familycontrols|orangespain|TuYo|cci\\.test'");

        String manufacturer = "";
        String osVersion = "";
        String wifiIp = "";
        String mobileIp = "";
        String safePathPackage = "";

        for (String line : combined.split("\n")) {
            line = line.trim();
            if (line.startsWith("MANUFACTURER=")) manufacturer = line.substring(13);
            else if (line.startsWith("OS=")) osVersion = line.substring(3);
            else if (line.startsWith("MODEL=") && model.isEmpty()) model = line.substring(6).replace('_', ' ');
            else if (line.startsWith("WIFI=")) wifiIp = line.substring(5);
            else if (line.startsWith("MOBILE=")) mobileIp = line.substring(7);
            else if (line.startsWith("package:")) {
                safePathPackage = line.substring(8).trim();
            }
        }

        String ipAddress = wifiIp.isEmpty() ? mobileIp : wifiIp;
        boolean appInstalled = !safePathPackage.isEmpty();
        String pid = "";
        if (appInstalled) {
            pid = runAdbShell(serial, "pidof -s " + safePathPackage).trim();
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
            Map<String, String> serialToPid = new HashMap<>();
            for (String serial : deviceCache.keySet()) {
                DeviceInfo cached = deviceCache.get(serial);
                DeviceInfo updated = enrichDeviceFast(serial, cached.getModel());
                deviceCache.put(serial, updated);
                refreshed.add(updated);
                if (updated.isAppInstalled()) {
                    serialToPid.put(serial, updated.getPid());
                }
            }
            sendDeviceList(refreshed);
            // Ensure logcat monitoring is running for all devices with apps
            if (!serialToPid.isEmpty()) {
                logcatService.onDevicesChanged(serialToPid);
            }
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
        // Match against the same package list as PackageClassifier in the core module
        List<String> supportedPackages = List.of(
                "com.smithmicro.tmobile.familymode.test",
                "com.smithmicro.att.securefamily",
                "com.att.securefamilycompanion",
                "com.wavemarket.waplauncher",
                "com.smithmicro.safepath.family",
                "com.smithmicro.safepath.family.light",
                "com.smithmicro.safepath.family.speakeasy",
                "com.smithmicro.cci.test",
                "com.smithmicro.sprint.safeandfound.test",
                "com.sprint.safefound",
                "com.tmobile.familycontrols",
                "com.smithmicro.orangespain.test",
                "com.orange.es.TuYo",
                "com.smithmicro.safepath.dish.test",
                "com.smithmicro.safepath.dish.kid.test",
                "com.smithmicro.safepath.family.child"
        );
        List<String> packageHints = List.of(
                "safepath.family", "securefamily", "wavemarket",
                "safeandfound", "safefound", "familycontrols",
                "orangespain", "TuYo", "safepath.dish", "familymode"
        );

        List<String> installed = new ArrayList<>();
        for (String line : packages.split("\n")) {
            String pkg = line.replace("package:", "").trim();
            if (!pkg.isEmpty()) installed.add(pkg);
        }

        // First: exact match against supported packages
        for (String pkg : installed) {
            if (supportedPackages.contains(pkg)) return pkg;
        }
        // Second: hint-based match
        for (String pkg : installed) {
            for (String hint : packageHints) {
                if (pkg.contains(hint)) return pkg;
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
