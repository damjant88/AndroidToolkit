package androidtoolkit.backend.crash;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlertConfigurationRepository extends JpaRepository<AlertConfigurationEntity, Long> {

    Optional<AlertConfigurationEntity> findByProjectId(Long projectId);
}
