package androidtoolkit.app;

import java.util.ArrayList;
import java.util.List;

public class PermissionUpdateResult {

    private final int appliedCount;
    private final int requestedCount;
    private final List<String> failures;

    public PermissionUpdateResult(int appliedCount, int requestedCount, List<String> failures) {
        this.appliedCount = appliedCount;
        this.requestedCount = requestedCount;
        this.failures = new ArrayList<>(failures);
    }

    public int getAppliedCount() {
        return appliedCount;
    }

    public int getRequestedCount() {
        return requestedCount;
    }

    public List<String> getFailures() {
        return new ArrayList<>(failures);
    }

    public boolean isSuccessful() {
        return failures.isEmpty() && appliedCount == requestedCount;
    }

    public String toDisplayMessage(String deviceName) {
        if (requestedCount == 0) {
            return "No permissions were selected for " + deviceName + ".";
        }
        if (isSuccessful()) {
            return "Applied " + appliedCount + " permission settings on " + deviceName + ".";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Applied ").append(appliedCount).append(" of ").append(requestedCount)
                .append(" permission settings on ").append(deviceName).append(".");
        if (!failures.isEmpty()) {
            builder.append("\n").append("Failed:").append("\n");
            for (String failure : failures) {
                builder.append("- ").append(failure).append("\n");
            }
        }
        return builder.toString().trim();
    }
}
