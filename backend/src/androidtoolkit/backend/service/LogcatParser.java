package androidtoolkit.backend.service;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stateless parser that extracts structured data from OkHttp logcat lines.
 * Applies regex patterns in order and returns the first match.
 */
@Component
public class LogcatParser {

    private static final Pattern ENVIRONMENT_PATTERN =
            Pattern.compile("-->\\s+\\w+\\s+https?://([^/]+)/");

    private static final Pattern CLIENT_VERSION_PATTERN =
            Pattern.compile("User-Agent:\\s+(?:[\\w%]+\\+)+\\w+\\s+(\\S+)");

    private static final Pattern SERVER_PRODUCT_VERSION_PATTERN =
            Pattern.compile("x-safepath-product-version:\\s+(\\S+)");

    private static final Pattern SERVER_PROJECT_VERSION_PATTERN =
            Pattern.compile("x-safepath-project-version:\\s+(\\S+)");

    private static final Pattern ACCESS_TOKEN_RESPONSE_PATTERN =
            Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"");

    private static final Pattern ACCESS_TOKEN_REQUEST_PATTERN =
            Pattern.compile("Authorization:\\s+Bearer\\s+(\\S+)");

    /**
     * Parses a single logcat line and extracts the first matching structured field.
     *
     * @param line a raw logcat line
     * @return the first matching ParsedField, or empty if no pattern matches
     */
    public Optional<ParsedField> parseLine(String line) {
        if (line == null || line.isEmpty()) {
            return Optional.empty();
        }

        Matcher matcher;

        matcher = ENVIRONMENT_PATTERN.matcher(line);
        if (matcher.find()) {
            String host = matcher.group(1);
            // Exclude download-prefixed, api-prefixed, vc01-prefixed, and urldb-prefixed environments
            if (!host.startsWith("download.") && !host.startsWith("api.") && !host.startsWith("vc01.") && !host.startsWith("urldb.")) {
                return Optional.of(new ParsedField(FieldType.ENVIRONMENT, host));
            }
        }

        matcher = CLIENT_VERSION_PATTERN.matcher(line);
        if (matcher.find()) {
            return Optional.of(new ParsedField(FieldType.CLIENT_VERSION, matcher.group(1)));
        }

        matcher = SERVER_PRODUCT_VERSION_PATTERN.matcher(line);
        if (matcher.find()) {
            return Optional.of(new ParsedField(FieldType.SERVER_PRODUCT_VERSION, matcher.group(1)));
        }

        matcher = SERVER_PROJECT_VERSION_PATTERN.matcher(line);
        if (matcher.find()) {
            return Optional.of(new ParsedField(FieldType.SERVER_PROJECT_VERSION, matcher.group(1)));
        }

        matcher = ACCESS_TOKEN_RESPONSE_PATTERN.matcher(line);
        if (matcher.find()) {
            return Optional.of(new ParsedField(FieldType.ACCESS_TOKEN, matcher.group(1)));
        }

        matcher = ACCESS_TOKEN_REQUEST_PATTERN.matcher(line);
        if (matcher.find()) {
            return Optional.of(new ParsedField(FieldType.ACCESS_TOKEN, matcher.group(1)));
        }

        return Optional.empty();
    }
}
