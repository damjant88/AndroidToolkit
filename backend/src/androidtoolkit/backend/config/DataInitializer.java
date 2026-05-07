package androidtoolkit.backend.config;

import androidtoolkit.backend.entity.User;
import androidtoolkit.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedDefaultAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByUsername("admin")) {
                User admin = new User("admin", "admin@androidtoolkit.local", passwordEncoder.encode("admin123"));
                admin.setRole(User.Role.ADMIN);
                admin.setTier(User.Tier.ADVANCED);
                userRepository.save(admin);
            }
        };
    }
}
