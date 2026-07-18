package config;

import java.util.Locale;

/**
 * Supported test environments. Selected via -Denv=qa (system property) or the ENV
 * environment variable. Defaults to DEV when neither is set.
 */
public enum Environment {
    DEV,
    QA,
    STAGING,
    PROD;

    public static Environment current() {
        String raw = System.getProperty("env", System.getenv("ENV"));
        return fromString(raw);
    }

    public static Environment fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEV;
        }
        try {
            return Environment.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown environment '" + raw + "'. Expected one of: dev, qa, staging, prod", e);
        }
    }

    public String configFileName() {
        return name().toLowerCase(Locale.ROOT) + ".yaml";
    }
}
