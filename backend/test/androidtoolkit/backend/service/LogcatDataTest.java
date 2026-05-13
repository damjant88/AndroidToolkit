package androidtoolkit.backend.service;

import androidtoolkit.backend.dto.LogcatData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.*;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for {@link LogcatData} state management.
 */
class LogcatDataTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─── Property 7: Last Write Wins ─────────────────────────────────────────────

    @Property
    void lastWriteWins_finalStateEqualsLastValueOfEachType(
            @ForAll("parsedFieldSequences") List<ParsedField> fields
    ) {
        LogcatData data = new LogcatData("test-serial");

        // Apply all fields in sequence
        for (ParsedField field : fields) {
            switch (field.type()) {
                case ENVIRONMENT -> data.setEnvironment(field.value());
                case CLIENT_VERSION -> data.setClientVersion(field.value());
                case SERVER_PRODUCT_VERSION -> data.setServerProductVersion(field.value());
                case SERVER_PROJECT_VERSION -> data.setServerProjectVersion(field.value());
                case ACCESS_TOKEN -> data.setAccessToken(field.value());
            }
        }

        // Determine expected last value for each type
        String expectedEnvironment = lastValueForType(fields, FieldType.ENVIRONMENT);
        String expectedClientVersion = lastValueForType(fields, FieldType.CLIENT_VERSION);
        String expectedServerProductVersion = lastValueForType(fields, FieldType.SERVER_PRODUCT_VERSION);
        String expectedServerProjectVersion = lastValueForType(fields, FieldType.SERVER_PROJECT_VERSION);
        String expectedAccessToken = lastValueForType(fields, FieldType.ACCESS_TOKEN);

        assertEquals(expectedEnvironment, data.getEnvironment());
        assertEquals(expectedClientVersion, data.getClientVersion());
        assertEquals(expectedServerProductVersion, data.getServerProductVersion());
        assertEquals(expectedServerProjectVersion, data.getServerProjectVersion());
        assertEquals(expectedAccessToken, data.getAccessToken());
    }

    private String lastValueForType(List<ParsedField> fields, FieldType type) {
        return fields.stream()
                .filter(f -> f.type() == type)
                .reduce((first, second) -> second)
                .map(ParsedField::value)
                .orElse(null);
    }

    @Provide
    Arbitrary<List<ParsedField>> parsedFieldSequences() {
        Arbitrary<ParsedField> fieldArbitrary = Arbitraries.of(FieldType.values())
                .flatMap(type -> Arbitraries.strings()
                        .withCharRange('a', 'z')
                        .withCharRange('0', '9')
                        .ofMinLength(3)
                        .ofMaxLength(20)
                        .map(value -> new ParsedField(type, value)));

        return fieldArbitrary.list().ofMinSize(1).ofMaxSize(30);
    }

    // ─── Property 8: Broadcast Completeness ──────────────────────────────────────

    @Property
    void broadcastCompleteness_jsonContainsAllFiveKeys(
            @ForAll("randomLogcatData") LogcatData data
    ) throws Exception {
        String json = objectMapper.writeValueAsString(data);
        JsonNode node = objectMapper.readTree(json);

        assertTrue(node.has("serial"), "JSON must contain 'serial' key");
        assertTrue(node.has("environment"), "JSON must contain 'environment' key");
        assertTrue(node.has("clientVersion"), "JSON must contain 'clientVersion' key");
        assertTrue(node.has("serverProductVersion"), "JSON must contain 'serverProductVersion' key");
        assertTrue(node.has("accessToken"), "JSON must contain 'accessToken' key");
    }

    @Provide
    Arbitrary<LogcatData> randomLogcatData() {
        Arbitrary<String> serial = Arbitraries.strings()
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .ofMinLength(5)
                .ofMaxLength(16);

        Arbitrary<String> nullableValue = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3)
                .ofMaxLength(15)
                .injectNull(0.3);

        return Combinators.combine(serial, nullableValue, nullableValue, nullableValue, nullableValue)
                .as((ser, env, clientVer, serverProdVer, token) -> {
                    LogcatData data = new LogcatData(ser);
                    data.setEnvironment(env);
                    data.setClientVersion(clientVer);
                    data.setServerProductVersion(serverProdVer);
                    data.setAccessToken(token);
                    return data;
                });
    }
}
