package androidtoolkit.backend.crash;

import androidtoolkit.backend.service.ObjectStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Deque;
import java.util.List;

/**
 * Captures crash log context (rolling buffer + post-crash lines), formats with metadata header,
 * and uploads to Object Storage with retry logic. Falls back to local storage on failure.
 */
@Service
public class CrashLogCaptureService {

    private static final Logger log = LoggerFactory.getLogger(CrashLogCaptureService.class);

    private static final int MAX_RETRIES = 3;
    private static final int MAX_POST_CRASH_LINES = 50;
    private static final String FALLBACK_DIR = "data/crash-logs-fallback";
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    private final ObjectStorageService objectStorageService;

    public CrashLogCaptureService(ObjectStorageService objectStorageService) {
        this.objectStorageService = objectStorageService;
    }

    /**
     * Captures the crash log from the line buffer and post-crash lines, formats it,
     * and uploads to Object Storage with retry. Falls back to local storage on failure.
     *
     * @param event          the crash event
     * @param lineBuffer     the rolling buffer contents at time of crash
     * @param postCrashLines lines collected after the crash (up to 50 used)
     * @return the storage path where the log was saved, or null if all storage failed
     */
    public String captureCrashLog(CrashEvent event, Deque<String> lineBuffer, List<String> postCrashLines) {
        List<String> bufferLines = List.copyOf(lineBuffer);
        List<String> truncatedPostCrash = postCrashLines != null && postCrashLines.size() > MAX_POST_CRASH_LINES
                ? postCrashLines.subList(0, MAX_POST_CRASH_LINES)
                : postCrashLines;

        String content = formatCrashLog(event, bufferLines, truncatedPostCrash);
        String storagePath = buildStoragePath(event.tenantId(), event.projectId(),
                event.deviceSerial(), event.timestamp());

        // Attempt upload with retries
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                String filename = extractFilename(storagePath);
                String prefix = extractPrefix(storagePath);

                objectStorageService.upload(
                        event.tenantId(),
                        prefix,
                        filename,
                        new ByteArrayInputStream(bytes),
                        bytes.length
                );

                log.info("Crash log uploaded to {} on attempt {}", storagePath, attempt);
                return storagePath;
            } catch (Exception e) {
                log.warn("Crash log upload attempt {}/{} failed for {}: {}",
                        attempt, MAX_RETRIES, storagePath, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    sleepWithBackoff(attempt);
                }
            }
        }

        // All retries exhausted — fall back to local storage
        return storeLocally(event, content, storagePath);
    }

    /**
     * Formats the crash log file content with a metadata header followed by buffer and post-crash lines.
     */
    public String formatCrashLog(CrashEvent event, List<String> bufferLines, List<String> postCrashLines) {
        StringBuilder sb = new StringBuilder();

        // Metadata header
        sb.append("=== CRASH LOG ===\n");
        sb.append("Timestamp: ").append(event.timestamp()).append("\n");
        sb.append("Device Serial: ").append(event.deviceSerial()).append("\n");
        sb.append("Package Name: ").append(event.packageName()).append("\n");
        sb.append("Crash Type: ").append(event.crashType()).append("\n");
        sb.append("Severity: ").append(event.severity()).append("\n");
        sb.append("=================\n\n");

        // Buffer lines (pre-crash context)
        sb.append("--- Pre-crash context (").append(bufferLines.size()).append(" lines) ---\n");
        for (String line : bufferLines) {
            sb.append(line).append("\n");
        }

        // Post-crash lines
        if (postCrashLines != null && !postCrashLines.isEmpty()) {
            sb.append("\n--- Post-crash lines (").append(postCrashLines.size()).append(" lines) ---\n");
            for (String line : postCrashLines) {
                sb.append(line).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Constructs the Object Storage path for a crash log.
     * Format: crash-logs/{tenantId}/{projectId}/{deviceSerial}/{timestamp}.log
     */
    public String buildStoragePath(Long tenantId, Long projectId, String deviceSerial, Instant timestamp) {
        String formattedTimestamp = TIMESTAMP_FORMAT.format(timestamp);
        return String.format("crash-logs/%d/%d/%s/%s.log",
                tenantId, projectId, deviceSerial, formattedTimestamp);
    }

    private String storeLocally(CrashEvent event, String content, String originalPath) {
        try {
            Path fallbackDir = Path.of(FALLBACK_DIR);
            Files.createDirectories(fallbackDir);

            String filename = event.deviceSerial() + "_" + TIMESTAMP_FORMAT.format(event.timestamp()) + ".log";
            Path localPath = fallbackDir.resolve(filename);

            Files.writeString(localPath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.warn("Crash log stored locally at {} after upload failures", localPath);
            return localPath.toString();
        } catch (IOException e) {
            log.error("Failed to store crash log locally: {}", e.getMessage());
            return null;
        }
    }

    private void sleepWithBackoff(int attempt) {
        try {
            long delayMs = (long) Math.pow(2, attempt - 1) * 1000; // 1s, 2s, 4s
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String extractFilename(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    private String extractPrefix(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(0, lastSlash) : "";
    }
}
