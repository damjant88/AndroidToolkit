package androidtoolkit.backend.controller;

import androidtoolkit.backend.crash.CrashHistoryService;
import androidtoolkit.backend.crash.DailyCrashCount;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for crash frequency trend data.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/crash-trends")
public class CrashTrendController {

    private final CrashHistoryService crashHistoryService;

    public CrashTrendController(CrashHistoryService crashHistoryService) {
        this.crashHistoryService = crashHistoryService;
    }

    /**
     * GET /api/projects/{projectId}/crash-trends
     * Returns daily crash frequency data for the specified date range and grouping.
     * Supports preset ranges: 7, 14, 30 days.
     */
    @GetMapping
    public List<DailyCrashCount> getCrashTrends(
            @PathVariable Long projectId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(defaultValue = "7") int presetDays,
            @RequestParam(defaultValue = "SEVERITY") CrashHistoryService.GroupBy groupBy) {

        LocalDate to = toDate != null ? toDate : LocalDate.now();
        LocalDate from = fromDate != null ? fromDate : to.minusDays(presetDays);

        return crashHistoryService.getFrequencyByDay(projectId, from, to, groupBy);
    }
}
