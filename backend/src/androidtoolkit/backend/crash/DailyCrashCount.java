package androidtoolkit.backend.crash;

import java.time.LocalDate;

/**
 * Aggregation result representing the crash count for a single day,
 * grouped by a key (severity level or device serial depending on the query).
 */
public record DailyCrashCount(
    LocalDate date,
    String groupKey,
    long count
) {}
