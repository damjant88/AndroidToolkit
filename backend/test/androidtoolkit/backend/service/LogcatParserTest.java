package androidtoolkit.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LogcatParserTest {

    private LogcatParser parser;

    @BeforeEach
    void setUp() {
        parser = new LogcatParser();
    }

    // ─── Null and Empty Input ────────────────────────────────────────────────────

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Null and empty input returns empty")
    void nullAndEmptyInput_returnsEmpty(String input) {
        Optional<ParsedField> result = parser.parseLine(input);
        assertTrue(result.isEmpty());
    }

    // ─── Non-matching Lines ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Unrelated logcat line returns empty")
    void unrelatedLine_returnsEmpty() {
        String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : some random log message";
        assertTrue(parser.parseLine(line).isEmpty());
    }

    @Test
    @DisplayName("Line with only whitespace returns empty")
    void whitespaceOnly_returnsEmpty() {
        assertTrue(parser.parseLine("   ").isEmpty());
    }

    // ─── Environment Pattern ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Environment Pattern")
    class EnvironmentPatternTests {

        @Test
        @DisplayName("Extracts hostname from GET request")
        void extractsHostFromGet() {
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : --> GET https://safepath-qa.example.com/api/v1/users";
            Optional<ParsedField> result = parser.parseLine(line);

            assertTrue(result.isPresent());
            assertEquals(FieldType.ENVIRONMENT, result.get().type());
            assertEquals("safepath-qa.example.com", result.get().value());
        }

        @Test
        @DisplayName("Extracts hostname from POST request")
        void extractsHostFromPost() {
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : --> POST https://safepath-prod.example.com/api/v2/devices";
            Optional<ParsedField> result = parser.parseLine(line);

            assertTrue(result.isPresent());
            assertEquals(FieldType.ENVIRONMENT, result.get().type());
            assertEquals("safepath-prod.example.com", result.get().value());
        }

        @Test
        @DisplayName("Extracts hostname from PUT request")
        void extractsHostFromPut() {
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : --> PUT https://staging.safepath.io/settings/update";
            Optional<ParsedField> result = parser.parseLine(line);

            assertTrue(result.isPresent());
            assertEquals(FieldType.ENVIRONMENT, result.get().type());
            assertEquals("staging.safepath.io", result.get().value());
        }

        @Test
        @DisplayName("Extracts hostname from HTTP (non-HTTPS) request")
        void extractsHostFromHttp() {
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : --> GET http://dev-server.internal.net/health";
            Optional<ParsedField> result = parser.parseLine(line);

            assertTrue(result.isPresent());
            assertEquals(FieldType.ENVIRONMENT, result.get().type());
            assertEquals("dev-server.internal.net", result.get().value());
        }

        @Test
        @DisplayName("Excludes host starting with download.")
        void excludesDownloadPrefix() {
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : --> GET https://download.example.com/files/app.apk";
            assertTrue(parser.parseLine(line).isEmpty());
        }

        @Test
        @DisplayName("Excludes host starting with api.")
        void excludesApiPrefix() {
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : --> GET https://api.segment.io/v1/track";
            assertTrue(parser.parseLine(line).isEmpty());
        }

        @Test
        @DisplayName("Excludes host starting with vc01.")
        void excludesVc01Prefix() {
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : --> GET https://vc01.cdn-provider.com/stream";
            assertTrue(parser.parseLine(line).isEmpty());
        }

        @Test
        @DisplayName("Excludes host starting with urldb.")
        void excludesUrldbPrefix() {
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : --> GET https://urldb.safepath.com/lookup";
            assertTrue(parser.parseLine(line).isEmpty());
        }

        @Test
        @DisplayName("Excludes host containing 'assets'")
        void excludesAssetsHost() {
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : --> GET https://static-assets.example.com/images/logo.png";
            assertTrue(parser.parseLine(line).isEmpty());
        }

        @Test
        @DisplayName("Excludes host containing 'aws'")
        void excludesAwsHost() {
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : --> GET https://s3.aws.amazon.com/bucket/file";
            assertTrue(parser.parseLine(line).isEmpty());
        }

        @Test
        @DisplayName("Excludes host with 'aws' in subdomain")
        void excludesAwsSubdomain() {
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : --> POST https://my-bucket.s3.us-east-1.amazonaws.com/upload";
            assertTrue(parser.parseLine(line).isEmpty());
        }
    }

    // ─── Client Version Pattern ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Client Version Pattern")
    class ClientVersionPatternTests {

        @Test
        @DisplayName("Extracts version from SP+Family+DEBUG user agent")
        void extractsVersionFromSpFamilyDebug() {
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : User-Agent: SP+Family+DEBUG 6.18.0.1234";
            Optional<ParsedField> result = parser.parseLine(line);

            assertTrue(result.isPresent());
            assertEquals(FieldType.CLIENT_VERSION, result.get().type());
            assertEquals("6.18.0.1234", result.get().value());
        }

        @Test
        @DisplayName("Extracts version from SpeakEasy+Care user agent")
        void extractsVersionFromSpeakEasyCare() {
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : User-Agent: SpeakEasy+Care 3.5.2";
            Optional<ParsedField> result = parser.parseLine(line);

            assertTrue(result.isPresent());
            assertEquals(FieldType.CLIENT_VERSION, result.get().type());
            assertEquals("3.5.2", result.get().value());
        }

        @Test
        @DisplayName("Extracts version from AT%26T+Secure+Family+Parent+DEBUG user agent")
        void extractsVersionFromAttSecureFamily() {
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : User-Agent: AT%26T+Secure+Family+Parent+DEBUG 7.0.0.5678";
            Optional<ParsedField> result = parser.parseLine(line);

            assertTrue(result.isPresent());
            assertEquals(FieldType.CLIENT_VERSION, result.get().type());
            assertEquals("7.0.0.5678", result.get().value());
        }

        @Test
        @DisplayName("Extracts version with simple two-part app name")
        void extractsVersionFromTwoPartAppName() {
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : User-Agent: MyApp+Release 1.0.0";
            Optional<ParsedField> result = parser.parseLine(line);

            assertTrue(result.isPresent());
            assertEquals(FieldType.CLIENT_VERSION, result.get().type());
            assertEquals("1.0.0", result.get().value());
        }

        @Test
        @DisplayName("Extracts version from SafePath.Connect+DEBUG user agent")
        void extractsVersionFromSafePathConnect() {
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : User-Agent: SafePath.Connect+DEBUG 1.0.0+ga9a34dc SafePath 8.4.4-SNAPSHOT+ga9a34dc Android Pixel 6";
            Optional<ParsedField> result = parser.parseLine(line);

            assertTrue(result.isPresent());
            assertEquals(FieldType.CLIENT_VERSION, result.get().type());
            assertEquals("1.0.0+ga9a34dc", result.get().value());
        }
    }

    // ─── Server Product Version Pattern ──────────────────────────────────────────

    @Nested
    @DisplayName("Server Product Version Pattern")
    class ServerProductVersionTests {

        @Test
        @DisplayName("Extracts server product version")
        void extractsProductVersion() {
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : x-safepath-product-version: 2.45.0";
            Optional<ParsedField> result = parser.parseLine(line);

            assertTrue(result.isPresent());
            assertEquals(FieldType.SERVER_PRODUCT_VERSION, result.get().type());
            assertEquals("2.45.0", result.get().value());
        }

        @Test
        @DisplayName("Extracts server product version with build metadata")
        void extractsProductVersionWithBuild() {
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : x-safepath-product-version: 2.45.0-SNAPSHOT";
            Optional<ParsedField> result = parser.parseLine(line);

            assertTrue(result.isPresent());
            assertEquals(FieldType.SERVER_PRODUCT_VERSION, result.get().type());
            assertEquals("2.45.0-SNAPSHOT", result.get().value());
        }
    }

    // ─── Server Project Version Pattern ──────────────────────────────────────────

    @Nested
    @DisplayName("Server Project Version Pattern")
    class ServerProjectVersionTests {

        @Test
        @DisplayName("Extracts server project version")
        void extractsProjectVersion() {
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : x-safepath-project-version: 1.12.3";
            Optional<ParsedField> result = parser.parseLine(line);

            assertTrue(result.isPresent());
            assertEquals(FieldType.SERVER_PROJECT_VERSION, result.get().type());
            assertEquals("1.12.3", result.get().value());
        }

        @Test
        @DisplayName("Extracts server project version with pre-release tag")
        void extractsProjectVersionWithPreRelease() {
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : x-safepath-project-version: 1.12.3-rc1";
            Optional<ParsedField> result = parser.parseLine(line);

            assertTrue(result.isPresent());
            assertEquals(FieldType.SERVER_PROJECT_VERSION, result.get().type());
            assertEquals("1.12.3-rc1", result.get().value());
        }
    }

    // ─── Access Token (Response Body) Pattern ────────────────────────────────────

    @Nested
    @DisplayName("Access Token (Response Body) Pattern")
    class AccessTokenResponseTests {

        @Test
        @DisplayName("Extracts access token from JSON response body")
        void extractsAccessTokenFromResponse() {
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : \"accessToken\" : \"eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.abc123\"";
            Optional<ParsedField> result = parser.parseLine(line);

            assertTrue(result.isPresent());
            assertEquals(FieldType.ACCESS_TOKEN, result.get().type());
            assertEquals("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.abc123", result.get().value());
        }

        @Test
        @DisplayName("Extracts access token with no spaces around colon")
        void extractsAccessTokenNoSpaces() {
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : \"accessToken\":\"token-value-xyz\"";
            Optional<ParsedField> result = parser.parseLine(line);

            assertTrue(result.isPresent());
            assertEquals(FieldType.ACCESS_TOKEN, result.get().type());
            assertEquals("token-value-xyz", result.get().value());
        }

        @Test
        @DisplayName("Extracts access token with multiple spaces around colon")
        void extractsAccessTokenMultipleSpaces() {
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  :   \"accessToken\"  :  \"my-jwt-token-here\"";
            Optional<ParsedField> result = parser.parseLine(line);

            assertTrue(result.isPresent());
            assertEquals(FieldType.ACCESS_TOKEN, result.get().type());
            assertEquals("my-jwt-token-here", result.get().value());
        }
    }

    // ─── Access Token (Request Header) Pattern ───────────────────────────────────

    @Nested
    @DisplayName("Access Token (Request Header) Pattern")
    class AccessTokenRequestTests {

        @Test
        @DisplayName("Extracts bearer token from Authorization header")
        void extractsBearerToken() {
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : Authorization: Bearer eyJhbGciOiJSUzI1NiJ9.payload.signature";
            Optional<ParsedField> result = parser.parseLine(line);

            assertTrue(result.isPresent());
            assertEquals(FieldType.ACCESS_TOKEN, result.get().type());
            assertEquals("eyJhbGciOiJSUzI1NiJ9.payload.signature", result.get().value());
        }

        @Test
        @DisplayName("Extracts short bearer token")
        void extractsShortBearerToken() {
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : Authorization: Bearer abc123xyz";
            Optional<ParsedField> result = parser.parseLine(line);

            assertTrue(result.isPresent());
            assertEquals(FieldType.ACCESS_TOKEN, result.get().type());
            assertEquals("abc123xyz", result.get().value());
        }
    }

    // ─── Priority Order Tests ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Priority Order")
    class PriorityOrderTests {

        @Test
        @DisplayName("Environment is checked before client version")
        void environmentBeforeClientVersion() {
            // A line that could match both environment and client version patterns
            // Environment pattern should win since it's checked first
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : --> GET https://safepath-qa.example.com/api User-Agent: SP+Family+DEBUG 6.18.0";
            Optional<ParsedField> result = parser.parseLine(line);

            assertTrue(result.isPresent());
            assertEquals(FieldType.ENVIRONMENT, result.get().type());
            assertEquals("safepath-qa.example.com", result.get().value());
        }

        @Test
        @DisplayName("Client version is returned when environment is excluded")
        void clientVersionWhenEnvironmentExcluded() {
            // Environment matches but is excluded (api. prefix), so client version should match
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : --> GET https://api.example.com/v1 User-Agent: SP+Family+DEBUG 6.18.0";
            Optional<ParsedField> result = parser.parseLine(line);

            assertTrue(result.isPresent());
            assertEquals(FieldType.CLIENT_VERSION, result.get().type());
            assertEquals("6.18.0", result.get().value());
        }

        @Test
        @DisplayName("Response body access token takes priority over request header token")
        void responseTokenBeforeRequestToken() {
            // A line with both patterns — response body pattern is checked first
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : \"accessToken\":\"response-token\" Authorization: Bearer request-token";
            Optional<ParsedField> result = parser.parseLine(line);

            assertTrue(result.isPresent());
            assertEquals(FieldType.ACCESS_TOKEN, result.get().type());
            assertEquals("response-token", result.get().value());
        }

        @Test
        @DisplayName("Server product version before server project version")
        void productVersionBeforeProjectVersion() {
            String line = "05-11 19:21:12.371 22549 30243 I OkHttp  : x-safepath-product-version: 2.0.0 x-safepath-project-version: 1.0.0";
            Optional<ParsedField> result = parser.parseLine(line);

            assertTrue(result.isPresent());
            assertEquals(FieldType.SERVER_PRODUCT_VERSION, result.get().type());
            assertEquals("2.0.0", result.get().value());
        }
    }
}
