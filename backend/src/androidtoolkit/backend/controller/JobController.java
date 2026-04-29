package androidtoolkit.backend.controller;

import androidtoolkit.backend.job.Job;
import androidtoolkit.backend.job.JobService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static androidtoolkit.backend.validation.InputValidator.validateFilePath;
import static androidtoolkit.backend.validation.InputValidator.validateSerial;

/**
 * REST API for background jobs.
 *
 *   POST /api/jobs/install   → start install job (returns job ID)
 *   POST /api/jobs/uninstall → start uninstall job (returns job ID)
 *   GET  /api/jobs/{id}      → get job status and progress
 *   GET  /api/jobs           → list recent jobs
 *
 * Subscribe to WebSocket /topic/jobs/{id} for real-time progress updates.
 */
@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/install")
    public Map<String, Object> startInstall(@RequestBody Map<String, Object> body) {
        String apkPath = (String) body.getOrDefault("apkPath", "");
        List<String> serials = (List<String>) body.getOrDefault("serials", List.of());

        validateFilePath(apkPath);
        serials.forEach(s -> validateSerial(s));

        Job job = jobService.startInstallJob(apkPath, serials);
        return Map.of(
                "jobId", job.getId(),
                "status", job.getStatus().name(),
                "message", "Install job started on " + serials.size() + " device(s)"
        );
    }

    @PostMapping("/uninstall")
    public Map<String, Object> startUninstall(@RequestBody Map<String, Object> body) {
        List<String> serials = (List<String>) body.getOrDefault("serials", List.of());
        serials.forEach(s -> validateSerial(s));

        Job job = jobService.startUninstallJob(serials);
        return Map.of(
                "jobId", job.getId(),
                "status", job.getStatus().name(),
                "message", "Uninstall job started on " + serials.size() + " device(s)"
        );
    }

    @GetMapping("/{jobId}")
    public Job getJob(@PathVariable String jobId) {
        Job job = jobService.getJob(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }
        return job;
    }

    @GetMapping
    public List<Job> getRecentJobs() {
        return jobService.getRecentJobs();
    }
}
