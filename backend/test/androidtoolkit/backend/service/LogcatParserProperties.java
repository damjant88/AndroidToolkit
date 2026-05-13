package androidtoolkit.backend.service;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for {@link LogcatParser}.
 * Each property generates random valid inputs, constructs a logcat line,
 * parses it, and verifies the extracted value matches what was generated.
 */
class LogcatParserProperties {

    private final LogcatParser parser = new LogcatParser();

    // ─── Property 3: Environment Parsing ─────────────────────────────────────────

    @Property
    void environmentParsing_extractsHostname(
            @ForAll("httpMethods") String method,
            @ForAll("validHostnames") String hostname,
            @ForAll("urlPaths") String path
    ) {
        String line = "--> " + method + " https://" + hostname + "/" + path;

        Optional<ParsedField> result = parser.parseLine(line);

        assertTrue(result.isPresent(), "Parser should extract environment from: " + line);
        assertEquals(FieldType.ENVIRONMENT, result.get().type());
        assertEquals(hostname, result.get().value());
    }

    @Provide
    Arbitrary<String> httpMethods() {
        return Arbitraries.of("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS");
    }

    @Provide
    Arbitrary<String> validHostnames() {
        // Generate hostnames that won't be excluded by the parser's filter logic:
        // Must NOT start with download., api., vc01., urldb.
        // Must NOT contain "assets" or "aws"
        Arbitrary<String> subdomain = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3)
                .ofMaxLength(12)
                .filter(s -> !s.startsWith("download") && !s.startsWith("api")
                        && !s.startsWith("vc01") && !s.startsWith("urldb")
                        && !s.contains("assets") && !s.contains("aws"));

        Arbitrary<String> domain = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3)
                .ofMaxLength(10)
                .filter(s -> !s.contains("assets") && !s.contains("aws"));

        Arbitrary<String> tld = Arbitraries.of("com", "net", "org", "io", "dev");

        return Combinators.combine(subdomain, domain, tld)
                .as((sub, dom, t) -> sub + "." + dom + "." + t);
    }

    @Provide
    Arbitrary<String> urlPaths() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(20);
    }

    // ─── Property 4: Client Version Parsing ──────────────────────────────────────

    @Property
    void clientVersionParsing_extractsVersion(
            @ForAll("appNameSegments") String appName,
            @ForAll("versionStrings") String version
    ) {
        String line = "User-Agent: " + appName + " " + version;

        Optional<ParsedField> result = parser.parseLine(line);

        assertTrue(result.isPresent(), "Parser should extract client version from: " + line);
        assertEquals(FieldType.CLIENT_VERSION, result.get().type());
        assertEquals(version, result.get().value());
    }

    @Provide
    Arbitrary<String> appNameSegments() {
        // The pattern requires: (?:[\w%]+\+)+\w+ — one or more word+segments followed by a final word
        Arbitrary<String> segment = Arbitraries.strings()
                .withCharRange('A', 'Z')
                .ofMinLength(2)
                .ofMaxLength(8);

        return segment.list().ofMinSize(2).ofMaxSize(4)
                .map(segments -> String.join("+", segments));
    }

    @Provide
    Arbitrary<String> versionStrings() {
        // Generate version strings like "1.2.3", "6.18.0.1234", "3.5.2-SNAPSHOT"
        Arbitrary<Integer> major = Arbitraries.integers().between(1, 99);
        Arbitrary<Integer> minor = Arbitraries.integers().between(0, 99);
        Arbitrary<Integer> patch = Arbitraries.integers().between(0, 99);
        Arbitrary<String> suffix = Arbitraries.of("", ".1234", ".5678", "-SNAPSHOT", "-rc1");

        return Combinators.combine(major, minor, patch, suffix)
                .as((ma, mi, pa, suf) -> ma + "." + mi + "." + pa + suf);
    }

    // ─── Property 5: Server Product Version Parsing ──────────────────────────────

    @Property
    void serverProductVersionParsing_extractsVersion(
            @ForAll("versionStrings") String version
    ) {
        String line = "x-safepath-product-version: " + version;

        Optional<ParsedField> result = parser.parseLine(line);

        assertTrue(result.isPresent(), "Parser should extract server product version from: " + line);
        assertEquals(FieldType.SERVER_PRODUCT_VERSION, result.get().type());
        assertEquals(version, result.get().value());
    }

    // ─── Property 6: Access Token Parsing ────────────────────────────────────────

    @Property
    void accessTokenParsing_extractsFromResponseBody(
            @ForAll("validTokens") String token
    ) {
        String line = "\"accessToken\":\"" + token + "\"";

        Optional<ParsedField> result = parser.parseLine(line);

        assertTrue(result.isPresent(), "Parser should extract access token from response body: " + line);
        assertEquals(FieldType.ACCESS_TOKEN, result.get().type());
        assertEquals(token, result.get().value());
    }

    @Property
    void accessTokenParsing_extractsFromAuthorizationHeader(
            @ForAll("validTokens") String token
    ) {
        String line = "Authorization: Bearer " + token;

        Optional<ParsedField> result = parser.parseLine(line);

        assertTrue(result.isPresent(), "Parser should extract access token from Authorization header: " + line);
        assertEquals(FieldType.ACCESS_TOKEN, result.get().type());
        assertEquals(token, result.get().value());
    }

    @Provide
    Arbitrary<String> validTokens() {
        // Tokens must not contain quotes or whitespace (regex captures \S+ or [^"]+)
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars('.', '-', '_')
                .ofMinLength(5)
                .ofMaxLength(60)
                .filter(s -> !s.contains("\"") && !s.contains(" ") && !s.isBlank());
    }
}
