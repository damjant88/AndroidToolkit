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

    /** Project definitions with their associated packages */
    private static final List<Map<String, Object>> DEFAULT_PROJECTS = List.of(
            Map.of("name", "SafePath", "packages", List.of(
                    "com.smithmicro.safepath.family", "com.smithmicro.safepath.family.child")),
            Map.of("name", "Secure Family", "packages", List.of(
                    "com.smithmicro.att.securefamily", "com.wavemarket.waplauncher", "com.att.securefamilycompanion")),
            Map.of("name", "Safe&Found", "packages", List.of(
                    "com.smithmicro.sprint.safeandfound.test", "com.sprint.safefound")),
            Map.of("name", "Family Mode", "packages", List.of(
                    "com.smithmicro.tmobile.familymode.test", "com.tmobile.familycontrols")),
            Map.of("name", "CCI", "packages", List.of(
                    "com.smithmicro.cci.test", "com.smithmicro.safepath.family.light", "com.smithmicro.safepath.family.speakeasy")),
            Map.of("name", "Orange", "packages", List.of(
                    "com.smithmicro.orangespain.test", "com.orange.es.TuYo")),
            Map.of("name", "Dish", "packages", List.of(
                    "com.smithmicro.safepath.dish.test", "com.smithmicro.safepath.dish.kid.test"))
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
                    projectRepository.save(project);
                    log.info("Created default project: {}", name);
                }
            }
        };
    }
}
