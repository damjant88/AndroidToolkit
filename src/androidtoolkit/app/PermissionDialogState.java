package androidtoolkit.app;

import java.util.ArrayList;
import java.util.List;

public class PermissionDialogState {

    private final String packageName;
    private final List<PermissionDefinition> definitions;
    private final List<String> activePermissionIds;
    private final List<String> unavailablePermissionIds;

    public PermissionDialogState(
            String packageName,
            List<PermissionDefinition> definitions,
            List<String> activePermissionIds,
            List<String> unavailablePermissionIds
    ) {
        this.packageName = packageName;
        this.definitions = new ArrayList<>(definitions);
        this.activePermissionIds = new ArrayList<>(activePermissionIds);
        this.unavailablePermissionIds = new ArrayList<>(unavailablePermissionIds);
    }

    public String getPackageName() {
        return packageName;
    }

    public List<PermissionDefinition> getDefinitions() {
        return new ArrayList<>(definitions);
    }

    public List<String> getActivePermissionIds() {
        return new ArrayList<>(activePermissionIds);
    }

    public List<String> getUnavailablePermissionIds() {
        return new ArrayList<>(unavailablePermissionIds);
    }

    public boolean hasDefinitions() {
        return !definitions.isEmpty();
    }
}
