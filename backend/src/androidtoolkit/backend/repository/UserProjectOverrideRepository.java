package androidtoolkit.backend.repository;

import androidtoolkit.backend.entity.Project;
import androidtoolkit.backend.entity.User;
import androidtoolkit.backend.entity.UserProjectOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserProjectOverrideRepository extends JpaRepository<UserProjectOverride, Long> {
    Optional<UserProjectOverride> findByUserAndProject(User user, Project project);
    void deleteAllByProject(Project project);
    List<UserProjectOverride> findAllByUser(User user);
}
