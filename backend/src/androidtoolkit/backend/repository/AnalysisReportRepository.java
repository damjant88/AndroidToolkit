package androidtoolkit.backend.repository;

import androidtoolkit.backend.entity.AnalysisReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AnalysisReportRepository extends JpaRepository<AnalysisReport, Long> {
    List<AnalysisReport> findByProjectIdAndReportDateBetween(Long projectId, LocalDate from, LocalDate to);
    Optional<AnalysisReport> findFirstByProjectIdOrderByReportDateDesc(Long projectId);
}
