package androidtoolkit.backend.config;

import androidtoolkit.backend.entity.Project;
import androidtoolkit.backend.entity.User;
import androidtoolkit.backend.repository.ProjectRepository;
import androidtoolkit.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    /** Project definitions with their associated packages and Confluence page IDs */
    private static final List<Map<String, Object>> DEFAULT_PROJECTS = List.of(
            Map.of("name", "SafePath", "confluenceParentPageId", "40793397", "confluenceArtifactsPageId", "168919041",
                    "packages", List.of("com.smithmicro.safepath.family", "com.smithmicro.safepath.family.child")),
            Map.of("name", "Secure Family", "confluenceParentPageId", "40803846", "confluenceArtifactsPageId", "101875725",
                    "packages", List.of("com.smithmicro.att.securefamily", "com.wavemarket.waplauncher", "com.att.securefamilycompanion")),
            Map.of("name", "Safe&Found", "confluenceParentPageId", "40802189", "confluenceArtifactsPageId", "40795173",
                    "packages", List.of("com.smithmicro.sprint.safeandfound.test", "com.sprint.safefound")),
            Map.of("name", "Family Mode", "confluenceParentPageId", "40796086", "confluenceArtifactsPageId", "67698812",
                    "packages", List.of("com.smithmicro.tmobile.familymode.test", "com.tmobile.familycontrols")),
            Map.of("name", "CCI", "confluenceParentPageId", "40795593", "confluenceArtifactsPageId", "169738286",
                    "packages", List.of("com.smithmicro.cci.test", "com.smithmicro.safepath.family.light", "com.smithmicro.safepath.family.speakeasy")),
            Map.of("name", "Orange", "confluenceParentPageId", "40785617", "confluenceArtifactsPageId", "40795708",
                    "packages", List.of("com.smithmicro.orangespain.test", "com.orange.es.TuYo")),
            Map.of("name", "Dish", "confluenceParentPageId", "40787434", "confluenceArtifactsPageId", "40789537",
                    "packages", List.of("com.smithmicro.safepath.dish.test", "com.smithmicro.safepath.dish.kid.test")),
            Map.of("name", "SPC", "confluenceParentPageId", "87392329", "confluenceArtifactsPageId", "180682769",
                    "packages", List.of("com.smithmicro.safepath.connect"))
    );

    @Bean
    CommandLineRunner seedDefaultAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByUsername("admin")) {
                User admin = new User("admin", "admin@androidtoolkit.local", passwordEncoder.encode("admin123"));
                admin.setRole(User.Role.ADMIN);
                admin.setTier(User.Tier.ADVANCED);
                userRepository.save(admin);
            } else {
                userRepository.findByUsername("admin").ifPresent(admin -> {
                    boolean changed = false;
                    if (admin.getRole() != User.Role.ADMIN) {
                        admin.setRole(User.Role.ADMIN);
                        changed = true;
                    }
                    if (admin.getTier() != User.Tier.ADVANCED) {
                        admin.setTier(User.Tier.ADVANCED);
                        changed = true;
                    }
                    if (changed) userRepository.save(admin);
                });
            }
        };
    }

    @Bean
    CommandLineRunner seedDefaultProjects(ProjectRepository projectRepository) {
        return args -> {
            for (Map<String, Object> def : DEFAULT_PROJECTS) {
                String name = (String) def.get("name");
                if (!projectRepository.existsByName(name)) {
                    Project project = new Project(name, "", "", "");
                    String parentPageId = (String) def.getOrDefault("confluenceParentPageId", "");
                    String artifactsPageId = (String) def.getOrDefault("confluenceArtifactsPageId", "");
                    project.setConfluenceParentPageId(parentPageId);
                    project.setConfluenceArtifactsPageId(artifactsPageId);
                    projectRepository.save(project);
                    log.info("Created default project: {} (Confluence parentId={}, artifactsId={})", name, parentPageId, artifactsPageId);
                } else {
                    // Update Confluence page IDs for existing projects if they are empty
                    projectRepository.findByName(name).ifPresent(project -> {
                        boolean changed = false;
                        String parentPageId = (String) def.getOrDefault("confluenceParentPageId", "");
                        String artifactsPageId = (String) def.getOrDefault("confluenceArtifactsPageId", "");
                        if ((project.getConfluenceParentPageId() == null || project.getConfluenceParentPageId().isBlank())
                                && !parentPageId.isBlank()) {
                            project.setConfluenceParentPageId(parentPageId);
                            changed = true;
                        }
                        if ((project.getConfluenceArtifactsPageId() == null || project.getConfluenceArtifactsPageId().isBlank())
                                && !artifactsPageId.isBlank()) {
                            project.setConfluenceArtifactsPageId(artifactsPageId);
                            changed = true;
                        }
                        if (changed) {
                            projectRepository.save(project);
                            log.info("Updated Confluence page IDs for project: {}", name);
                        }
                    });
                }
            }
        };
    }
}
