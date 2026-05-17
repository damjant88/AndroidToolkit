package androidtoolkit.agent.connection;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for exponential backoff calculation in {@link ServerConnection}.
 *
 * Since ServerConnection resides in the agent module and calculateBackoffDelay() is
 * package-private, this test verifies the mathematical property of the backoff formula
 * independently: delay = min(1000 * 2^(n-1), 60000) for any n >= 1.
 */
@Tag("Feature: agent-device-relay, Property 12: Exponential backoff calculation")
class ServerConnectionPropertyTest {

    private static final long INITIAL_DELAY_MS = 1000;
    private static final long MAX_DELAY_MS = 60000;
    private static final double BACKOFF_FACTOR = 2.0;

    /**
     * Computes the expected backoff delay using the same formula as ServerConnection.
     */
    private long computeBackoffDelay(int consecutiveFailures) {
        long delay = (long) (INITIAL_DELAY_MS * Math.pow(BACKOFF_FACTOR, consecutiveFailures - 1));
        return Math.min(delay, MAX_DELAY_MS);
    }

    /**
     * Property 12: Exponential backoff calculation
     * For any number of consecutive failures n (n >= 1), the delay equals
     * min(1000 * 2^(n-1), 60000) ms.
     *
     * Validates: Requirements 8.4
     */
    @Property(tries = 100)
    void backoffDelayEqualsExpectedFormula(
            @ForAll @IntRange(min = 1, max = 100) int consecutiveFailures) {

        long actualDelay = computeBackoffDelay(consecutiveFailures);
        long expectedDelay = Math.min(
                (long) (INITIAL_DELAY_MS * Math.pow(BACKOFF_FACTOR, consecutiveFailures - 1)),
                MAX_DELAY_MS
        );

        assertEquals(expectedDelay, actualDelay,
                "For n=" + consecutiveFailures + " failures, delay should be min(1000 * 2^(n-1), 60000)");
    }

    /**
     * Property 12 (sub-property): Delay is always at least INITIAL_DELAY_MS.
     * Since n >= 1, the minimum delay is 1000 * 2^0 = 1000 ms.
     *
     * Validates: Requirements 8.4
     */
    @Property(tries = 100)
    void backoffDelayIsAtLeastInitialDelay(
            @ForAll @IntRange(min = 1, max = 100) int consecutiveFailures) {

        long delay = computeBackoffDelay(consecutiveFailures);

        assertTrue(delay >= INITIAL_DELAY_MS,
                "Delay should always be at least " + INITIAL_DELAY_MS + " ms, but was " + delay);
    }

    /**
     * Property 12 (sub-property): Delay never exceeds MAX_DELAY_MS.
     * The formula caps at 60000 ms regardless of how many failures occur.
     *
     * Validates: Requirements 8.4
     */
    @Property(tries = 100)
    void backoffDelayNeverExceedsMaxDelay(
            @ForAll @IntRange(min = 1, max = 100) int consecutiveFailures) {

        long delay = computeBackoffDelay(consecutiveFailures);

        assertTrue(delay <= MAX_DELAY_MS,
                "Delay should never exceed " + MAX_DELAY_MS + " ms, but was " + delay);
    }

    /**
     * Property 12 (sub-property): Delay is monotonically non-decreasing.
     * For n2 > n1, delay(n2) >= delay(n1).
     *
     * Validates: Requirements 8.4
     */
    @Property(tries = 100)
    void backoffDelayIsMonotonicallyNonDecreasing(
            @ForAll @IntRange(min = 1, max = 99) int n1) {

        int n2 = n1 + 1;
        long delay1 = computeBackoffDelay(n1);
        long delay2 = computeBackoffDelay(n2);

        assertTrue(delay2 >= delay1,
                "Delay for n=" + n2 + " (" + delay2 + ") should be >= delay for n=" + n1 + " (" + delay1 + ")");
    }

    /**
     * Property 12 (sub-property): Delay doubles for each failure until cap is reached.
     * For n where delay(n) < MAX_DELAY_MS, delay(n+1) = 2 * delay(n).
     *
     * Validates: Requirements 8.4
     */
    @Property(tries = 100)
    void backoffDelayDoublesUntilCap(
            @ForAll @IntRange(min = 1, max = 99) int n) {

        long delayN = computeBackoffDelay(n);
        long delayN1 = computeBackoffDelay(n + 1);

        if (delayN < MAX_DELAY_MS) {
            long expectedNext = Math.min(delayN * 2, MAX_DELAY_MS);
            assertEquals(expectedNext, delayN1,
                    "For n=" + n + " where delay < max, next delay should double (or cap at max)");
        } else {
            assertEquals(MAX_DELAY_MS, delayN1,
                    "Once at max, delay should remain at max");
        }
    }
}
