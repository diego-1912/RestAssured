package utils;

import models.Post;
import models.User;
import net.datafaker.Faker;

/** DataFaker-backed builders for dynamic, non-colliding test payloads. */
public final class TestDataGenerator {

    private static final Faker FAKER = new Faker();

    private TestDataGenerator() {
    }

    public static Post randomPost() {
        return randomPost(FAKER.number().numberBetween(1, 10));
    }

    public static Post randomPost(int userId) {
        return new Post(userId, FAKER.lorem().sentence(), FAKER.lorem().paragraph());
    }

    public static User randomUser() {
        String first = FAKER.name().firstName();
        String last = FAKER.name().lastName();
        String username = (first + "." + last + IdGenerator.shortId()).toLowerCase();
        return new User(first + " " + last, username, username + "@example-test.com");
    }

    public static Faker faker() {
        return FAKER;
    }
}
