package androidtoolkit.backend.crash;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * A rolling buffer that maintains the last N logcat lines per device session.
 * When the buffer reaches capacity, the oldest line is evicted to make room for new lines.
 *
 * <p>Thread safety is NOT provided — the buffer is accessed per-session.
 */
public class LineBuffer {

    private final int capacity;
    private final Deque<String> lines;

    /**
     * Creates a new LineBuffer with the specified capacity.
     *
     * @param capacity the maximum number of lines to retain (must be between 50 and 2000 inclusive)
     * @throws IllegalArgumentException if capacity is outside the valid range [50, 2000]
     */
    public LineBuffer(int capacity) {
        if (capacity < 50 || capacity > 2000) {
            throw new IllegalArgumentException(
                    "Buffer capacity must be between 50 and 2000 inclusive, got: " + capacity);
        }
        this.capacity = capacity;
        this.lines = new ArrayDeque<>(capacity);
    }

    /**
     * Adds a line to the buffer. If the buffer is at capacity, the oldest line is evicted first.
     *
     * @param line the logcat line to add
     */
    public void add(String line) {
        if (lines.size() >= capacity) {
            lines.pollFirst();
        }
        lines.addLast(line);
    }

    /**
     * Returns an immutable snapshot of the current buffer contents in insertion order.
     *
     * @return an unmodifiable list of the buffered lines
     */
    public List<String> snapshot() {
        return List.copyOf(lines);
    }

    /**
     * Returns the current number of lines in the buffer.
     *
     * @return the number of lines currently stored
     */
    public int size() {
        return lines.size();
    }

    /**
     * Returns the maximum capacity of this buffer.
     *
     * @return the configured capacity
     */
    public int getCapacity() {
        return capacity;
    }
}
