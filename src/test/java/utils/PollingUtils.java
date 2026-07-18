package utils;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Retry/backoff helper for polling-based assertions - e.g. a REST or GraphQL read that is
 * eventually consistent after a WebSocket-driven event, or after a write that's processed
 * asynchronously. Wraps Awaitility with sane defaults and clearer failure messages.
 */
public final class PollingUtils {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofMillis(500);

    private PollingUtils() {
    }

    /** Polls {@code supplier} until it returns a value matching {@code condition}, or times out. */
    public static <T> T waitUntil(Supplier<T> supplier, java.util.function.Predicate<T> condition) {
        return waitUntil(supplier, condition, DEFAULT_TIMEOUT, DEFAULT_POLL_INTERVAL);
    }

    public static <T> T waitUntil(Supplier<T> supplier, java.util.function.Predicate<T> condition,
                                   Duration timeout, Duration pollInterval) {
        try {
            return Awaitility.await()
                    .atMost(timeout)
                    .pollInterval(pollInterval)
                    .until(supplier::get, condition::test);
        } catch (ConditionTimeoutException e) {
            throw new AssertionError("Condition not met within " + timeout, e);
        }
    }

    /** Retries a fallible action a fixed number of times with exponential backoff. */
    public static <T> T retryWithBackoff(Callable<T> action, int maxAttempts, long initialBackoffMs,
                                          double backoffMultiplier) {
        long backoff = initialBackoffMs;
        Exception lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.call();
            } catch (Exception e) {
                lastError = e;
                if (attempt == maxAttempts) {
                    break;
                }
                sleep(backoff);
                backoff = (long) (backoff * backoffMultiplier);
            }
        }
        throw new RuntimeException("Action failed after " + maxAttempts + " attempts", lastError);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
