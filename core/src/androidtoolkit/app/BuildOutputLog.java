package androidtoolkit.app;

/**
 * Abstraction for logging build operation messages.
 * The Swing ConsoleView implements this, but a web backend could use a different implementation.
 */
public interface BuildOutputLog {
    void appendText(String text);
}
