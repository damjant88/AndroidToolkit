package androidtoolkit.backend.service;

import androidtoolkit.backend.entity.Project;
import androidtoolkit.backend.entity.Tenant;
import androidtoolkit.backend.entity.User;
import androidtoolkit.backend.repository.ProjectRepository;
import androidtoolkit.backend.repository.TenantRepository;
import androidtoolkit.backend.repository.UserRepository;
import androidtoolkit.domain.tenant.SubscriptionTier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class MigrationService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ObjectStorageService objectStorageService;

    public MigrationService(TenantRepository tenantRepository, UserRepository userRepository,
                            ProjectRepository projectRepository, ObjectStorageService objectStorageService) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.objectStorageService = objectStorageService;
    }

    @Transactional
    public MigrationResult importStandaloneArchive(InputStream archive, String tenantName) {
        Tenant tenant = new Tenant(tenantName, SubscriptionTier.FREE);
        tenantRepository.save(tenant);

        int projectsImported = 0;
        int usersImported = 0;
        int logsImported = 0;
        List<MigrationConflict> conflicts = new ArrayList<>();

        try (ZipInputStream zis = new ZipInputStream(archive)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.startsWith("projects/")) {
                    // Import project metadata
                    String projectName = name.replace("projects/", "").replace(".json", "");
                    if (projectRepository.existsByName(projectName)) {
                        conflicts.add(new MigrationConflict("project", projectName, "Already exists"));
                    } else {
                        Project project = new Project(projectName, "", "");
                        project.setTenant(tenant);
                        projectRepository.save(project);
                        projectsImported++;
                    }
                } else if (name.startsWith("users/")) {
                    String username = name.replace("users/", "").replace(".json", "");
                    if (userRepository.existsByUsername(username)) {
                        conflicts.add(new MigrationConflict("user", username, "Already exists"));
                    } else {
                        usersImported++;
                    }
                } else if (name.startsWith("logs/")) {
                    // Upload log files to object storage
                    objectStorageService.upload(tenant.getId(), "migrated", name, zis, entry.getSize());
                    logsImported++;
                }
                zis.closeEntry();
            }
        } catch (Exception e) {
            conflicts.add(new MigrationConflict("archive", "parse", e.getMessage()));
        }

        return new MigrationResult(projectsImported, usersImported, logsImported, conflicts);
    }

    public record MigrationResult(int projectsImported, int usersImported, int logsImported,
                                   List<MigrationConflict> conflicts) {}

    public record MigrationConflict(String type, String identifier, String reason) {}
}
