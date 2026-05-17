package androidtoolkit.backend.crash;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.StringLength;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for {@link CrashLogCaptureService}.
 * Covers Properties 7 and 8 from the design document.
 */
class CrashLogCapturePropertyTest {

    private CrashLogCaptureService createService() {
        ObjectStorageServiceStub storageService = new ObjectStorageServiceStub();
        return new CrashLogCaptureService(storageService);
    }

    // --- Property 7: Crash log capture completeness ---

    @Tag("Feature: crash-detection-alerts, Property 7: Crash log capture completeness")
    @Property(tries = 100)
    void formattedLogContainsMetadataHeader(
            @ForAll("crashEvents") CrashEvent event,
            @ForAll @IntRange(min = 1, max = 100) int bufferLineCount,
            @ForAll @IntRange(min = 0, max = 60) int postCrashLineCount) {

        CrashLogCaptureService service = createService();

        List<String> bufferLines = IntStream.range(0, bufferLineCount)
                .mapToObj(i -> "buffer-line-" + i)
                .toList();
        List<String> postCrashLines = IntStream.range(0, postCrashLineCount)
                .mapToObj(i -> "post-crash-" + i)
                .toList();

        String formatted = service.formatCrashLog(event, bufferLines, postCrashLines);

        // Verify metadata header is present
        assertTrue(formatted.contains("=== CRASH LOG ==="), "Should contain header marker");
        assertTrue(formatted.contains("Timestamp: " + event.timestamp()), "Should contain timestamp");
        assertTrue(formatted.contains("Device Serial: " + event.deviceSerial()), "Should contain device serial");
        assertTrue(formatted.contains("Package Name: " + event.packageName()), "Should contain package name");
        assertTrue(formatted.contains("Crash Type: " + event.crashType()), "Should contain crash type");
        assertTrue(formatted.contains("Severity: " + event.severity()), "Should contain severity");
    }

    @Tag("Feature: crash-detection-alerts, Property 7: Crash log capture completeness")
    @Property(tries = 100)
    void formattedLogContainsAllBufferLines(
            @ForAll("crashEvents") CrashEvent event,
            @ForAll @IntRange(min = 1, max = 50) int bufferLineCount) {

        CrashLogCaptureService service = createService();

        List<String> bufferLines = IntStream.range(0, bufferLineCount)
                .mapToObj(i -> "unique-buffer-content-" + i)
                .toList();

        String formatted = service.formatCrashLog(event, bufferLines, List.of());

        // Every buffer line should appear in the output
        for (String line : bufferLines) {
            assertTrue(formatted.contains(line),
                    "Formatted log should contain buffer line: " + line);
        }
    }

    @Tag("Feature: crash-detection-alerts, Property 7: Crash log capture completeness")
    @Property(tries = 100)
    void formattedLogContainsPostCrashLines(
            @ForAll("crashEvents") CrashEvent event,
            @ForAll @IntRange(min = 1, max = 50) int postCrashLineCount) {

        CrashLogCaptureService service = createService();

        List<String> postCrashLines = IntStream.range(0, postCrashLineCount)
                .mapToObj(i -> "unique-post-crash-" + i)
                .toList();

        String formatted = service.formatCrashLog(event, List.of("buffer"), postCrashLines);

        for (String line : postCrashLines) {
            assertTrue(formatted.contains(line),
                    "Formatted log should contain post-crash line: " + line);
        }
    }

    // --- Property 8: Crash log storage path format ---

    @Tag("Feature: crash-detection-alerts, Property 8: Crash log storage path format")
    @Property(tries = 200)
    void storagePathMatchesExpectedFormat(
            @ForAll @LongRange(min = 1, max = 10000) long tenantId,
            @ForAll @LongRange(min = 1, max = 10000) long projectId,
            @ForAll @StringLength(min = 5, max = 20) String deviceSerial) {

        CrashLogCaptureService service = createService();
        Instant timestamp = Instant.parse("2025-03-15T10:30:00Z");

        String path = service.buildStoragePath(tenantId, projectId, deviceSerial, timestamp);

        // Verify format: crash-logs/{tenantId}/{projectId}/{deviceSerial}/{timestamp}.log
        assertTrue(path.startsWith("crash-logs/"), "Path should start with crash-logs/");
        assertTrue(path.contains("/" + tenantId + "/"), "Path should contain tenantId");
        assertTrue(path.contains("/" + projectId + "/"), "Path should contain projectId");
        assertTrue(path.contains("/" + deviceSerial + "/"), "Path should contain deviceSerial");
        assertTrue(path.endsWith(".log"), "Path should end with .log");

        // Verify the structure has exactly 4 slashes (5 segments)
        long slashCount = path.chars().filter(c -> c == '/').count();
        assertEquals(4, slashCount, "Path should have format crash-logs/t/p/d/ts.log");
    }

    @Tag("Feature: crash-detection-alerts, Property 8: Crash log storage path format")
    @Property(tries = 100)
    void storagePathContainsTimestampComponent(
            @ForAll @LongRange(min = 1, max = 100) long tenantId,
            @ForAll @LongRange(min = 1, max = 100) long projectId) {

        CrashLogCaptureService service = createService();
        Instant timestamp = Instant.parse("2025-06-20T14:45:30Z");

        String path = service.buildStoragePath(tenantId, projectId, "device123", timestamp);

        // The timestamp should be formatted as yyyyMMdd'T'HHmmss'Z'
        assertTrue(path.contains("20250620T144530Z"),
                "Path should contain formatted timestamp: " + path);
    }

    @Provide
    Arbitrary<CrashEvent> crashEvents() {
        return Arbitraries.of(
                new CrashEvent("id1", Instant.now(), "device1", "Device One", "com.example.app",
                        "FATAL EXCEPTION", SeverityLevel.FATAL, "stack trace", null, 1L, 1L),
                new CrashEvent("id2", Instant.now(), "device2", null, "com.test.app",
                        "ANR in", SeverityLevel.ANR, "", null, 2L, 1L),
                new CrashEvent("id3", Instant.now(), "device3", "Device Three", "unknown",
                        "SIGSEGV", SeverityLevel.FATAL, "signal 11", null, 1L, 2L)
        );
    }

    /**
     * Stub implementation of ObjectStorageService for testing.
     */
    private static class ObjectStorageServiceStub implements androidtoolkit.backend.service.ObjectStorageService {
        @Override
        public String upload(Long tenantId, String projectPrefix, String filename,
                             java.io.InputStream data, long size) {
            return projectPrefix + "/" + filename;
        }

        @Override
        public java.io.InputStream download(String objectKey) {
            return new java.io.ByteArrayInputStream(new byte[0]);
        }

        @Override
        public void archive(String objectKey) {}

        @Override
        public long getTenantStorageUsage(Long tenantId) { return 0; }
    }
}
