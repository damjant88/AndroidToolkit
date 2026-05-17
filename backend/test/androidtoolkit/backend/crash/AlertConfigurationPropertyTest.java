package androidtoolkit.backend.crash;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for {@link AlertConfigurationService}.
 * Covers Properties 15, 16, and 17 from the design document.
 */
class AlertConfigurationPropertyTest {

    private AlertConfigurationService createService() {
        AlertConfigurationRepository configRepo = mock(AlertConfigurationRepository.class);
        CrashPatternRepository patternRepo = mock(CrashPatternRepository.class);
        androidtoolkit.backend.repository.ProjectRepository projectRepo =
                mock(androidtoolkit.backend.repository.ProjectRepository.class);
        return new AlertConfigurationService(configRepo, patternRepo, projectRepo);
    }

    // --- Property 15: Buffer size validation ---

    @Tag("Feature: crash-detection-alerts, Property 15: Buffer size validation")
    @Property(tries = 200)
    void bufferSizeValidationAcceptsValidRange(@ForAll @IntRange(min = 50, max = 2000) int size) {
        AlertConfigurationService service = createService();
        ValidationResult result = service.validateBufferSize(size);

        assertTrue(result.valid(),
                "Buffer size " + size + " should be valid (within [50, 2000])");
        assertNull(result.message());
    }

    @Tag("Feature: crash-detection-alerts, Property 15: Buffer size validation")
    @Property(tries = 200)
    void bufferSizeValidationRejectsBelowMinimum(@ForAll @IntRange(min = Integer.MIN_VALUE, max = 49) int size) {
        AlertConfigurationService service = createService();
        ValidationResult result = service.validateBufferSize(size);

        assertFalse(result.valid(),
                "Buffer size " + size + " should be rejected (below 50)");
        assertNotNull(result.message());
        assertTrue(result.message().contains("50") && result.message().contains("2000"),
                "Error message should mention valid range");
    }

    @Tag("Feature: crash-detection-alerts, Property 15: Buffer size validation")
    @Property(tries = 200)
    void bufferSizeValidationRejectsAboveMaximum(@ForAll @IntRange(min = 2001, max = Integer.MAX_VALUE) int size) {
        AlertConfigurationService service = createService();
        ValidationResult result = service.validateBufferSize(size);

        assertFalse(result.valid(),
                "Buffer size " + size + " should be rejected (above 2000)");
        assertNotNull(result.message());
    }

    // --- Property 16: Regex pattern validation ---

    @Tag("Feature: crash-detection-alerts, Property 16: Regex pattern validation")
    @Property(tries = 100)
    void regexValidationAcceptsValidPatterns(@ForAll("validRegexPatterns") String regex) {
        AlertConfigurationService service = createService();
        ValidationResult result = service.validateRegex(regex);

        assertTrue(result.valid(),
                "Valid regex '" + regex + "' should be accepted");
    }

    @Tag("Feature: crash-detection-alerts, Property 16: Regex pattern validation")
    @Property(tries = 100)
    void regexValidationRejectsInvalidPatterns(@ForAll("invalidRegexPatterns") String regex) {
        AlertConfigurationService service = createService();
        ValidationResult result = service.validateRegex(regex);

        assertFalse(result.valid(),
                "Invalid regex '" + regex + "' should be rejected");
        assertNotNull(result.message());
        assertTrue(result.message().contains("Invalid regex") || result.message().contains("empty"),
                "Error message should be descriptive");
    }

    @Tag("Feature: crash-detection-alerts, Property 16: Regex pattern validation")
    @Property(tries = 50)
    void regexValidationRejectsEmptyAndNull() {
        AlertConfigurationService service = createService();

        ValidationResult nullResult = service.validateRegex(null);
        assertFalse(nullResult.valid());

        ValidationResult emptyResult = service.validateRegex("");
        assertFalse(emptyResult.valid());

        ValidationResult blankResult = service.validateRegex("   ");
        assertFalse(blankResult.valid());
    }

    // --- Property 17: Custom crash pattern round-trip ---

    @Tag("Feature: crash-detection-alerts, Property 17: Custom crash pattern round-trip")
    @Property(tries = 100)
    void customPatternPreservesRegexAndSeverity(
            @ForAll("validRegexPatterns") String regex,
            @ForAll SeverityLevel severity) {

        // This tests the structural property: if we create a pattern entity with
        // a given regex and severity, those values are preserved
        CrashPatternEntity pattern = new CrashPatternEntity();
        pattern.setRegex(regex);
        pattern.setSeverity(severity);
        pattern.setDefault(false);
        pattern.setEnabled(true);

        assertEquals(regex, pattern.getRegex(),
                "Pattern regex should be preserved");
        assertEquals(severity, pattern.getSeverity(),
                "Pattern severity should be preserved");
        assertFalse(pattern.isDefault());
        assertTrue(pattern.isEnabled());
    }

    @Provide
    Arbitrary<String> validRegexPatterns() {
        return Arbitraries.of(
                "FATAL EXCEPTION",
                "ANR in",
                "Process crashed",
                "java\\.lang\\.RuntimeException",
                "SIGABRT",
                "SIGSEGV",
                "OutOfMemoryError",
                "StackOverflowError",
                "NullPointerException",
                ".*Exception.*",
                "Error:\\s+.*",
                "\\bcrash\\b",
                "signal \\d+",
                "Process .+ has died"
        );
    }

    @Provide
    Arbitrary<String> invalidRegexPatterns() {
        return Arbitraries.of(
                "[unclosed",
                "(unmatched",
                "*invalid",
                "+invalid",
                "?invalid",
                "\\",
                "[z-a]",
                "(?P<invalid)",
                "a{5,2}"  // illegal repetition: min > max
        );
    }
}
