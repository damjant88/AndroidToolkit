package androidtoolkit.service;

import java.util.LinkedHashSet;
import java.util.Set;

public class PermissionStateSnapshot {

    private final Set<String> activePermissionIds;
    private final Set<String> unavailablePermissionIds;

    public PermissionStateSnapshot(Set<String> activePermissionIds, Set<String> unavailablePermissionIds) {
        this.activePermissionIds = new LinkedHashSet<>(activePermissionIds);
        this.unavailablePermissionIds = new LinkedHashSet<>(unavailablePermissionIds);
    }

    public Set<String> getActivePermissionIds() {
        return new LinkedHashSet<>(activePermissionIds);
    }

    public Set<String> getUnavailablePermissionIds() {
        return new LinkedHashSet<>(unavailablePermissionIds);
    }
}
