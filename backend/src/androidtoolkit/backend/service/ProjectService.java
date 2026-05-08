package androidtoolkit.backend.service;

import androidtoolkit.backend.dto.OverrideRequest;
import androidtoolkit.backend.dto.OverrideResponse;
import androidtoolkit.backend.dto.ProjectRequest;
import androidtoolkit.backend.dto.ProjectResponse;
import androidtoolkit.backend.dto.ResolvedProjectResponse;
import androidtoolkit.backend.entity.Project;
import androidtoolkit.backend.entity.User;
import androidtoolkit.backend.entity.UserProjectOverride;
import androidtoolkit.backend.repository.ProjectRepository;
import androidtoolkit.backend.repository.UserProjectOverrideRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing Project entities with CRUD operations.
 * Handles validation, duplicate name detection, and cascade deletion of overrides.
 */
@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserProjectOverrideRepository overrideRepository;

    public ProjectService(ProjectRepository projectRepository,
                          UserProjectOverrideRepository overrideRepository) {
        this.projectRepository = projectRepository;
        this.overrideRepository = overrideRepository;
    }

    public List<ProjectResponse> findAll() {
        return projectRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public ProjectResponse findById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        return toResponse(project);
    }

    public ProjectResponse create(ProjectRequest request) {
        validateRequest(request);
        if (projectRepository.existsByName(request.name().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Project name already exists");
        }
        Project project = new Project();
        project.setName(request.name().trim());
        project.setRemoteApkLocation(request.remoteApkLocation().trim());
        project.setLocalApkFolder(request.localApkFolder().trim());
        return toResponse(projectRepository.save(project));
    }

    public ProjectResponse update(Long id, ProjectRequest request) {
        validateRequest(request);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        // Check name uniqueness (excluding current project)
        projectRepository.findByName(request.name().trim()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Project name already exists");
            }
        });
        project.setName(request.name().trim());
        project.setRemoteApkLocation(request.remoteApkLocation().trim());
        project.setLocalApkFolder(request.localApkFolder().trim());
        return toResponse(projectRepository.save(project));
    }

    public void delete(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        overrideRepository.deleteAllByProject(project);
        projectRepository.delete(project);
    }

    public OverrideResponse getOverride(Long projectId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        UserProjectOverride override = overrideRepository.findByUserAndProject(user, project)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No override found"));
        return toOverrideResponse(override);
    }

    public OverrideResponse setOverride(Long projectId, User user, OverrideRequest request) {
        if (request.localApkFolder() == null || request.localApkFolder().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Local APK folder is required");
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        UserProjectOverride override = overrideRepository.findByUserAndProject(user, project)
                .orElseGet(() -> {
                    UserProjectOverride o = new UserProjectOverride();
                    o.setUser(user);
                    o.setProject(project);
                    return o;
                });
        override.setLocalApkFolder(request.localApkFolder().trim());
        return toOverrideResponse(overrideRepository.save(override));
    }

    public void deleteOverride(Long projectId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        overrideRepository.findByUserAndProject(user, project)
                .ifPresent(overrideRepository::delete);
    }

    public ResolvedProjectResponse getResolved(Long projectId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        Optional<UserProjectOverride> override = overrideRepository.findByUserAndProject(user, project);
        String resolvedLocalPath = override.map(UserProjectOverride::getLocalApkFolder)
                .orElse(project.getLocalApkFolder());
        return new ResolvedProjectResponse(
                project.getId(), project.getName(), project.getRemoteApkLocation(),
                resolvedLocalPath, override.isPresent()
        );
    }

    private void validateRequest(ProjectRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project name is required");
        }
        if (request.remoteApkLocation() == null || request.remoteApkLocation().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Remote APK location is required");
        }
        if (request.localApkFolder() == null || request.localApkFolder().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Local APK folder is required");
        }
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getRemoteApkLocation(),
                project.getLocalApkFolder(),
                project.getCreatedAt()
        );
    }

    private OverrideResponse toOverrideResponse(UserProjectOverride override) {
        return new OverrideResponse(
                override.getId(),
                override.getProject().getId(),
                override.getLocalApkFolder()
        );
    }
}
