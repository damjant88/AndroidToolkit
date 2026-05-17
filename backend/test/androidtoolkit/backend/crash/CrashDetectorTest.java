package androidtoolkit.backend.crash;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CrashDetector}.
 */
class CrashDetectorTest {

    private CrashPatternRepository patternRepository;
    private CrashDetector crashDetector;

    @BeforeEach
    void setUp() {
        patternRepository = mock(CrashPatternRepository.class);
        crashDetector = new CrashDetector(patternRepository);
    }

    private CrashPatternEntity createPattern(String regex, SeverityLevel severity) {
        CrashPatternEntity pattern = new CrashPatternEntity();
        pattern.setRegex(regex);
        pattern.setSeverity(severity);
        pattern.setEnabled(true);
        return pattern;
    }

    // --- classifySeverity tests ---

    @Test
    void classifySeverity_fatalException_returnsFatal() {
        assertEquals(SeverityLevel.FATAL, crashDetector.classifySeverity("FATAL EXCEPTION"));
    }

    @Test
    void classifySeverity_sigabrt_returnsFatal() {
        assertEquals(SeverityLevel.FATAL, crashDetector.classifySeverity("SIGABRT"));
    }

    @Test
    void classifySeverity_sigsegv_returnsFatal() {
        assertEquals(SeverityLevel.FATAL, crashDetector.classifySeverity("SIGSEGV"));
    }

    @Test
    void classifySeverity_anrIn_returnsAnr() {
        assertEquals(SeverityLevel.ANR, crashDetector.classifySeverity("ANR in"));
    }

    @Test
    void classifySeverity_processCrashed_returnsWarning() {
        assertEquals(SeverityLevel.WARNING, crashDetector.classifySeverity("Process crashed"));
    }

    @Test
    void classifySeverity_runtimeException_returnsWarning() {
        assertEquals(SeverityLevel.WARNING, crashDetector.classifySeverity("java.lang.RuntimeException"));
    }

    @Test
    void classifySeverity_null_returnsWarning() {
        assertEquals(SeverityLevel.WARNING, crashDetector.classifySeverity(null));
    }

    // --- extractPackageName tests ---

    @Test
    void extractPackageName_fromLine() {
        String line = "E/AndroidRuntime: FATAL EXCEPTION in com.example.myapp";
        assertEquals("com.example.myapp", crashDetector.extractPackageName(line, List.of()));
    }

    @Test
    void extractPackageName_fromContext() {
        String line = "E/AndroidRuntime: FATAL EXCEPTION";
        List<String> context = List.of(
                "    at java.lang.Thread.run(Thread.java:764)",
                "    Process: com.mycompany.testapp, PID: 12345"
        );
        assertEquals("com.mycompany.testapp", crashDetector.extractPackageName(line, context));
    }

    @Test
    void extractPackageName_noPackageFound_returnsUnknown() {
        String line = "FATAL EXCEPTION: main";
        assertEquals("unknown", crashDetector.extractPackageName(line, List.of()));
    }

    @Test
    void extractPackageName_nullContext_returnsUnknown() {
        String line = "FATAL EXCEPTION: main";
        assertEquals("unknown", crashDetector.extractPackageName(line, null));
    }

    // --- analyze tests ---

    @Test
    void analyze_nullLine_returnsEmpty() {
        Optional<CrashEvent> result = crashDetector.analyze("device1", null, List.of(), 1L, 1L);
        assertTrue(result.isEmpty());
    }

    @Test
    void analyze_emptyLine_returnsEmpty() {
        Optional<CrashEvent> result = crashDetector.analyze("device1", "", List.of(), 1L, 1L);
        assertTrue(result.isEmpty());
    }

    @Test
    void analyze_noMatchingPattern_returnsEmpty() {
        when(patternRepository.findByProjectIdAndEnabledTrue(1L))
                .thenReturn(List.of(createPattern("FATAL EXCEPTION", SeverityLevel.FATAL)));

        Optional<CrashEvent> result = crashDetector.analyze(
                "device1", "I/MyApp: Normal log line", List.of(), 1L, 1L);
        assertTrue(result.isEmpty());
    }

    @Test
    void analyze_matchingPattern_returnsCrashEvent() {
        when(patternRepository.findByProjectIdAndEnabledTrue(1L))
                .thenReturn(List.of(createPattern("FATAL EXCEPTION", SeverityLevel.FATAL)));

        Optional<CrashEvent> result = crashDetector.analyze(
                "device1",
                "E/AndroidRuntime: FATAL EXCEPTION: main in com.example.app",
                List.of("    at com.example.app.MainActivity.onCreate(MainActivity.java:42)"),
                1L, 1L);

        assertTrue(result.isPresent());
        CrashEvent event = result.get();
        assertEquals("device1", event.deviceSerial());
        assertEquals(SeverityLevel.FATAL, event.severity());
        assertEquals("com.example.app", event.packageName());
        assertEquals("FATAL EXCEPTION", event.crashType());
        assertNotNull(event.id());
        assertNotNull(event.timestamp());
        assertEquals(1L, event.projectId());
        assertEquals(1L, event.tenantId());
    }

    @Test
    void analyze_matchingPattern_extractsStackTrace() {
        when(patternRepository.findByProjectIdAndEnabledTrue(1L))
                .thenReturn(List.of(createPattern("FATAL EXCEPTION", SeverityLevel.FATAL)));

        List<String> context = List.of(
                "    at com.example.app.MainActivity.onCreate(MainActivity.java:42)",
                "    at android.app.Activity.performCreate(Activity.java:7136)",
                "    at android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1271)"
        );

        Optional<CrashEvent> result = crashDetector.analyze(
                "device1",
                "E/AndroidRuntime: FATAL EXCEPTION: main in com.example.app",
                context, 1L, 1L);

        assertTrue(result.isPresent());
        String stackTrace = result.get().stackTraceSnippet();
        assertEquals(String.join("\n", context), stackTrace);
    }

    @Test
    void analyze_stackTraceLimitedTo20Lines() {
        when(patternRepository.findByProjectIdAndEnabledTrue(1L))
                .thenReturn(List.of(createPattern("FATAL EXCEPTION", SeverityLevel.FATAL)));

        // Create 30 context lines
        List<String> context = java.util.stream.IntStream.range(0, 30)
                .mapToObj(i -> "    at com.example.Class" + i + ".method(Class" + i + ".java:" + i + ")")
                .toList();

        Optional<CrashEvent> result = crashDetector.analyze(
                "device1",
                "E/AndroidRuntime: FATAL EXCEPTION: main in com.example.app",
                context, 1L, 1L);

        assertTrue(result.isPresent());
        String stackTrace = result.get().stackTraceSnippet();
        long lineCount = stackTrace.lines().count();
        assertEquals(20, lineCount);
    }

    // --- isDuplicate / deduplication tests ---

    @Test
    void analyze_duplicateWithin5Seconds_returnsEmpty() {
        when(patternRepository.findByProjectIdAndEnabledTrue(1L))
                .thenReturn(List.of(createPattern("FATAL EXCEPTION", SeverityLevel.FATAL)));

        // First crash should succeed
        Optional<CrashEvent> first = crashDetector.analyze(
                "device1",
                "E/AndroidRuntime: FATAL EXCEPTION: main in com.example.app",
                List.of(), 1L, 1L);
        assertTrue(first.isPresent());

        // Second crash within 5 seconds should be suppressed
        Optional<CrashEvent> second = crashDetector.analyze(
                "device1",
                "E/AndroidRuntime: FATAL EXCEPTION: main in com.example.app",
                List.of(), 1L, 1L);
        assertTrue(second.isEmpty());
    }

    @Test
    void analyze_differentDevices_notDeduplicated() {
        when(patternRepository.findByProjectIdAndEnabledTrue(1L))
                .thenReturn(List.of(createPattern("FATAL EXCEPTION", SeverityLevel.FATAL)));

        Optional<CrashEvent> first = crashDetector.analyze(
                "device1",
                "E/AndroidRuntime: FATAL EXCEPTION: main in com.example.app",
                List.of(), 1L, 1L);
        assertTrue(first.isPresent());

        Optional<CrashEvent> second = crashDetector.analyze(
                "device2",
                "E/AndroidRuntime: FATAL EXCEPTION: main in com.example.app",
                List.of(), 1L, 1L);
        assertTrue(second.isPresent());
    }

    @Test
    void isDuplicate_noPreviousCrash_returnsFalse() {
        CrashEvent event = new CrashEvent(
                "id1", Instant.now(), "device1", null, "com.example.app",
                "FATAL EXCEPTION", SeverityLevel.FATAL, "", null, 1L, 1L);
        assertFalse(crashDetector.isDuplicate("device1", event));
    }

    @Test
    void isDuplicate_afterClearingState_returnsFalse() {
        when(patternRepository.findByProjectIdAndEnabledTrue(1L))
                .thenReturn(List.of(createPattern("FATAL EXCEPTION", SeverityLevel.FATAL)));

        // Trigger a crash
        crashDetector.analyze("device1",
                "E/AndroidRuntime: FATAL EXCEPTION: main in com.example.app",
                List.of(), 1L, 1L);

        // Clear state
        crashDetector.clearDeduplicationState();

        // Should not be duplicate anymore
        CrashEvent event = new CrashEvent(
                "id2", Instant.now(), "device1", null, "com.example.app",
                "FATAL EXCEPTION", SeverityLevel.FATAL, "", null, 1L, 1L);
        assertFalse(crashDetector.isDuplicate("device1", event));
    }

    // --- ANR pattern test ---

    @Test
    void analyze_anrPattern_returnsAnrSeverity() {
        when(patternRepository.findByProjectIdAndEnabledTrue(1L))
                .thenReturn(List.of(createPattern("ANR in", SeverityLevel.ANR)));

        Optional<CrashEvent> result = crashDetector.analyze(
                "device1",
                "E/ActivityManager: ANR in com.example.app (com.example.app/.MainActivity)",
                List.of(), 1L, 1L);

        assertTrue(result.isPresent());
        assertEquals(SeverityLevel.ANR, result.get().severity());
    }

    // --- SIGSEGV pattern test ---

    @Test
    void analyze_sigsegvPattern_returnsFatalSeverity() {
        when(patternRepository.findByProjectIdAndEnabledTrue(1L))
                .thenReturn(List.of(createPattern("SIGSEGV", SeverityLevel.FATAL)));

        Optional<CrashEvent> result = crashDetector.analyze(
                "device1",
                "F/libc: Fatal signal 11 (SIGSEGV), code 1, fault addr 0x0 in tid 12345",
                List.of(), 1L, 1L);

        assertTrue(result.isPresent());
        assertEquals(SeverityLevel.FATAL, result.get().severity());
    }
}
