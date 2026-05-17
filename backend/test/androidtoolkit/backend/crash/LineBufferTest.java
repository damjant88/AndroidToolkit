package androidtoolkit.backend.crash;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LineBuffer}.
 */
class LineBufferTest {

    @Test
    void constructor_rejectsCapacityBelowMinimum() {
        assertThrows(IllegalArgumentException.class, () -> new LineBuffer(49));
    }

    @Test
    void constructor_rejectsCapacityAboveMaximum() {
        assertThrows(IllegalArgumentException.class, () -> new LineBuffer(2001));
    }

    @Test
    void constructor_acceptsMinimumCapacity() {
        LineBuffer buffer = new LineBuffer(50);
        assertEquals(50, buffer.getCapacity());
    }

    @Test
    void constructor_acceptsMaximumCapacity() {
        LineBuffer buffer = new LineBuffer(2000);
        assertEquals(2000, buffer.getCapacity());
    }

    @Test
    void add_singleLine() {
        LineBuffer buffer = new LineBuffer(100);
        buffer.add("line 1");

        assertEquals(1, buffer.size());
        assertEquals(List.of("line 1"), buffer.snapshot());
    }

    @Test
    void add_multipleLinesWithinCapacity() {
        LineBuffer buffer = new LineBuffer(100);
        buffer.add("line 1");
        buffer.add("line 2");
        buffer.add("line 3");

        assertEquals(3, buffer.size());
        assertEquals(List.of("line 1", "line 2", "line 3"), buffer.snapshot());
    }

    @Test
    void add_evictsOldestWhenAtCapacity() {
        LineBuffer buffer = new LineBuffer(50);
        for (int i = 1; i <= 51; i++) {
            buffer.add("line " + i);
        }

        assertEquals(50, buffer.size());
        List<String> snapshot = buffer.snapshot();
        assertEquals("line 2", snapshot.get(0));
        assertEquals("line 51", snapshot.get(49));
    }

    @Test
    void add_maintainsInsertionOrder() {
        LineBuffer buffer = new LineBuffer(50);
        buffer.add("first");
        buffer.add("second");
        buffer.add("third");

        assertEquals(List.of("first", "second", "third"), buffer.snapshot());
    }

    @Test
    void snapshot_returnsImmutableList() {
        LineBuffer buffer = new LineBuffer(100);
        buffer.add("line 1");

        List<String> snapshot = buffer.snapshot();
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add("line 2"));
    }

    @Test
    void snapshot_returnsEmptyListWhenBufferIsEmpty() {
        LineBuffer buffer = new LineBuffer(100);
        assertEquals(List.of(), buffer.snapshot());
        assertEquals(0, buffer.size());
    }

    @Test
    void size_reflectsCurrentCount() {
        LineBuffer buffer = new LineBuffer(50);
        assertEquals(0, buffer.size());

        buffer.add("a");
        assertEquals(1, buffer.size());

        buffer.add("b");
        assertEquals(2, buffer.size());
    }

    @Test
    void size_neverExceedsCapacity() {
        LineBuffer buffer = new LineBuffer(50);
        for (int i = 0; i < 200; i++) {
            buffer.add("line " + i);
        }
        assertEquals(50, buffer.size());
    }

    @Test
    void getCapacity_returnsConfiguredValue() {
        assertEquals(500, new LineBuffer(500).getCapacity());
        assertEquals(100, new LineBuffer(100).getCapacity());
    }

    @Test
    void bufferRetainsMostRecentLines() {
        LineBuffer buffer = new LineBuffer(50);
        for (int i = 0; i < 100; i++) {
            buffer.add("line " + i);
        }

        List<String> snapshot = buffer.snapshot();
        assertEquals(50, snapshot.size());
        // Should contain lines 50-99 (the most recent 50)
        for (int i = 0; i < 50; i++) {
            assertEquals("line " + (i + 50), snapshot.get(i));
        }
    }
}
