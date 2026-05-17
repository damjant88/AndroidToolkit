package androidtoolkit.backend.controller;

import androidtoolkit.backend.crash.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for per-project alert configuration management.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/alert-config")
public class AlertConfigController {

    private final AlertConfigurationService alertConfigurationService;

    public AlertConfigController(AlertConfigurationService alertConfigurationService) {
        this.alertConfigurationService = alertConfigurationService;
    }

    /**
     * GET /api/projects/{projectId}/alert-config
     * Returns the alert configuration for the project (or defaults if none exists).
     */
    @GetMapping
    public AlertConfigurationEntity getAlertConfig(@PathVariable Long projectId) {
        return alertConfigurationService.getForProject(projectId);
    }

    /**
     * PUT /api/projects/{projectId}/alert-config
     * Updates the alert configuration for the project.
     */
    @PutMapping
    public AlertConfigurationEntity updateAlertConfig(
            @PathVariable Long projectId,
            @RequestBody AlertConfigurationEntity request) {
        return alertConfigurationService.update(projectId, request);
    }

    /**
     * GET /api/projects/{projectId}/alert-config/patterns
     * Returns all active crash patterns for the project.
     */
    @GetMapping("/patterns")
    public List<CrashPatternEntity> getPatterns(@PathVariable Long projectId) {
        return alertConfigurationService.getActivePatterns(projectId);
    }

    /**
     * POST /api/projects/{projectId}/alert-config/patterns
     * Adds a custom crash pattern for the project.
     */
    @PostMapping("/patterns")
    public CrashPatternEntity addPattern(
            @PathVariable Long projectId,
            @RequestBody PatternRequest request) {
        return alertConfigurationService.addCustomPattern(projectId, request.regex(), request.severity());
    }

    /**
     * DELETE /api/projects/{projectId}/alert-config/patterns/{patternId}
     * Removes a custom crash pattern.
     */
    @DeleteMapping("/patterns/{patternId}")
    public ResponseEntity<Void> removePattern(
            @PathVariable Long projectId,
            @PathVariable Long patternId) {
        alertConfigurationService.removeCustomPattern(projectId, patternId);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/projects/{projectId}/alert-config/patterns/{patternId}
     * Updates an existing custom crash pattern.
     */
    @PutMapping("/patterns/{patternId}")
    public ResponseEntity<CrashPatternEntity> updatePattern(
            @PathVariable Long projectId,
            @PathVariable Long patternId,
            @RequestBody PatternRequest request) {
        // Remove old and add new
        alertConfigurationService.removeCustomPattern(projectId, patternId);
        CrashPatternEntity newPattern = alertConfigurationService.addCustomPattern(
                projectId, request.regex(), request.severity());
        return ResponseEntity.ok(newPattern);
    }

    /**
     * POST /api/projects/{projectId}/alert-config/validate-regex
     * Validates a regex pattern without saving it.
     */
    @PostMapping("/validate-regex")
    public ValidationResult validateRegex(@RequestBody String regex) {
        return alertConfigurationService.validateRegex(regex);
    }

    /**
     * Request body for adding/updating crash patterns.
     */
    public record PatternRequest(String regex, SeverityLevel severity) {}
}
