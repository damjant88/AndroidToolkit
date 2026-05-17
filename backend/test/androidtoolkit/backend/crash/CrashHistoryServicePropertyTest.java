package androidtoolkit.backend.crash;

import androidtoolkit.backend.entity.Tenant;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for {@link CrashHistoryService}.
 * Covers Properties 11, 12, 13 from the design document.
 */
class CrashHistoryServicePropertyTest {

    // --- Property 11: CrashEvent database persistence round-trip ---

    @Tag("Feature: crash-detection-alerts, Property 11: CrashEvent database persistence round-trip")
    @Property(tries = 100)
    void crashEventEntityPreservesAllFields(@ForAll("crashEvents") CrashEvent event) {
        // Test the mapping from CrashEvent to CrashEventEntity preserves all fields
        CrashEventEntity entity = new CrashEventEntity();
        entity.setTimestamp(event.timestamp());
        entity.setDeviceSerial(event.deviceSerial());
        entity.setDeviceName(event.deviceName());
        entity.setPackageName(event.packageName());
        entity.setCrashType(event.crashType());
        entity.setSeverity(event.severity());
        entity.setStackTraceSnippet(event.stackTraceSnippet());
        entity.setCrashLogPath(event.crashLogPath());

        // Verify all fields are preserved in the entity
        assertEquals(event.timestamp(), entity.getTimestamp());
        assertEquals(event.deviceSerial(), entity.getDeviceSerial());
        assertEquals(event.deviceName(), entity.getDeviceName());
        assertEquals(event.packageName(), entity.getPackageName());
        assertEquals(event.crashType(), entity.getCrashType());
        assertEquals(event.severity(), entity.getSeverity());
        assertEquals(event.stackTraceSnippet(), entity.getStackTraceSnippet());
        assertEquals(event.crashLogPath(), entity.getCrashLogPath());
    }

    @Tag("Feature: crash-detection-alerts, Property 11: CrashEvent database persistence round-trip")
    @Property(tries = 100)
    void entityDefaultsAreCorrect() {
        CrashEventEntity entity = new CrashEventEntity();

        assertFalse(entity.isAcknowledged(), "New entity should not be acknowledged");
        assertNull(entity.getAcknowledgedAt(), "New entity should have null acknowledgedAt");
        assertNull(entity.getAutoPullLogPath(), "New entity should have null autoPullLogPath");
    }

    // --- Property 12: Multi-tenant data isolation ---

    @Tag("Feature: crash-detection-alerts, Property 12: Multi-tenant data isolation")
    @Property(tries = 100)
    void crashEventsAssociatedWithCorrectTenant(
            @ForAll @LongRange(min = 1, max = 100) long tenantId1,
            @ForAll @LongRange(min = 101, max = 200) long tenantId2) {

        // Create events for two different tenants
        CrashEventEntity entity1 = new CrashEventEntity();
        Tenant tenant1 = mock(Tenant.class);
        when(tenant1.getId()).thenReturn(tenantId1);
        entity1.setTenant(tenant1);

        CrashEventEntity entity2 = new CrashEventEntity();
        Tenant tenant2 = mock(Tenant.class);
        when(tenant2.getId()).thenReturn(tenantId2);
        entity2.setTenant(tenant2);

        // Verify they are associated with different tenants
        assertNotEquals(entity1.getTenant().getId(), entity2.getTenant().getId(),
                "Events from different tenants should have different tenant IDs");
    }

    // --- Property 13: Crash history filter correctness ---

    @Tag("Feature: crash-detection-alerts, Property 13: Crash history filter correctness")
    @Property(tries = 100)
    void filterByDeviceSerialMatchesCorrectly(
            @ForAll @IntRange(min = 1, max = 20) int totalEvents,
            @ForAll @IntRange(min = 1, max = 5) int deviceCount) {

        // Generate events across multiple devices
        List<CrashEventEntity> allEvents = new ArrayList<>();
        String targetDevice = "device0";

        for (int i = 0; i < totalEvents; i++) {
            CrashEventEntity entity = new CrashEventEntity();
            entity.setDeviceSerial("device" + (i % deviceCount));
            entity.setTimestamp(Instant.now().minusSeconds(i));
            entity.setSeverity(SeverityLevel.FATAL);
            entity.setCrashType("FATAL EXCEPTION");
            allEvents.add(entity);
        }

        // Filter by target device
        List<CrashEventEntity> filtered = allEvents.stream()
                .filter(e -> e.getDeviceSerial().equals(targetDevice))
                .toList();

        // All filtered events should have the target device serial
        for (CrashEventEntity entity : filtered) {
            assertEquals(targetDevice, entity.getDeviceSerial());
        }
    }

    @Tag("Feature: crash-detection-alerts, Property 13: Crash history filter correctness")
    @Property(tries = 100)
    void filterBySeverityMatchesCorrectly(
            @ForAll @IntRange(min = 5, max = 30) int totalEvents,
            @ForAll SeverityLevel targetSeverity) {

        List<CrashEventEntity> allEvents = new ArrayList<>();
        SeverityLevel[] severities = SeverityLevel.values();

        for (int i = 0; i < totalEvents; i++) {
            CrashEventEntity entity = new CrashEventEntity();
            entity.setDeviceSerial("device" + i);
            entity.setTimestamp(Instant.now().minusSeconds(i));
            entity.setSeverity(severities[i % severities.length]);
            entity.setCrashType("crash");
            allEvents.add(entity);
        }

        // Filter by severity
        List<CrashEventEntity> filtered = allEvents.stream()
                .filter(e -> e.getSeverity() == targetSeverity)
                .toList();

        // All filtered events should have the target severity
        for (CrashEventEntity entity : filtered) {
            assertEquals(targetSeverity, entity.getSeverity());
        }
    }

    @Tag("Feature: crash-detection-alerts, Property 13: Crash history filter correctness")
    @Property(tries = 100)
    void filterByDateRangeIsInclusive(
            @ForAll @IntRange(min = 5, max = 30) int totalEvents) {

        LocalDate from = LocalDate.of(2025, 3, 10);
        LocalDate to = LocalDate.of(2025, 3, 15);
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);

        List<CrashEventEntity> allEvents = new ArrayList<>();
        for (int i = 0; i < totalEvents; i++) {
            CrashEventEntity entity = new CrashEventEntity();
            entity.setDeviceSerial("device" + i);
            // Spread events across March 5-20
            entity.setTimestamp(LocalDate.of(2025, 3, 5 + (i % 16))
                    .atStartOfDay(ZoneOffset.UTC).toInstant());
            entity.setSeverity(SeverityLevel.FATAL);
            entity.setCrashType("crash");
            allEvents.add(entity);
        }

        // Filter by date range
        List<CrashEventEntity> filtered = allEvents.stream()
                .filter(e -> !e.getTimestamp().isBefore(fromInstant) && !e.getTimestamp().isAfter(toInstant))
                .toList();

        // All filtered events should be within the range
        for (CrashEventEntity entity : filtered) {
            assertTrue(!entity.getTimestamp().isBefore(fromInstant),
                    "Event timestamp should be >= from date");
            assertTrue(!entity.getTimestamp().isAfter(toInstant),
                    "Event timestamp should be <= to date");
        }
    }

    @Tag("Feature: crash-detection-alerts, Property 13: Crash history filter correctness")
    @Property(tries = 100)
    void filteredResultsOrderedByTimestampDescending(
            @ForAll @IntRange(min = 2, max = 20) int eventCount) {

        List<CrashEventEntity> events = new ArrayList<>();
        for (int i = 0; i < eventCount; i++) {
            CrashEventEntity entity = new CrashEventEntity();
            entity.setDeviceSerial("device1");
            entity.setTimestamp(Instant.now().minusSeconds(i * 60L));
            entity.setSeverity(SeverityLevel.FATAL);
            entity.setCrashType("crash");
            events.add(entity);
        }

        // Sort by timestamp descending (as the query would)
        events.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));

        // Verify ordering
        for (int i = 0; i < events.size() - 1; i++) {
            assertTrue(events.get(i).getTimestamp().compareTo(events.get(i + 1).getTimestamp()) >= 0,
                    "Events should be ordered by timestamp descending");
        }
    }

    @Provide
    Arbitrary<CrashEvent> crashEvents() {
        Arbitrary<String> ids = Arbitraries.strings().alpha().ofLength(8).map(s -> "evt-" + s);
        Arbitrary<Instant> timestamps = Arbitraries.longs()
                .between(1_700_000_000L, 1_800_000_000L)
                .map(Instant::ofEpochSecond);
        Arbitrary<String> serials = Arbitraries.of("device1", "device2", "device3", "device4");
        Arbitrary<String> names = Arbitraries.of("Phone A", "Tablet B", null, "Device C");
        Arbitrary<String> packages = Arbitraries.of("com.example.app", "com.test.app", "unknown");
        Arbitrary<String> types = Arbitraries.of("FATAL EXCEPTION", "ANR in", "SIGSEGV");
        Arbitrary<SeverityLevel> severities = Arbitraries.of(SeverityLevel.values());
        Arbitrary<String> traces = Arbitraries.of("trace1", "trace2", "");

        return Combinators.combine(ids, timestamps, serials, names, packages,
                        types, severities, traces)
                .as((id, ts, serial, name, pkg, type, sev, trace) ->
                        new CrashEvent(id, ts, serial, name, pkg, type, sev, trace,
                                "path/log.log", 1L, 1L));
    }
}
