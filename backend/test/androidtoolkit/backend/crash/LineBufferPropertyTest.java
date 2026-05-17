package androidtoolkit.backend.crash;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for {@link LineBuffer}.
 */
@Tag("Feature: crash-detection-alerts, Property 6: Rolling buffer invariant")
class LineBufferPropertyTest {

    /**
     * Property 6: Rolling buffer invariant
     * For any sequence of N lines added to a buffer with capacity C,
     * the buffer contains exactly min(N, C) lines and they are the most recent lines in insertion order.
     */
    @Property(tries = 200)
    void bufferContainsExactlyMinOfNAndCLines(
            @ForAll @IntRange(min = 50, max = 500) int capacity,
            @ForAll @IntRange(min = 0, max = 1000) int lineCount) {

        LineBuffer buffer = new LineBuffer(capacity);
        for (int i = 0; i < lineCount; i++) {
            buffer.add("line-" + i);
        }

        int expectedSize = Math.min(lineCount, capacity);
        assertEquals(expectedSize, buffer.size(),
                "Buffer should contain min(N, C) lines");
    }

    @Property(tries = 200)
    void bufferRetainsMostRecentLinesInOrder(
            @ForAll @IntRange(min = 50, max = 500) int capacity,
            @ForAll @IntRange(min = 1, max = 1000) int lineCount) {

        LineBuffer buffer = new LineBuffer(capacity);
        List<String> allLines = IntStream.range(0, lineCount)
                .mapToObj(i -> "line-" + i)
                .toList();

        for (String line : allLines) {
            buffer.add(line);
        }

        List<String> snapshot = buffer.snapshot();
        int expectedSize = Math.min(lineCount, capacity);
        int startIndex = lineCount - expectedSize;

        assertEquals(expectedSize, snapshot.size());

        // Verify the lines are the most recent ones in insertion order
        for (int i = 0; i < expectedSize; i++) {
            assertEquals(allLines.get(startIndex + i), snapshot.get(i),
                    "Line at position " + i + " should be the " + (startIndex + i) + "th inserted line");
        }
    }

    @Property(tries = 200)
    void bufferSizeNeverExceedsCapacity(
            @ForAll @IntRange(min = 50, max = 2000) int capacity,
            @ForAll @IntRange(min = 0, max = 5000) int lineCount) {

        LineBuffer buffer = new LineBuffer(capacity);
        for (int i = 0; i < lineCount; i++) {
            buffer.add("line-" + i);
            assertTrue(buffer.size() <= capacity,
                    "Buffer size should never exceed capacity");
        }
    }

    @Property(tries = 100)
    void snapshotIsImmutable(
            @ForAll @IntRange(min = 50, max = 200) int capacity,
            @ForAll @IntRange(min = 1, max = 100) int lineCount) {

        LineBuffer buffer = new LineBuffer(capacity);
        for (int i = 0; i < lineCount; i++) {
            buffer.add("line-" + i);
        }

        List<String> snapshot = buffer.snapshot();
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add("extra"));
    }
}
