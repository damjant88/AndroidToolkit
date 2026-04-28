package androidtoolkit.ui;

import java.awt.Color;

public class BuildProgressState {

    private final String message;
    private final boolean indeterminate;
    private final Color backgroundColor;

    public BuildProgressState(String message, boolean indeterminate, Color backgroundColor) {
        this.message = message;
        this.indeterminate = indeterminate;
        this.backgroundColor = backgroundColor;
    }

    public String getMessage() {
        return message;
    }

    public boolean isIndeterminate() {
        return indeterminate;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }
}
