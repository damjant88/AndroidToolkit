package androidtoolkit.app;

public class PermissionDefinition {

    private final String id;
    private final String label;
    private final PermissionCommandType commandType;
    private final String commandValue;

    public PermissionDefinition(String id, String label, PermissionCommandType commandType, String commandValue) {
        this.id = id;
        this.label = label;
        this.commandType = commandType;
        this.commandValue = commandValue;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public PermissionCommandType getCommandType() {
        return commandType;
    }

    public String getCommandValue() {
        return commandValue;
    }
}
