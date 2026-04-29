package androidtoolkit.backend.job;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class Job {

    public enum Status { PENDING, RUNNING, COMPLETED, FAILED }
    public enum Type { INSTALL, UNINSTALL }

    private final String id;
    private final Type type;
    private final String buildName;
    private final List<String> targetSerials;
    private final Instant startedAt;
    private volatile Status status;
    private volatile Instant completedAt;
    private final Map<String, DeviceResult> deviceResults = new ConcurrentHashMap<>();

    public Job(Type type, String buildName, List<String> targetSerials) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.type = type;
        this.buildName = buildName;
        this.targetSerials = new ArrayList<>(targetSerials);
        this.startedAt = Instant.now();
        this.status = Status.PENDING;
    }

    public String getId() { return id; }
    public Type getType() { return type; }
    public String getBuildName() { return buildName; }
    public List<String> getTargetSerials() { return new ArrayList<>(targetSerials); }
    public Instant getStartedAt() { return startedAt; }
    public Status getStatus() { return status; }
    public Instant getCompletedAt() { return completedAt; }
    public Map<String, DeviceResult> getDeviceResults() { return new java.util.HashMap<>(deviceResults); }

    public void setStatus(Status status) { this.status = status; }

    public void markRunning() { this.status = Status.RUNNING; }

    public void addDeviceResult(String serial, boolean success, String message) {
        deviceResults.put(serial, new DeviceResult(serial, success, message));
        // Check if all devices are done
        if (deviceResults.size() == targetSerials.size()) {
            boolean allSuccess = deviceResults.values().stream().allMatch(DeviceResult::isSuccess);
            this.status = allSuccess ? Status.COMPLETED : Status.FAILED;
            this.completedAt = Instant.now();
        }
    }

    public int getCompletedCount() { return deviceResults.size(); }
    public int getTotalCount() { return targetSerials.size(); }
    public int getProgressPercent() {
        if (targetSerials.isEmpty()) return 100;
        return (deviceResults.size() * 100) / targetSerials.size();
    }

    public static class DeviceResult {
        private final String serial;
        private final boolean success;
        private final String message;

        public DeviceResult(String serial, boolean success, String message) {
            this.serial = serial;
            this.success = success;
            this.message = message;
        }

        public String getSerial() { return serial; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}
