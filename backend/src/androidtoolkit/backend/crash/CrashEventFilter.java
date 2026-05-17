package androidtoolkit.backend.crash;

import java.time.LocalDate;

/**
 * Filter criteria for querying crash events.
 * All fields are optional — null values indicate no filtering on that field.
 */
public record CrashEventFilter(
    String deviceSerial,
    LocalDate fromDate,
    LocalDate toDate,
    SeverityLevel severity,
    String crashType,
    Boolean acknowledged
) {}
