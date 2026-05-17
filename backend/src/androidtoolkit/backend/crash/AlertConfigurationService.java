package androidtoolkit.backend.crash;

import androidtoolkit.backend.entity.Project;
import androidtoolkit.backend.repository.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Manages per-project crash detection alert configuration settings.
 * Provides default configuration when no explicit config exists,
 * validates buffer sizes and regex patterns, and manages custom crash patterns.
 */
@Service
@Transactional
public class AlertConfigurationService {

    private static final int MIN_BUFFER_SIZE = 50;
    private static final int MAX_BUFFER_SIZE = 2000;
    private static final int DEFAULT_BUFFER_SIZE = 500;

    private final AlertConfigurationRepository configRepository;
    private final CrashPatternRepository patternRepository;
    private final ProjectRepository projectRepository;

    public AlertConfigurationService(AlertConfigurationRepository configRepository,
                                     CrashPatternRepository patternRepository,
                                     ProjectRepository projectRepository) {
        this.configRepository = configRepository;
        this.patternRepository = patternRepository;
        this.projectRepository = projectRepository;
    }

    /**
     * Retrieves the alert configuration for a project.
     * Returns a default configuration if no explicit config exists.
     */
    public AlertConfigurationEntity getForProject(Long projectId) {
        return configRepository.findByProjectId(projectId)
                .orElseGet(() -> buildDefaultConfiguration(projectId));
    }

    /**
     * Updates the alert configuration for a project.
     * Creates a new configuration entity if none exists.
     * Validates buffer size before saving.
     */
    public AlertConfigurationEntity update(Long projectId, AlertConfigurationEntity request) {
        ValidationResult bufferValidation = validateBufferSize(request.getBufferSize());
        if (!bufferValidation.valid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, bufferValidation.message());
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        AlertConfigurationEntity config = configRepository.findByProjectId(projectId)
                .orElseGet(() -> {
                    AlertConfigurationEntity newConfig = new AlertConfigurationEntity();
                    newConfig.setProject(project);
                    newConfig.setTenant(project.getTenant());
                    return newConfig;
                });

        config.setCrashDetectionEnabled(request.isCrashDetectionEnabled());
        config.setAutoPullEnabled(request.isAutoPullEnabled());
        config.setNotificationPreference(request.getNotificationPreference());
        config.setBufferSize(request.getBufferSize());

        return configRepository.save(config);
    }

    /**
     * Adds a custom crash pattern for a project.
     * Validates the regex before saving; rejects with a descriptive error if invalid.
     */
    public CrashPatternEntity addCustomPattern(Long projectId, String regex, SeverityLevel severity) {
        ValidationResult regexValidation = validateRegex(regex);
        if (!regexValidation.valid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, regexValidation.message());
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        CrashPatternEntity pattern = new CrashPatternEntity();
        pattern.setProject(project);
        pattern.setRegex(regex);
        pattern.setSeverity(severity);
        pattern.setDefault(false);
        pattern.setEnabled(true);
        pattern.setCreatedAt(Instant.now());

        return patternRepository.save(pattern);
    }

    /**
     * Removes a custom crash pattern by ID for a given project.
     * Only non-default patterns can be removed.
     */
    public void removeCustomPattern(Long projectId, Long patternId) {
        CrashPatternEntity pattern = patternRepository.findById(patternId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pattern not found"));

        if (pattern.getProject() == null || !pattern.getProject().getId().equals(projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pattern not found for this project");
        }

        if (pattern.isDefault()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove default patterns");
        }

        patternRepository.delete(pattern);
    }

    /**
     * Returns all active (enabled) crash patterns for a project.
     */
    public List<CrashPatternEntity> getActivePatterns(Long projectId) {
        return patternRepository.findByProjectIdAndEnabledTrue(projectId);
    }

    /**
     * Validates that a regex string compiles without errors.
     * Returns a ValidationResult indicating success or failure with a descriptive error.
     */
    public ValidationResult validateRegex(String regex) {
        if (regex == null || regex.isBlank()) {
            return ValidationResult.failure("Regex pattern cannot be empty");
        }
        try {
            Pattern.compile(regex);
            return ValidationResult.success();
        } catch (PatternSyntaxException e) {
            return ValidationResult.failure("Invalid regex pattern: " + e.getDescription()
                    + " near index " + e.getIndex());
        }
    }

    /**
     * Validates that a buffer size is within the allowed range [50, 2000] inclusive.
     */
    public ValidationResult validateBufferSize(int size) {
        if (size < MIN_BUFFER_SIZE || size > MAX_BUFFER_SIZE) {
            return ValidationResult.failure(
                    "Buffer size must be between " + MIN_BUFFER_SIZE + " and " + MAX_BUFFER_SIZE
                            + " inclusive, but was " + size);
        }
        return ValidationResult.success();
    }

    private AlertConfigurationEntity buildDefaultConfiguration(Long projectId) {
        AlertConfigurationEntity defaultConfig = new AlertConfigurationEntity();
        defaultConfig.setCrashDetectionEnabled(true);
        defaultConfig.setAutoPullEnabled(false);
        defaultConfig.setNotificationPreference(NotificationPreference.BROWSER);
        defaultConfig.setBufferSize(DEFAULT_BUFFER_SIZE);
        return defaultConfig;
    }
}
