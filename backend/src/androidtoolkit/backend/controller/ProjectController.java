package androidtoolkit.backend.controller;

import androidtoolkit.backend.dto.OverrideRequest;
import androidtoolkit.backend.dto.OverrideResponse;
import androidtoolkit.backend.dto.ProjectRequest;
import androidtoolkit.backend.dto.ProjectResponse;
import androidtoolkit.backend.dto.ResolvedProjectResponse;
import androidtoolkit.backend.entity.User;
import androidtoolkit.backend.repository.UserRepository;
import androidtoolkit.backend.service.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final UserRepository userRepository;

    public ProjectController(ProjectService projectService, UserRepository userRepository) {
        this.projectService = projectService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<ProjectResponse> listProjects() {
        requireAuthenticated();
        return projectService.findAll();
    }

    @GetMapping("/{id}")
    public ProjectResponse getProject(@PathVariable Long id) {
        requireAuthenticated();
        return projectService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@RequestBody ProjectRequest request) {
        requireAdmin();
        return projectService.create(request);
    }

    @PutMapping("/{id}")
    public ProjectResponse updateProject(@PathVariable Long id, @RequestBody ProjectRequest request) {
        requireAdmin();
        return projectService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@PathVariable Long id) {
        requireAdmin();
        projectService.delete(id);
    }

    @GetMapping("/{id}/overrides/me")
    public OverrideResponse getMyOverride(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        return projectService.getOverride(id, user);
    }

    @PutMapping("/{id}/overrides/me")
    public OverrideResponse setMyOverride(@PathVariable Long id, @RequestBody OverrideRequest request) {
        User user = getAuthenticatedUser();
        return projectService.setOverride(id, user, request);
    }

    @DeleteMapping("/{id}/overrides/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyOverride(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        projectService.deleteOverride(id, user);
    }

    @GetMapping("/{id}/resolved")
    public ResolvedProjectResponse getResolvedProject(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        return projectService.getResolved(id, user);
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            return (User) auth.getPrincipal();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
    }

    private void requireAuthenticated() {
        getAuthenticatedUser();
    }

    private void requireAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        if (user.getRole() != User.Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}
