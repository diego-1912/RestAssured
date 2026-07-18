package config;

import java.util.Map;

/**
 * Plain data holder mapped 1:1 from a config/{env}.yaml file via SnakeYAML.
 * Field names intentionally match the YAML keys.
 */
public class EnvironmentConfig {

    public String env;
    public RestConfig rest;
    public GraphqlConfig graphql;
    public WebsocketConfig websocket;
    public AuthConfig auth;
    public RetryConfig retry;
    public DbConfig db;

    public static class RestConfig {
        public String baseUrl;
        public int connectTimeoutMs;
        public int readTimeoutMs;
        public int slaMs;
    }

    public static class GraphqlConfig {
        public String baseUrl;
        public int connectTimeoutMs;
        public int readTimeoutMs;
    }

    public static class WebsocketConfig {
        public String url;
        public int handshakeTimeoutMs;
        public int messageTimeoutMs;
        public int reconnectAttempts;
        public int reconnectDelayMs;
    }

    /**
     * No secret VALUES ever live here - only the names of the environment variables
     * that hold them. Actual values are resolved at runtime via System.getenv(...).
     */
    public static class AuthConfig {
        public String type; // NONE | BASIC | BEARER | OAUTH2 | API_KEY
        public String tokenUrl;
        public String clientIdEnvVar;
        public String clientSecretEnvVar;
        public String apiKeyHeader;
        public String apiKeyEnvVar;
        public String bearerTokenEnvVar;
        public String basicUsernameEnvVar;
        public String basicPasswordEnvVar;
    }

    public static class RetryConfig {
        public int maxAttempts;
        public long backoffMs;
        public double backoffMultiplier;
    }

    public static class DbConfig {
        public String url;
        public String usernameEnvVar;
        public String passwordEnvVar;
    }

    /** Escape hatch for any extra keys added to a YAML file without a matching field. */
    public Map<String, Object> extra;
}
