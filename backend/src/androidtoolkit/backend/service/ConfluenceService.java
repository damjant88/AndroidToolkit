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
     * Fetches artifacts by direct page ID.
     */
    public Map<String, Object> getArtifactsByPageId(String pageId) {
        try {
            String url = confluenceUrl + "/rest/api/content/" + pageId + "?expand=body.storage";
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map page = response.getBody();
            if (page == null) return Map.of("error", "No response from Confluence");

            String title = (String) page.get("title");
            Map bodyContent = (Map) page.get("body");
            Map storage = (Map) bodyContent.get("storage");
            String html = (String) storage.get("value");

            List<Map<String, String>> components = parseMainTable(html);

            return Map.of(
                    "title", title,
                    "pageId", pageId,
                    "url", confluenceUrl + "/spaces/SP/pages/" + pageId,
                    "components", components
            );
        } catch (Exception e) {
            log.error("Failed to fetch Confluence page {}: {}", pageId, e.getMessage());
            return Map.of("error", "Failed to fetch page: " + e.getMessage());
        }
    }

    /**
     * Fetches the latest RC artifacts page for a given project search term.
     */
    public Map<String, Object> getLatestRcArtifacts(String searchTerm) {
        try {
            // Use simple content search by title
            String searchUrl = confluenceUrl + "/rest/api/content?spaceKey=SP&title=" +
                    java.net.URLEncoder.encode(searchTerm, StandardCharsets.UTF_8) +
                    "&expand=body.storage&limit=1";

            log.info("Confluence search URL: {}", searchUrl);

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

        // Find the "Main" section — it's after <h1...>Main</h1>
        int mainIdx = html.indexOf(">Main</");
        if (mainIdx == -1) {
            mainIdx = html.indexOf(">Main<");
        }
        if (mainIdx == -1) {
            log.warn("Could not find 'Main' section in page HTML");
            return components;
        }

        // Get the table after "Main"
        String afterMain = html.substring(mainIdx);
        int tableStart = afterMain.indexOf("<tbody>");
        int tableEnd = afterMain.indexOf("</tbody>");
        if (tableStart == -1 || tableEnd == -1) return components;

        String tableBody = afterMain.substring(tableStart, tableEnd);

        // Split into rows
        Pattern rowPattern = Pattern.compile("<tr[^>]*>(.*?)</tr>", Pattern.DOTALL);
        Pattern cellPattern = Pattern.compile("<t[dh][^>]*>(.*?)</t[dh]>", Pattern.DOTALL);
        Pattern s3Pattern = Pattern.compile("(s3://safepath-builds/[^<\\s\"&]+)");

        Matcher rowMatcher = rowPattern.matcher(tableBody);
        boolean headerSkipped = false;

        while (rowMatcher.find()) {
            String row = rowMatcher.group(1);
            List<String> cells = new ArrayList<>();
            Matcher cellMatcher = cellPattern.matcher(row);
            while (cellMatcher.find()) {
                String cell = cellMatcher.group(1)
                        .replaceAll("<[^>]+>", " ")
                        .replaceAll("&nbsp;", " ")
                        .replaceAll("&quot;", "\"")
                        .replaceAll("&amp;", "&")
                        .replaceAll("\\s+", " ")
                        .trim();
                cells.add(cell);
            }

            if (cells.isEmpty()) continue;

            // Skip header row
            if (!headerSkipped && cells.get(0).contains("Component")) {
                headerSkipped = true;
                continue;
            }

            if (headerSkipped && cells.size() >= 5) {
                String component = cells.get(0).trim();
                String spVersion = cells.get(1).trim();
                String version = cells.get(2).trim();
                String gitRef = cells.get(3).trim();

                // Extract S3 paths from the raw row HTML
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
