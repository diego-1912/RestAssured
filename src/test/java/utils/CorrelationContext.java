package utils;

import java.util.UUID;

/**
 * Holds the correlation/trace id for the current test thread so it can be
 * propagated across REST -> GraphQL -> WebSocket calls within the same test,
 * e.g. asserting that a WS push event carries the id of the REST call that triggered it.
 */
public final class CorrelationContext {

    public static final String HEADER_NAME = "X-Correlation-Id";

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private CorrelationContext() {
    }

    public static String current() {
        String id = CURRENT.get();
        if (id == null) {
            id = newId();
            CURRENT.set(id);
        }
        return id;
    }

    public static String startNew() {
        String id = newId();
        CURRENT.set(id);
        return id;
    }

    public static void set(String id) {
        CURRENT.set(id);
    }

    public static void clear() {
        CURRENT.remove();
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }
}
