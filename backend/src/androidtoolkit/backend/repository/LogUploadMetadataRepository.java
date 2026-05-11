package androidtoolkit.backend.repository;

import androidtoolkit.backend.entity.LogUploadMetadata;
import androidtoolkit.backend.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface LogUploadMetadataRepository extends JpaRepository<LogUploadMetadata, Long> {
    List<LogUploadMetadata> findByUploadedAtBetween(Instant from, Instant to);
    List<LogUploadMetadata> findByProjectAndUploadedAtBetween(Project project, Instant from, Instant to);
}
