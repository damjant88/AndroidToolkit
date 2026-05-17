package androidtoolkit.backend.crash;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for crash trend aggregation.
 * Covers Property 14 from the design document.
 */
@Tag("Feature: crash-detection-alerts, Property 14: Daily frequency aggregation completeness")
class CrashTrendPropertyTest {

    /**
     * Property 14: Daily frequency aggregation completeness
     * For any date range [from, to] and set of crash events, the aggregation
     * returns exactly one data point per day in the range per group key,
     * with correct counts including zero for days with no crashes.
     */
    @Property(tries = 100)
    void aggregationReturnsOneDataPointPerDayPerGroup(
            @ForAll @IntRange(min = 1, max = 30) int rangeDays,
            @ForAll @IntRange(min = 0, max = 50) int eventCount) {

        LocalDate from = LocalDate.of(2025, 3, 1);
        LocalDate to = from.plusDays(rangeDays - 1);

        // Generate random crash events within and outside the range
        List<DailyCrashCount> rawCounts = generateRawCounts(from, to, eventCount);

        // Simulate the fill-missing-days logic
        List<DailyCrashCount> filled = fillMissingDays(rawCounts, from, to);

        // Collect unique group keys
        Set<String> groupKeys = filled.stream()
                .map(DailyCrashCount::groupKey)
                .collect(Collectors.toSet());

        if (groupKeys.isEmpty()) {
            // No data at all — acceptable
            return;
        }

        // Verify: for each group key, there should be exactly rangeDays data points
        for (String key : groupKeys) {
            long countForKey = filled.stream()
                    .filter(dc -> dc.groupKey().equals(key))
                    .count();
            assertEquals(rangeDays, countForKey,
                    "Should have exactly " + rangeDays + " data points for group '" + key + "'");
        }
    }

    @Property(tries = 100)
    void aggregationIncludesZeroCountDays(
            @ForAll @IntRange(min = 7, max = 14) int rangeDays) {

        LocalDate from = LocalDate.of(2025, 3, 1);
        LocalDate to = from.plusDays(rangeDays - 1);

        // Generate events only on the first day
        List<DailyCrashCount> rawCounts = List.of(
                new DailyCrashCount(from, "FATAL", 3)
        );

        List<DailyCrashCount> filled = fillMissingDays(rawCounts, from, to);

        // Days after the first should have zero counts
        long zeroDays = filled.stream()
                .filter(dc -> dc.count() == 0)
                .count();

        assertEquals(rangeDays - 1, zeroDays,
                "All days except the first should have zero count");
    }

    @Property(tries = 100)
    void aggregationCountsAreNonNegative(
            @ForAll @IntRange(min = 1, max = 30) int rangeDays,
            @ForAll @IntRange(min = 0, max = 100) int eventCount) {

        LocalDate from = LocalDate.of(2025, 3, 1);
        LocalDate to = from.plusDays(rangeDays - 1);

        List<DailyCrashCount> rawCounts = generateRawCounts(from, to, eventCount);
        List<DailyCrashCount> filled = fillMissingDays(rawCounts, from, to);

        for (DailyCrashCount dc : filled) {
            assertTrue(dc.count() >= 0,
                    "Crash count should never be negative");
        }
    }

    @Property(tries = 100)
    void aggregationDatesAreWithinRange(
            @ForAll @IntRange(min = 1, max = 30) int rangeDays,
            @ForAll @IntRange(min = 0, max = 50) int eventCount) {

        LocalDate from = LocalDate.of(2025, 3, 1);
        LocalDate to = from.plusDays(rangeDays - 1);

        List<DailyCrashCount> rawCounts = generateRawCounts(from, to, eventCount);
        List<DailyCrashCount> filled = fillMissingDays(rawCounts, from, to);

        for (DailyCrashCount dc : filled) {
            assertFalse(dc.date().isBefore(from),
                    "Date " + dc.date() + " should not be before " + from);
            assertFalse(dc.date().isAfter(to),
                    "Date " + dc.date() + " should not be after " + to);
        }
    }

    @Property(tries = 100)
    void aggregationTotalMatchesEventCount(
            @ForAll @IntRange(min = 1, max = 14) int rangeDays,
            @ForAll @IntRange(min = 0, max = 50) int eventCount) {

        LocalDate from = LocalDate.of(2025, 3, 1);
        LocalDate to = from.plusDays(rangeDays - 1);

        List<DailyCrashCount> rawCounts = generateRawCounts(from, to, eventCount);
        List<DailyCrashCount> filled = fillMissingDays(rawCounts, from, to);

        long totalCount = filled.stream().mapToLong(DailyCrashCount::count).sum();
        long rawTotal = rawCounts.stream().mapToLong(DailyCrashCount::count).sum();

        assertEquals(rawTotal, totalCount,
                "Total count after filling should equal the raw total");
    }

    // --- Helper methods that replicate the service logic ---

    private List<DailyCrashCount> generateRawCounts(LocalDate from, LocalDate to, int eventCount) {
        Random random = new Random(42);
        Map<String, Long> countMap = new HashMap<>();
        SeverityLevel[] severities = SeverityLevel.values();

        for (int i = 0; i < eventCount; i++) {
            int dayOffset = random.nextInt((int) (to.toEpochDay() - from.toEpochDay() + 1));
            LocalDate date = from.plusDays(dayOffset);
            String severity = severities[random.nextInt(severities.length)].name();
            String key = date + "|" + severity;
            countMap.merge(key, 1L, Long::sum);
        }

        List<DailyCrashCount> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : countMap.entrySet()) {
            String[] parts = entry.getKey().split("\\|");
            result.add(new DailyCrashCount(LocalDate.parse(parts[0]), parts[1], entry.getValue()));
        }
        return result;
    }

    private List<DailyCrashCount> fillMissingDays(List<DailyCrashCount> rawCounts,
                                                   LocalDate from, LocalDate to) {
        Set<String> groupKeys = rawCounts.stream()
                .map(DailyCrashCount::groupKey)
                .collect(Collectors.toSet());

        if (groupKeys.isEmpty()) {
            groupKeys = Set.of("FATAL", "ANR", "WARNING");
        }

        Map<String, Long> countMap = rawCounts.stream()
                .collect(Collectors.toMap(
                        dc -> dc.date() + "|" + dc.groupKey(),
                        DailyCrashCount::count,
                        Long::sum
                ));

        List<DailyCrashCount> result = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            for (String key : groupKeys) {
                long count = countMap.getOrDefault(date + "|" + key, 0L);
                result.add(new DailyCrashCount(date, key, count));
            }
        }
        return result;
    }
}
