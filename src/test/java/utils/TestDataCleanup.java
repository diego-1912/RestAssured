package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Per-test registry of teardown actions. Tests register cleanup (e.g. "delete the post
 * I just created") right after creating data; {@link BaseTest} drains the registry in
 * {@code @AfterEach} in reverse order, regardless of whether the test passed or failed.
 */
public final class TestDataCleanup {

    private static final Logger log = LoggerFactory.getLogger(TestDataCleanup.class);
    private static final ThreadLocal<Deque<Runnable>> TASKS = ThreadLocal.withInitial(ArrayDeque::new);

    private TestDataCleanup() {
    }

    public static void register(Runnable cleanupTask) {
        TASKS.get().push(cleanupTask);
    }

    public static void runAll() {
        Deque<Runnable> tasks = TASKS.get();
        while (!tasks.isEmpty()) {
            Runnable task = tasks.pop();
            try {
                task.run();
            } catch (Exception e) {
                log.warn("Test data cleanup task failed (continuing): {}", e.getMessage());
            }
        }
    }
}
