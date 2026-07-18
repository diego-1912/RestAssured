package config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import java.io.InputStream;
import java.util.Objects;

/**
 * Central config loader. Resolves the active {@link Environment} (system property
 * {@code -Denv=qa} or env var {@code ENV}, default dev), loads
 * {@code config/<env>.yaml} from the test classpath, and exposes typed accessors.
 *
 * <p>Individual values can be overridden without touching YAML by passing a system
 * property of the same dotted path, e.g. {@code -Drest.baseUrl=https://...}.
 *
 * <p>Secrets are never stored in YAML - only the name of the environment variable
 * that holds them. Use {@link #resolveSecret(String)} to read the actual value.
 */
public final class ConfigManager {

    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);

    private static volatile ConfigManager instance;

    private final Environment environment;
    private final EnvironmentConfig config;

    private ConfigManager(Environment environment) {
        this.environment = environment;
        this.config = load(environment);
        log.info("Loaded configuration for environment '{}'", environment);
    }

    public static ConfigManager getInstance() {
        Environment current = Environment.current();
        ConfigManager local = instance;
        if (local == null || local.environment != current) {
            synchronized (ConfigManager.class) {
                if (instance == null || instance.environment != current) {
                    instance = new ConfigManager(current);
                }
                local = instance;
            }
        }
        return local;
    }

    private static EnvironmentConfig load(Environment environment) {
        String resource = "config/" + environment.configFileName();
        try (InputStream in = ConfigManager.class.getClassLoader().getResourceAsStream(resource)) {
            Objects.requireNonNull(in, "Missing config resource on classpath: " + resource);

            LoaderOptions loaderOptions = new LoaderOptions();
            Constructor constructor = new Constructor(EnvironmentConfig.class, loaderOptions);
            PropertyUtils propertyUtils = new PropertyUtils();
            propertyUtils.setSkipMissingProperties(true);
            propertyUtils.setBeanAccess(BeanAccess.FIELD);
            constructor.setPropertyUtils(propertyUtils);

            Yaml yaml = new Yaml(constructor);
            EnvironmentConfig cfg = yaml.load(in);
            Objects.requireNonNull(cfg, "Config file was empty: " + resource);
            return cfg;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load configuration from " + resource, e);
        }
    }

    public Environment environment() {
        return environment;
    }

    // ---- REST ----

    public String restBaseUrl() {
        return override("rest.baseUrl", config.rest.baseUrl);
    }

    public int restConnectTimeoutMs() {
        return config.rest.connectTimeoutMs;
    }

    public int restReadTimeoutMs() {
        return config.rest.readTimeoutMs;
    }

    public int restSlaMs() {
        return config.rest.slaMs;
    }

    // ---- GraphQL ----

    public String graphqlBaseUrl() {
        return override("graphql.baseUrl", config.graphql.baseUrl);
    }

    public int graphqlConnectTimeoutMs() {
        return config.graphql.connectTimeoutMs;
    }

    public int graphqlReadTimeoutMs() {
        return config.graphql.readTimeoutMs;
    }

    // ---- WebSocket ----

    public String websocketUrl() {
        return override("websocket.url", config.websocket.url);
    }

    public int websocketHandshakeTimeoutMs() {
        return config.websocket.handshakeTimeoutMs;
    }

    public int websocketMessageTimeoutMs() {
        return config.websocket.messageTimeoutMs;
    }

    public int websocketReconnectAttempts() {
        return config.websocket.reconnectAttempts;
    }

    public int websocketReconnectDelayMs() {
        return config.websocket.reconnectDelayMs;
    }

    // ---- Auth ----

    public EnvironmentConfig.AuthConfig auth() {
        return config.auth;
    }

    // ---- Retry ----

    public EnvironmentConfig.RetryConfig retry() {
        return config.retry;
    }

    // ---- DB ----

    public EnvironmentConfig.DbConfig db() {
        return config.db;
    }

    /**
     * Resolves a secret by the name of the environment variable that holds it.
     * Never logs the value. Throws if the variable is required but unset.
     */
    public static String resolveSecret(String envVarName) {
        return resolveSecret(envVarName, true);
    }

    public static String resolveSecret(String envVarName, boolean required) {
        if (envVarName == null || envVarName.isBlank()) {
            if (required) {
                throw new IllegalStateException("No environment variable name configured for this secret");
            }
            return null;
        }
        String value = System.getenv(envVarName);
        if ((value == null || value.isBlank()) && required) {
            throw new IllegalStateException(
                    "Required environment variable '" + envVarName + "' is not set. "
                            + "Secrets must be injected via env vars / CI secret store, never hardcoded.");
        }
        return value;
    }

    private static String override(String systemPropertyPath, String defaultValue) {
        return System.getProperty(systemPropertyPath, defaultValue);
    }
}
