package androidtoolkit.backend.controller;

import androidtoolkit.backend.entity.Project;
import androidtoolkit.backend.repository.ProjectRepository;
import androidtoolkit.backend.service.ConfluenceService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/confluence")
public class ConfluenceController {

    private final ConfluenceService confluenceService;
    private final ProjectRepository projectRepository;

    public ConfluenceController(ConfluenceService confluenceService, ProjectRepository projectRepository) {
        this.confluenceService = confluenceService;
        this.projectRepository = projectRepository;
    }

    /**
     * Get the latest RC artifacts for a project.
     * Supports: projectId (reads config from DB), parentId, pageId, or search.
     */
    @GetMapping("/artifacts")
    public Map<String, Object> getArtifacts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String pageId,
            @RequestParam(required = false) String parentId,
            @RequestParam(required = false) Long projectId) {

        // If projectId is provided, read Confluence config from the project in DB
        if (projectId != null) {
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project == null) {
                return Map.of("error", "Project not found with id: " + projectId);
            }
            // Direct page ID takes priority
            String directPageId = project.getConfluenceArtifactsPageId();
            if (directPageId != null && !directPageId.isBlank()) {
                return confluenceService.getArtifactsByPageId(directPageId.trim());
            }
            // Fall back to parent page auto-discovery
            String parentPageId = project.getConfluenceParentPageId();
            if (parentPageId != null && !parentPageId.isBlank()) {
                return confluenceService.getLatestChildArtifacts(parentPageId.trim());
            }
            return Map.of("error", "No Confluence page configured for project: " + project.getName());
        }

        if (parentId != null && !parentId.isBlank()) {
            return confluenceService.getLatestChildArtifacts(parentId);
        }
        if (pageId != null && !pageId.isBlank()) {
            return confluenceService.getArtifactsByPageId(pageId);
        }
        if (search != null && !search.isBlank()) {
            return confluenceService.getLatestRcArtifacts(search);
        }
        return Map.of("error", "Either 'projectId', 'parentId', 'pageId', or 'search' parameter is required");
    }
}
