package androidtoolkit.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/**
 * Fetches and parses RC artifact data from Confluence pages.
 */
@Service
public class ConfluenceService {

    private static final Logger log = LoggerFactory.getLogger(ConfluenceService.class);

    @Value("${confluence.url:https://smithmicro.atlassian.net/wiki}")
    private String confluenceUrl;

    @Value("${confluence.username:}")
    private String username;

    @Value("${confluence.api-token:}")
    private String apiToken;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Fetches the latest RC artifacts page for a given project search term.
     * Returns parsed component artifacts from the "Main" table.
     */
    public Map<String, Object> getLatestRcArtifacts(String searchTerm) {
        try {
            // Search for the page
            String searchUrl = confluenceUrl + "/rest/api/content?title=" +
                    java.net.URLEncoder.encode(searchTerm, StandardCharsets.UTF_8) +
                    "&spaceKey=SP&expand=body.storage";

            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(searchUrl, HttpMethod.GET, entity, Map.class);
            Map body = response.getBody();
            if (body == null) return Map.of("error", "No response from Confluence");

            List<Map> results = (List<Map>) body.get("results");
            if (results == null || results.isEmpty()) {
                return Map.of("error", "Page not found: " + searchTerm);
            }

            Map page = results.get(0);
            String title = (String) page.get("title");
            String pageId = String.valueOf(page.get("id"));
            Map bodyContent = (Map) page.get("body");
            Map storage = (Map) bodyContent.get("storage");
            String html = (String) storage.get("value");

            // Parse the HTML table
            List<Map<String, String>> components = parseMainTable(html);

            return Map.of(
                    "title", title,
                    "pageId", pageId,
                    "url", confluenceUrl + "/spaces/SP/pages/" + pageId,
                    "components", components
            );
        } catch (Exception e) {
            log.error("Failed to fetch Confluence page: {}", e.getMessage());
            return Map.of("error", "Failed to fetch: " + e.getMessage());
        }
    }

    /**
     * Parses the "Main" table from Confluence storage format HTML.
     * Extracts Component, Version, and Artifact(s) columns.
     */
    private List<Map<String, String>> parseMainTable(String html) {
        List<Map<String, String>> components = new ArrayList<>();

        // Find rows in the HTML tables
        Pattern rowPattern = Pattern.compile("<tr>(.*?)</tr>", Pattern.DOTALL);
        Pattern cellPattern = Pattern.compile("<t[dh][^>]*>(.*?)</t[dh]>", Pattern.DOTALL);
        Pattern linkPattern = Pattern.compile("href=\"([^\"]+)\"");
        Pattern s3Pattern = Pattern.compile("(s3://safepath-builds/[^\\s<\"]+)");

        Matcher rowMatcher = rowPattern.matcher(html);
        boolean inMainTable = false;
        boolean headerSkipped = false;

        while (rowMatcher.find()) {
            String row = rowMatcher.group(1);
            List<String> cells = new ArrayList<>();
            Matcher cellMatcher = cellPattern.matcher(row);
            while (cellMatcher.find()) {
                String cell = cellMatcher.group(1)
                        .replaceAll("<[^>]+>", " ")
                        .replaceAll("&nbsp;", " ")
                        .replaceAll("\\s+", " ")
                        .trim();
                cells.add(cell);
            }

            if (cells.isEmpty()) continue;

            // Detect "Main" section header
            if (cells.size() >= 1 && cells.get(0).contains("Component") && row.contains("Artifact")) {
                inMainTable = true;
                headerSkipped = true;
                continue;
            }

            if (inMainTable && cells.size() >= 5) {
                String component = cells.get(0).trim();
                String spVersion = cells.get(1).trim();
                String version = cells.get(2).trim();
                String gitRef = cells.get(3).trim();
                String artifacts = cells.get(4).trim();

                // Extract S3 paths
                List<String> s3Paths = new ArrayList<>();
                Matcher s3Matcher = s3Pattern.matcher(row);
                while (s3Matcher.find()) {
                    s3Paths.add(s3Matcher.group(1));
                }

                Map<String, String> entry = new LinkedHashMap<>();
                entry.put("component", component);
                entry.put("spVersion", spVersion);
                entry.put("version", version);
                entry.put("gitRef", gitRef.length() > 12 ? gitRef.substring(0, 12) : gitRef);
                entry.put("artifacts", artifacts);
                entry.put("s3Paths", String.join("|", s3Paths));
                components.add(entry);
            }
        }

        return components;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (!username.isEmpty() && !apiToken.isEmpty()) {
            String auth = username + ":" + apiToken;
            String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encoded);
        }
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
}
