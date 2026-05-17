package androidtoolkit.backend.crash;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.jqwik.api.*;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for CrashEvent serialization round-trip.
 */
@Tag("Feature: crash-detection-alerts, Property 5: CrashEvent serialization round-trip")
class CrashEventSerializationPropertyTest {

    private final ObjectMapper objectMapper;

    CrashEventSerializationPropertyTest() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Property 5: CrashEvent serialization round-trip
     * For any valid CrashEvent, serializing to JSON and deserializing back
     * produces an equivalent CrashEvent with all fields preserved.
     */
    @Property(tries = 200)
    void crashEventRoundTripsViaJson(@ForAll("crashEvents") CrashEvent original) throws Exception {

        String json = objectMapper.writeValueAsString(original);
        CrashEvent deserialized = objectMapper.readValue(json, CrashEvent.class);

        assertEquals(original.id(), deserialized.id());
        assertEquals(original.timestamp(), deserialized.timestamp());
        assertEquals(original.deviceSerial(), deserialized.deviceSerial());
        assertEquals(original.deviceName(), deserialized.deviceName());
        assertEquals(original.packageName(), deserialized.packageName());
        assertEquals(original.crashType(), deserialized.crashType());
        assertEquals(original.severity(), deserialized.severity());
        assertEquals(original.stackTraceSnippet(), deserialized.stackTraceSnippet());
        assertEquals(original.crashLogPath(), deserialized.crashLogPath());
        assertEquals(original.projectId(), deserialized.projectId());
        assertEquals(original.tenantId(), deserialized.tenantId());
    }

    @Property(tries = 100)
    void crashEventSerializationPreservesAllFields(
            @ForAll("crashEvents") CrashEvent original) throws Exception {

        String json = objectMapper.writeValueAsString(original);

        // Verify all fields are present in JSON
        assertTrue(json.contains("\"id\""));
        assertTrue(json.contains("\"deviceSerial\""));
        assertTrue(json.contains("\"severity\""));
        assertTrue(json.contains("\"crashType\""));
        assertTrue(json.contains("\"projectId\""));
        assertTrue(json.contains("\"tenantId\""));
    }

    @Property(tries = 100)
    void roundTripPreservesSeverityEnum(@ForAll SeverityLevel severity) throws Exception {
        CrashEvent event = new CrashEvent(
                UUID.randomUUID().toString(), Instant.now(), "device1", "Device",
                "com.example.app", "FATAL EXCEPTION", severity, "trace",
                "path/log.log", 1L, 1L);

        String json = objectMapper.writeValueAsString(event);
        CrashEvent deserialized = objectMapper.readValue(json, CrashEvent.class);

        assertEquals(severity, deserialized.severity());
    }

    @Provide
    Arbitrary<CrashEvent> crashEvents() {
        Arbitrary<String> ids = Arbitraries.strings().alpha().ofLength(8)
                .map(s -> "evt-" + s);
        Arbitrary<Instant> timestamps = Arbitraries.longs()
                .between(1_000_000_000L, 2_000_000_000L)
                .map(Instant::ofEpochSecond);
        Arbitrary<String> serials = Arbitraries.of("device1", "device2", "device3", "ABC123", "XYZ789");
        Arbitrary<String> deviceNames = Arbitraries.of("Phone A", "Tablet B", null, "Device C", "Pixel 7");
        Arbitrary<String> packageNames = Arbitraries.of(
                "com.example.app", "com.test.myapp", "org.sample.demo", "unknown");
        Arbitrary<String> crashTypes = Arbitraries.of(
                "FATAL EXCEPTION", "ANR in", "SIGSEGV", "Process crashed", "java.lang.RuntimeException");
        Arbitrary<SeverityLevel> severities = Arbitraries.of(SeverityLevel.values());
        Arbitrary<String> stackTraces = Arbitraries.of("at com.example.Main.run(Main.java:42)", "", "trace line");

        // Use combine with 8 params max, then map to add remaining fields
        return Combinators.combine(ids, timestamps, serials, deviceNames, packageNames,
                        crashTypes, severities, stackTraces)
                .as((id, ts, serial, name, pkg, type, sev, trace) ->
                        new CrashEvent(id, ts, serial, name, pkg, type, sev, trace,
                                "crash-logs/1/1/" + serial + "/log.log", 1L, 1L));
    }
}
