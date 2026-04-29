package androidtoolkit.backend.job;

import androidtoolkit.backend.service.DeviceMonitorService;
import androidtoolkit.service.CommandExecutor;
import androidtoolkit.service.DeviceGateway;
import androidtoolkit.app.AppServices;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages background install/uninstall jobs.
 * Jobs run in parallel across devices and broadcast progress via WebSocket.
 */
@Service
public class JobService {

    private final CommandExecutor commandExecutor;
    private final DeviceGateway deviceGateway;
    private final SimpMessagingTemplate messagingTemplate;
    private final DeviceMonitorService deviceMonitorService;
    private final Map<String, Job> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public JobService(AppServices appServices, SimpMessagingTemplate messagingTemplate, DeviceMonitorService deviceMonitorService) {
        this.commandExecutor = appServices.commandExecutor();
        this.deviceGateway = appServices.deviceGateway();
        this.messagingTemplate = messagingTemplate;
        this.deviceMonitorService = deviceMonitorService;
    }

    public Job startInstallJob(String apkPath, List<String> serials) {
        String buildName = new File(apkPath).getName();
        Job job = new Job(Job.Type.INSTALL, buildName, serials);
        jobs.put(job.getId(), job);
        job.markRunning();
        broadcastJobUpdate(job);

        for (String serial : serials) {
            executor.submit(() -> {
                String output = commandExecutor.runCommand("adb -s " + serial + " install \"" + apkPath + "\"");
                boolean success = output.contains("Success");
                job.addDeviceResult(serial, success, success ? "Installed" : output);
                broadcastJobUpdate(job);
                if (job.getStatus() != Job.Status.RUNNING) {
                    deviceMonitorService.broadcastDeviceUpdate();
                }
            });
        }

        return job;
    }

    public Job startUninstallJob(List<String> serials) {
        Job job = new Job(Job.Type.UNINSTALL, "", serials);
        jobs.put(job.getId(), job);
        job.markRunning();
        broadcastJobUpdate(job);

        for (String serial : serials) {
            executor.submit(() -> {
                String packageName = deviceGateway.getSafePathPackage(serial);
                if (packageName == null || packageName.isBlank()) {
                    job.addDeviceResult(serial, false, "No supported package found");
                } else {
                    boolean success = deviceGateway.uninstallApp(serial, packageName);
                    job.addDeviceResult(serial, success, success ? "Uninstalled" : "Failed");
                }
                broadcastJobUpdate(job);
                if (job.getStatus() != Job.Status.RUNNING) {
                    deviceMonitorService.broadcastDeviceUpdate();
                }
            });
        }

        return job;
    }

    public Job getJob(String jobId) {
        return jobs.get(jobId);
    }

    public List<Job> getRecentJobs() {
        List<Job> all = new ArrayList<>(jobs.values());
        all.sort((a, b) -> b.getStartedAt().compareTo(a.getStartedAt()));
        return all.size() > 20 ? all.subList(0, 20) : all;
    }

    private void broadcastJobUpdate(Job job) {
        messagingTemplate.convertAndSend("/topic/jobs/" + job.getId(), job);
    }
}
