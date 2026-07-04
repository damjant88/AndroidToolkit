package androidtoolkit.backend.controller;

import androidtoolkit.backend.service.ConfluenceService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/confluence")
public class ConfluenceController {

    private final ConfluenceService confluenceService;

    public ConfluenceController(ConfluenceService confluenceService) {
        this.confluenceService = confluenceService;
    }

    /**
     * Get the latest RC artifacts for a project.
     * Example: /api/confluence/artifacts?search=12.2.0 Components Artifacts AT&T Secure Family
     */
    @GetMapping("/artifacts")
    public Map<String, Object> getArtifacts(@RequestParam String search) {
        return confluenceService.getLatestRcArtifacts(search);
    }
}
