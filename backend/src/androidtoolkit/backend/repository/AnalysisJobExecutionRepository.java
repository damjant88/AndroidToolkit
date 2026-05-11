package androidtoolkit.backend.repository;

import androidtoolkit.backend.entity.AnalysisJobExecution;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnalysisJobExecutionRepository extends JpaRepository<AnalysisJobExecution, Long> {
    List<AnalysisJobExecution> findByOrderByStartedAtDesc(Pageable pageable);
}
