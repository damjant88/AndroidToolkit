package androidtoolkit.backend.crash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Core crash detection engine. Analyzes logcat lines against registered crash patterns,
 * applies per-device deduplication within a 5-second window, extracts package names,
 * classifies severity, and produces CrashEvent objects.
 */
@Service
public class CrashDetector {

    private static final Logger log = LoggerFactory.getLogger(CrashDetector.class);

    /**
     * Deduplication window in seconds. Crashes from the same device within this
     * window are suppressed.
     */
    static final long DEDUP_WINDOW_SECONDS = 5;

    /**
     * Maximum number of stack trace lines to capture from the context.
     */
    static final int MAX_STACK_TRACE_LINES = 20;

    /**
     * Pattern to match Android package names (e.g., com.example.app).
     * Matches sequences of lowercase letters/digits separated by dots, with at least two segments.
     */
    private static final Pattern PACKAGE_NAME_PATTERN =
            Pattern.compile("\\b([a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*){2,})\\b");

    private final CrashPatternRepository crashPatternRepository;

    /**
     * Tracks the last crash timestamp per device serial for deduplication.
     */
    private final ConcurrentHashMap<String, Instant> lastCrashTimestamps = new ConcurrentHashMap<>();

    public CrashDetector(CrashPatternRepository crashPatternRepository) {
        this.crashPatternRepository = crashPatternRepository;
    }

    /**
     * Analyzes a logcat line against all active crash patterns for the given project.
     * If a match is found and the event is not a duplicate, returns a CrashEvent.
     *
     * @param serial    the device serial number
     * @param line      the current logcat line to analyze
     * @param context   subsequent lines for stack trace extraction (may be null or empty)
     * @param projectId the project ID to load patterns for
     * @param tenantId  the tenant ID for the crash event
     * @return an Optional containing the CrashEvent if a crash was detected, or empty
     */
    public Optional<CrashEvent> analyze(String serial, String line, List<String> context,
                                        Long projectId, Long tenantId) {
        if (line == null || line.isEmpty()) {
            return Optional.empty();
        }

        List<CrashPatternEntity> activePatterns = crashPatternRepository.findByProjectIdAndEnabledTrue(projectId);

        for (CrashPatternEntity pattern : activePatterns) {
            try {
                Pattern compiledPattern = Pattern.compile(pattern.getRegex());
                if (compiledPattern.matcher(line).find()) {
                    // Found a match - build the crash event
                    SeverityLevel severity = classifySeverity(pattern.getRegex());
                    String packageName = extractPackageName(line, context);
                    String stackTrace = extractStackTrace(context);

                    CrashEvent event = new CrashEvent(
                            UUID.randomUUID().toString(),
                            Instant.now(),
                            serial,
                            null, // deviceName is resolved downstream
                            packageName,
                            pattern.getRegex(),
                            severity,
                            stackTrace,
                            null, // crashLogPath set after upload
                            projectId,
                            tenantId
                    );

                    // Check deduplication
                    if (isDuplicate(serial, event)) {
                        log.debug("Suppressed duplicate crash event for device {}", serial);
                        return Optional.empty();
                    }

                    // Record this crash timestamp for deduplication
                    lastCrashTimestamps.put(serial, event.timestamp());

                    return Optional.of(event);
                }
            } catch (Exception e) {
                log.warn("Failed to compile or match pattern '{}': {}", pattern.getRegex(), e.getMessage());
            }
        }

        return Optional.empty();
    }

    /**
     * Checks whether a crash event should be suppressed due to deduplication.
     * Returns true if the same device had a crash within the last 5 seconds.
     *
     * @param serial the device serial number
     * @param event  the crash event to check
     * @return true if this crash should be suppressed (is a duplicate)
     */
    boolean isDuplicate(String serial, CrashEvent event) {
        Instant lastCrash = lastCrashTimestamps.get(serial);
        if (lastCrash == null) {
            return false;
        }
        long secondsSinceLastCrash = java.time.Duration.between(lastCrash, event.timestamp()).getSeconds();
        return secondsSinceLastCrash < DEDUP_WINDOW_SECONDS;
    }

    /**
     * Extracts the Android package name from the logcat line and context lines.
     * Looks for standard Android package name format (e.g., com.example.app).
     * Returns "unknown" if no package name can be found.
     *
     * @param line    the crash logcat line
     * @param context subsequent context lines
     * @return the extracted package name, or "unknown" if not found
     */
    String extractPackageName(String line, List<String> context) {
        // First try to extract from the crash line itself
        String packageName = findPackageName(line);
        if (packageName != null) {
            return packageName;
        }

        // Then try context lines
        if (context != null) {
            for (String contextLine : context) {
                packageName = findPackageName(contextLine);
                if (packageName != null) {
                    return packageName;
                }
            }
        }

        return "unknown";
    }

    /**
     * Classifies the severity level based on the matched crash pattern.
     * FATAL for FATAL EXCEPTION, SIGABRT, SIGSEGV.
     * ANR for "ANR in".
     * WARNING for all other patterns.
     *
     * @param matchedPattern the regex pattern that matched
     * @return the classified severity level
     */
    SeverityLevel classifySeverity(String matchedPattern) {
        if (matchedPattern == null) {
            return SeverityLevel.WARNING;
        }

        // Check for FATAL patterns
        if (matchedPattern.contains("FATAL EXCEPTION")
                || matchedPattern.contains("SIGABRT")
                || matchedPattern.contains("SIGSEGV")) {
            return SeverityLevel.FATAL;
        }

        // Check for ANR pattern
        if (matchedPattern.contains("ANR in")) {
            return SeverityLevel.ANR;
        }

        // All other patterns are WARNING
        return SeverityLevel.WARNING;
    }

    /**
     * Extracts a stack trace snippet from the context lines, limited to MAX_STACK_TRACE_LINES.
     *
     * @param context the lines following the crash signature
     * @return the stack trace snippet (up to 20 lines), or empty string if no context
     */
    private String extractStackTrace(List<String> context) {
        if (context == null || context.isEmpty()) {
            return "";
        }

        int linesToCapture = Math.min(context.size(), MAX_STACK_TRACE_LINES);
        return context.stream()
                .limit(linesToCapture)
                .collect(Collectors.joining("\n"));
    }

    /**
     * Attempts to find an Android package name in a single line.
     *
     * @param line the line to search
     * @return the package name if found, or null
     */
    private String findPackageName(String line) {
        if (line == null) {
            return null;
        }
        Matcher matcher = PACKAGE_NAME_PATTERN.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Clears the deduplication state. Useful for testing.
     */
    void clearDeduplicationState() {
        lastCrashTimestamps.clear();
    }
}
