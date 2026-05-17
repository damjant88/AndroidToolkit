package androidtoolkit.backend.crash;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for {@link CrashDetector}.
 * Covers Properties 1-5 from the design document.
 */
class CrashDetectorPropertyTest {

    private CrashPatternRepository mockRepository() {
        return mock(CrashPatternRepository.class);
    }

    private CrashPatternEntity pattern(String regex, SeverityLevel severity) {
        CrashPatternEntity p = new CrashPatternEntity();
        p.setRegex(regex);
        p.setSeverity(severity);
        p.setEnabled(true);
        return p;
    }

    // --- Property 1: Crash detection with correct severity classification ---

    @Tag("Feature: crash-detection-alerts, Property 1: Crash detection with correct severity classification")
    @Property(tries = 100)
    void fatalPatternsClassifiedAsFatal(
            @ForAll("fatalPatterns") String patternStr,
            @ForAll @StringLength(min = 5, max = 50) String prefix) {

        CrashPatternRepository repo = mockRepository();
        when(repo.findByProjectIdAndEnabledTrue(1L))
                .thenReturn(List.of(pattern(patternStr, SeverityLevel.FATAL)));

        CrashDetector detector = new CrashDetector(repo);
        String line = prefix + " " + patternStr + " in com.example.app";

        Optional<CrashEvent> result = detector.analyze("device1", line, List.of(), 1L, 1L);

        assertTrue(result.isPresent(), "Should detect crash for pattern: " + patternStr);
        assertEquals(SeverityLevel.FATAL, result.get().severity(),
                "Pattern '" + patternStr + "' should be classified as FATAL");
    }

    @Provide
    Arbitrary<String> fatalPatterns() {
        return Arbitraries.of("FATAL EXCEPTION", "SIGABRT", "SIGSEGV");
    }

    @Tag("Feature: crash-detection-alerts, Property 1: Crash detection with correct severity classification")
    @Property(tries = 100)
    void anrPatternClassifiedAsAnr(@ForAll @StringLength(min = 5, max = 50) String prefix) {
        CrashPatternRepository repo = mockRepository();
        when(repo.findByProjectIdAndEnabledTrue(1L))
                .thenReturn(List.of(pattern("ANR in", SeverityLevel.ANR)));

        CrashDetector detector = new CrashDetector(repo);
        String line = prefix + " ANR in com.example.app";

        Optional<CrashEvent> result = detector.analyze("device1", line, List.of(), 1L, 1L);

        assertTrue(result.isPresent());
        assertEquals(SeverityLevel.ANR, result.get().severity());
    }

    @Tag("Feature: crash-detection-alerts, Property 1: Crash detection with correct severity classification")
    @Property(tries = 100)
    void warningPatternsClassifiedAsWarning(
            @ForAll("warningPatterns") String patternStr,
            @ForAll @StringLength(min = 5, max = 50) String prefix) {

        CrashPatternRepository repo = mockRepository();
        when(repo.findByProjectIdAndEnabledTrue(1L))
                .thenReturn(List.of(pattern(patternStr, SeverityLevel.WARNING)));

        CrashDetector detector = new CrashDetector(repo);
        String line = prefix + " " + patternStr + " something";

        Optional<CrashEvent> result = detector.analyze("device1", line, List.of(), 1L, 1L);

        assertTrue(result.isPresent(), "Should detect crash for pattern: " + patternStr);
        assertEquals(SeverityLevel.WARNING, result.get().severity(),
                "Pattern '" + patternStr + "' should be classified as WARNING");
    }

    @Provide
    Arbitrary<String> warningPatterns() {
        return Arbitraries.of("Process crashed", "java.lang.RuntimeException");
    }

    // --- Property 2: Package name extraction ---

    @Tag("Feature: crash-detection-alerts, Property 2: Package name extraction")
    @Property(tries = 100)
    void extractsPackageNameFromLine(@ForAll("androidPackageNames") String packageName) {
        CrashPatternRepository repo = mockRepository();
        when(repo.findByProjectIdAndEnabledTrue(1L))
                .thenReturn(List.of(pattern("FATAL EXCEPTION", SeverityLevel.FATAL)));

        CrashDetector detector = new CrashDetector(repo);
        String line = "E/AndroidRuntime: FATAL EXCEPTION: main in " + packageName;

        Optional<CrashEvent> result = detector.analyze("device1", line, List.of(), 1L, 1L);

        assertTrue(result.isPresent());
        assertEquals(packageName, result.get().packageName(),
                "Should extract package name: " + packageName);
    }

    @Provide
    Arbitrary<String> androidPackageNames() {
        // Generate valid Android package names: com.xxx.yyy format
        Arbitrary<String> segment = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3).ofMaxLength(8);

        return Combinators.combine(segment, segment, segment)
                .as((a, b, c) -> a + "." + b + "." + c);
    }

    // --- Property 3: Stack trace snippet bounded to 20 lines ---

    @Tag("Feature: crash-detection-alerts, Property 3: Stack trace snippet bounded to 20 lines")
    @Property(tries = 100)
    void stackTraceSnippetBoundedTo20Lines(
            @ForAll @IntRange(min = 0, max = 50) int contextLineCount) {

        CrashPatternRepository repo = mockRepository();
        when(repo.findByProjectIdAndEnabledTrue(1L))
                .thenReturn(List.of(pattern("FATAL EXCEPTION", SeverityLevel.FATAL)));

        CrashDetector detector = new CrashDetector(repo);

        List<String> context = new ArrayList<>();
        for (int i = 0; i < contextLineCount; i++) {
            context.add("    at com.example.Class" + i + ".method(Class" + i + ".java:" + i + ")");
        }

        Optional<CrashEvent> result = detector.analyze(
                "device1",
                "E/AndroidRuntime: FATAL EXCEPTION: main in com.example.app",
                context, 1L, 1L);

        assertTrue(result.isPresent());
        String snippet = result.get().stackTraceSnippet();

        if (contextLineCount == 0) {
            assertEquals("", snippet);
        } else {
            long lineCount = snippet.lines().count();
            int expectedLines = Math.min(contextLineCount, 20);
            assertEquals(expectedLines, lineCount,
                    "Stack trace should have min(" + contextLineCount + ", 20) = " + expectedLines + " lines");
        }
    }

    // --- Property 4: Crash event deduplication within time window ---

    @Tag("Feature: crash-detection-alerts, Property 4: Crash event deduplication within time window")
    @Property(tries = 100)
    void duplicateCrashesWithin5SecondsAreSuppressed(
            @ForAll @IntRange(min = 2, max = 10) int crashCount) {

        CrashPatternRepository repo = mockRepository();
        when(repo.findByProjectIdAndEnabledTrue(1L))
                .thenReturn(List.of(pattern("FATAL EXCEPTION", SeverityLevel.FATAL)));

        CrashDetector detector = new CrashDetector(repo);
        String line = "E/AndroidRuntime: FATAL EXCEPTION: main in com.example.app";

        int detectedCount = 0;
        for (int i = 0; i < crashCount; i++) {
            Optional<CrashEvent> result = detector.analyze("device1", line, List.of(), 1L, 1L);
            if (result.isPresent()) {
                detectedCount++;
            }
        }

        // Within the same instant, only the first should be detected
        assertEquals(1, detectedCount,
                "Only one crash event should be emitted within the deduplication window");
    }

    @Tag("Feature: crash-detection-alerts, Property 4: Crash event deduplication within time window")
    @Property(tries = 100)
    void differentDevicesAreNotDeduplicated(
            @ForAll @IntRange(min = 2, max = 10) int deviceCount) {

        CrashPatternRepository repo = mockRepository();
        when(repo.findByProjectIdAndEnabledTrue(1L))
                .thenReturn(List.of(pattern("FATAL EXCEPTION", SeverityLevel.FATAL)));

        CrashDetector detector = new CrashDetector(repo);
        String line = "E/AndroidRuntime: FATAL EXCEPTION: main in com.example.app";

        int detectedCount = 0;
        for (int i = 0; i < deviceCount; i++) {
            Optional<CrashEvent> result = detector.analyze("device" + i, line, List.of(), 1L, 1L);
            if (result.isPresent()) {
                detectedCount++;
            }
        }

        assertEquals(deviceCount, detectedCount,
                "Each device should produce its own crash event");
    }
}
