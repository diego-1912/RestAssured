package utils;

import java.util.UUID;

/** Random/UUID generation for idempotency-key testing and unique test data. */
public final class IdGenerator {

    private IdGenerator() {
    }

    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    public static String idempotencyKey() {
        return "idem-" + uuid();
    }

    public static String shortId() {
        return uuid().substring(0, 8);
    }
}
