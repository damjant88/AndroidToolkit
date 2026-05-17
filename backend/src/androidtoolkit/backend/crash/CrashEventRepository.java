package androidtoolkit.backend.crash;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface CrashEventRepository extends JpaRepository<CrashEventEntity, Long> {

    @Query(value = "SELECT * FROM crash_events c WHERE c.project_id = :projectId " +
            "AND (:deviceSerial IS NULL OR c.device_serial = CAST(:deviceSerial AS VARCHAR)) " +
            "AND (CAST(:fromDate AS TIMESTAMP) IS NULL OR c.timestamp >= CAST(:fromDate AS TIMESTAMP)) " +
            "AND (CAST(:toDate AS TIMESTAMP) IS NULL OR c.timestamp <= CAST(:toDate AS TIMESTAMP)) " +
            "AND (CAST(:severity AS VARCHAR) IS NULL OR c.severity = CAST(:severity AS VARCHAR)) " +
            "AND (:crashType IS NULL OR c.crash_type = CAST(:crashType AS VARCHAR)) " +
            "AND (CAST(:acknowledged AS BOOLEAN) IS NULL OR c.acknowledged = CAST(:acknowledged AS BOOLEAN)) " +
            "ORDER BY c.timestamp DESC",
            countQuery = "SELECT COUNT(*) FROM crash_events c WHERE c.project_id = :projectId " +
            "AND (:deviceSerial IS NULL OR c.device_serial = CAST(:deviceSerial AS VARCHAR)) " +
            "AND (CAST(:fromDate AS TIMESTAMP) IS NULL OR c.timestamp >= CAST(:fromDate AS TIMESTAMP)) " +
            "AND (CAST(:toDate AS TIMESTAMP) IS NULL OR c.timestamp <= CAST(:toDate AS TIMESTAMP)) " +
            "AND (CAST(:severity AS VARCHAR) IS NULL OR c.severity = CAST(:severity AS VARCHAR)) " +
            "AND (:crashType IS NULL OR c.crash_type = CAST(:crashType AS VARCHAR)) " +
            "AND (CAST(:acknowledged AS BOOLEAN) IS NULL OR c.acknowledged = CAST(:acknowledged AS BOOLEAN))",
            nativeQuery = true)
    Page<CrashEventEntity> findByProjectWithFilters(
            @Param("projectId") Long projectId,
            @Param("deviceSerial") String deviceSerial,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            @Param("severity") String severity,
            @Param("crashType") String crashType,
            @Param("acknowledged") Boolean acknowledged,
            Pageable pageable
    );

    @Query("SELECT new androidtoolkit.backend.crash.DailyCrashCount(" +
            "CAST(c.timestamp AS LocalDate), CAST(c.severity AS string), COUNT(c)) " +
            "FROM CrashEventEntity c " +
            "WHERE c.project.id = :projectId " +
            "AND c.timestamp >= :fromDate AND c.timestamp <= :toDate " +
            "GROUP BY CAST(c.timestamp AS LocalDate), c.severity " +
            "ORDER BY CAST(c.timestamp AS LocalDate)")
    List<DailyCrashCount> countDailyCrashesBySeverity(
            @Param("projectId") Long projectId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate
    );

    @Query("SELECT new androidtoolkit.backend.crash.DailyCrashCount(" +
            "CAST(c.timestamp AS LocalDate), c.deviceSerial, COUNT(c)) " +
            "FROM CrashEventEntity c " +
            "WHERE c.project.id = :projectId " +
            "AND c.timestamp >= :fromDate AND c.timestamp <= :toDate " +
            "GROUP BY CAST(c.timestamp AS LocalDate), c.deviceSerial " +
            "ORDER BY CAST(c.timestamp AS LocalDate)")
    List<DailyCrashCount> countDailyCrashesByDevice(
            @Param("projectId") Long projectId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate
    );
}
