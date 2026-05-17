package androidtoolkit.backend.crash;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrashPatternRepository extends JpaRepository<CrashPatternEntity, Long> {

    List<CrashPatternEntity> findByProjectIdAndEnabledTrue(Long projectId);

    List<CrashPatternEntity> findByProjectId(Long projectId);
}
