package clients.graphql;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads .graphql query/mutation files from src/test/resources/graphql at runtime and
 * caches them by path, so queries live as version-controlled .graphql files instead of
 * Java string literals.
 */
public final class GraphQLQueryLoader {

    private static final String BASE_DIR = "graphql/";
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    private GraphQLQueryLoader() {
    }

    /**
     * @param relativePath path under src/test/resources/graphql, e.g. "queries/getPost.graphql"
     */
    public static String load(String relativePath) {
        return CACHE.computeIfAbsent(relativePath, GraphQLQueryLoader::readFromClasspath);
    }

    private static String readFromClasspath(String relativePath) {
        String resource = BASE_DIR + relativePath;
        try (InputStream in = GraphQLQueryLoader.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalArgumentException("GraphQL file not found on classpath: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read GraphQL file: " + resource, e);
        }
    }
}
