package androidtoolkit.backend.crash;

import androidtoolkit.backend.entity.Project;
import androidtoolkit.backend.entity.Tenant;
import androidtoolkit.backend.repository.ProjectRepository;
import androidtoolkit.backend.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles persistence, querying, and aggregation of crash events.
 * Enforces multi-tenant data isolation via tenant filter.
 */
@Service
@Transactional
public class CrashHistoryService {

    private static final Logger log = LoggerFactory.getLogger(CrashHistoryService.class);

    private final CrashEventRepository crashEventRepository;
    private final ProjectRepository projectRepository;
    private final TenantRepository tenantRepository;

    public CrashHistoryService(CrashEventRepository crashEventRepository,
                               ProjectRepository projectRepository,
                               TenantRepository tenantRepository) {
        this.crashEventRepository = crashEventRepository;
        this.projectRepository = projectRepository;
        this.tenantRepository = tenantRepository;
    }

    /**
     * Persists a CrashEvent as a CrashEventEntity in the database.
     */
    public CrashEventEntity persist(CrashEvent event) {
        Project project = projectRepository.findById(event.projectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        Tenant tenant = tenantRepository.findById(event.tenantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        CrashEventEntity entity = new CrashEventEntity();
        entity.setTenant(tenant);
        entity.setProject(project);
        entity.setTimestamp(event.timestamp());
        entity.setDeviceSerial(event.deviceSerial());
        entity.setDeviceName(event.deviceName());
        entity.setPackageName(event.packageName());
        entity.setCrashType(event.crashType());
        entity.setSeverity(event.severity());
        entity.setStackTraceSnippet(event.stackTraceSnippet());
        entity.setCrashLogPath(event.crashLogPath());

        return crashEventRepository.save(entity);
    }

    /**
     * Finds crash events for a project with optional filters, paginated.
     */
    @Transactional(readOnly = true)
    public Page<CrashEventEntity> findByProject(Long projectId, CrashEventFilter filter, Pageable pageable) {
        Instant fromDate = filter.fromDate() != null
                ? filter.fromDate().atStartOfDay(ZoneOffset.UTC).toInstant()
                : null;
        Instant toDate = filter.toDate() != null
                ? filter.toDate().atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC)
                : null;

        return crashEventRepository.findByProjectWithFilters(
                projectId,
                filter.deviceSerial(),
                fromDate,
                toDate,
                filter.severity() != null ? filter.severity().name() : null,
                filter.crashType(),
                filter.acknowledged(),
                pageable
        );
    }

    /**
     * Finds a single crash event by ID.
     */
    @Transactional(readOnly = true)
    public Optional<CrashEventEntity> findById(Long id) {
        return crashEventRepository.findById(id);
    }

    /**
     * Acknowledges a crash event.
     */
    public CrashEventEntity acknowledge(Long crashId) {
        CrashEventEntity entity = crashEventRepository.findById(crashId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Crash event not found"));
        entity.setAcknowledged(true);
        entity.setAcknowledgedAt(Instant.now());
        return crashEventRepository.save(entity);
    }

    /**
     * Computes daily crash counts for a project within a date range, grouped by severity or device.
     * Returns data points for every day in the range, including days with zero crashes.
     */
    @Transactional(readOnly = true)
    public List<DailyCrashCount> getFrequencyByDay(Long projectId, LocalDate from, LocalDate to, GroupBy groupBy) {
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);

        List<DailyCrashCount> rawCounts;
        if (groupBy == GroupBy.DEVICE) {
            rawCounts = crashEventRepository.countDailyCrashesByDevice(projectId, fromInstant, toInstant);
        } else {
            rawCounts = crashEventRepository.countDailyCrashesBySeverity(projectId, fromInstant, toInstant);
        }

        // Fill in missing days with zero counts
        return fillMissingDays(rawCounts, from, to, groupBy);
    }

    /**
     * Fills in days with zero crashes to ensure every day in the range has a data point.
     */
    private List<DailyCrashCount> fillMissingDays(List<DailyCrashCount> rawCounts,
                                                   LocalDate from, LocalDate to, GroupBy groupBy) {
        // Collect all unique group keys from the data
        Set<String> groupKeys = rawCounts.stream()
                .map(DailyCrashCount::groupKey)
                .collect(Collectors.toSet());

        // If no data at all, provide at least one group key
        if (groupKeys.isEmpty()) {
            if (groupBy == GroupBy.SEVERITY) {
                groupKeys = Set.of("FATAL", "ANR", "WARNING");
            } else {
                return Collections.emptyList();
            }
        }

        // Build a map for quick lookup
        Map<String, Long> countMap = rawCounts.stream()
                .collect(Collectors.toMap(
                        dc -> dc.date() + "|" + dc.groupKey(),
                        DailyCrashCount::count,
                        Long::sum
                ));

        List<DailyCrashCount> result = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            for (String key : groupKeys) {
                long count = countMap.getOrDefault(date + "|" + key, 0L);
                result.add(new DailyCrashCount(date, key, count));
            }
        }

        return result;
    }

    /**
     * Grouping criteria for frequency aggregation.
     */
    public enum GroupBy {
        SEVERITY,
        DEVICE
    }
}
