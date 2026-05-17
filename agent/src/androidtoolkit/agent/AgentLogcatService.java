package androidtoolkit.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs logcat locally on the agent, parses lines in real-time to extract
 * metadata (environment, client version, server version, access token).
 * The frontend polls this data via the agent's REST API.
 */
@Service
public class AgentLogcatService {

    private static final Logger log = LoggerFactory.getLogger(AgentLogcatService.class);

    // Regex patterns (same as backend's LogcatParser)
    private static final Pattern ENVIRONMENT_PATTERN =
            Pattern.compile("-->\\s+\\w+\\s+https?://([^/]+)/");
    private static final Pattern CLIENT_VERSION_PATTERN =
            Pattern.compile("User-Agent:\\s+(?:[\\w%]+\\+)+\\w+\\s+(\\S+)");
    private static final Pattern SERVER_PRODUCT_VERSION_PATTERN =
            Pattern.compile("x-safepath-product-version:\\s+(\\S+)");
    private static final Pattern SERVER_PROJECT_VERSION_PATTERN =
            Pattern.compile("x-safepath-project-version:\\s+(\\S+)");
    private static final Pattern ACCESS_TOKEN_RESPONSE_PATTERN =
            Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ACCESS_TOKEN_REQUEST_PATTERN =
            Pattern.compile("Authorization:\\s+Bearer\\s+(\\S+)");

    private final ConcurrentHashMap<String, LogcatMetadata> deviceMetadata = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Process> activeProcesses = new ConcurrentHashMap<>();

    /**
     * Starts logcat monitoring for a device. Parses lines in real-time.
     * Runs without PID filter to catch all app log lines (PID can change on restart).
     */
    public void startMonitoring(String serial, String pid) {
        if (activeProcesses.containsKey(serial)) return;

        Thread thread = Thread.ofVirtual().name("logcat-monitor-" + serial).start(() -> {
            try {
                // Don't filter by PID — the app PID can change and OkHttp logs
                // may come from different threads/processes
                ProcessBuilder pb = new ProcessBuilder("adb", "-s", serial, "logcat");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                activeProcesses.put(serial, process);

                LogcatMetadata metadata = deviceMetadata.computeIfAbsent(serial, LogcatMetadata::new);

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        parseLine(line, metadata);
                    }
                }
            } catch (Exception e) {
                if (!Thread.currentThread().isInterrupted()) {
                    log.debug("Logcat monitor ended for {}: {}", serial, e.getMessage());
                }
            } finally {
                activeProcesses.remove(serial);
            }
        });
    }

    /**
     * Stops logcat monitoring for a device.
     */
    public void stopMonitoring(String serial) {
        Process process = activeProcesses.remove(serial);
        if (process != null) {
            process.destroyForcibly();
        }
    }

    /**
     * Returns the current parsed metadata for a device.
     */
    public LogcatMetadata getMetadata(String serial) {
        return deviceMetadata.getOrDefault(serial, new LogcatMetadata(serial));
    }

    /**
     * Starts monitoring for all known devices (called when devices are discovered).
     */
    public void onDevicesChanged(Map<String, String> serialToPid) {
        // Start monitoring for new devices
        for (Map.Entry<String, String> entry : serialToPid.entrySet()) {
            if (!activeProcesses.containsKey(entry.getKey())) {
                startMonitoring(entry.getKey(), entry.getValue());
            }
        }
        // Stop monitoring for removed devices
        for (String serial : activeProcesses.keySet()) {
            if (!serialToPid.containsKey(serial)) {
                stopMonitoring(serial);
            }
        }
    }

    private void parseLine(String line, LogcatMetadata metadata) {
        if (line == null || line.isEmpty()) return;

        Matcher matcher;

        matcher = ENVIRONMENT_PATTERN.matcher(line);
        if (matcher.find()) {
            String host = matcher.group(1);
            if (!host.startsWith("download.") && !host.startsWith("api.") &&
                    !host.startsWith("vc01.") && !host.startsWith("urldb.") &&
                    !host.contains("assets") && !host.contains("aws")) {
                metadata.environment = host;
            }
        }

        matcher = CLIENT_VERSION_PATTERN.matcher(line);
        if (matcher.find()) {
            metadata.clientVersion = matcher.group(1);
        }

        matcher = SERVER_PRODUCT_VERSION_PATTERN.matcher(line);
        if (matcher.find()) {
            metadata.serverProductVersion = matcher.group(1);
        }

        matcher = SERVER_PROJECT_VERSION_PATTERN.matcher(line);
        if (matcher.find()) {
            metadata.serverProjectVersion = matcher.group(1);
        }

        matcher = ACCESS_TOKEN_RESPONSE_PATTERN.matcher(line);
        if (matcher.find()) {
            metadata.accessToken = matcher.group(1);
            metadata.tokenType = extractJwtType(matcher.group(1));
        }

        matcher = ACCESS_TOKEN_REQUEST_PATTERN.matcher(line);
        if (matcher.find()) {
            metadata.accessToken = matcher.group(1);
            metadata.tokenType = extractJwtType(matcher.group(1));
        }
    }

    private String extractJwtType(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                if (payload.contains("\"type\"")) {
                    int idx = payload.indexOf("\"type\"");
                    int start = payload.indexOf("\"", idx + 6) + 1;
                    int end = payload.indexOf("\"", start);
                    if (start > 0 && end > start) {
                        return payload.substring(start, end);
                    }
                }
                // Fallback: check for "role" or "sub" to determine type
                if (payload.contains("godevice") || payload.contains("child")) return "godevice";
                if (payload.contains("admin") || payload.contains("parent")) return "admin";
            }
        } catch (Exception e) { /* ignore */ }
        return "";
    }

    /**
     * Simple metadata holder for parsed logcat data.
     */
    public static class LogcatMetadata {
        public String serial;
        public String environment;
        public String clientVersion;
        public String serverProductVersion;
        public String serverProjectVersion;
        public String accessToken;
        public String tokenType;

        public LogcatMetadata(String serial) {
            this.serial = serial;
        }
    }
}
