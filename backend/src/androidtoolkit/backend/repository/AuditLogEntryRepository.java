package androidtoolkit.backend.repository;

import androidtoolkit.backend.entity.AuditLogEntry;
import androidtoolkit.domain.tenant.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AuditLogEntryRepository extends JpaRepository<AuditLogEntry, Long> {

    @Query("SELECT a FROM AuditLogEntry a WHERE a.tenantId = :tenantId " +
            "AND (:dateFrom IS NULL OR a.timestamp >= :dateFrom) " +
            "AND (:dateTo IS NULL OR a.timestamp <= :dateTo) " +
            "AND (:actorUserId IS NULL OR a.actorUserId = :actorUserId) " +
            "AND (:actionType IS NULL OR a.actionType = :actionType) " +
            "ORDER BY a.timestamp DESC")
    Page<AuditLogEntry> findByFilters(
            @Param("tenantId") Long tenantId,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            @Param("actorUserId") Long actorUserId,
            @Param("actionType") AuditAction actionType,
            Pageable pageable
    );
}
