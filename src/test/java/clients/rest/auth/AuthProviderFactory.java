package clients.rest.auth;

import config.ConfigManager;
import config.EnvironmentConfig;

import java.util.Locale;

/** Builds the right {@link AuthenticationProvider} from the active environment's auth config. */
public final class AuthProviderFactory {

    private AuthProviderFactory() {
    }

    public static AuthenticationProvider fromConfig(ConfigManager config) {
        EnvironmentConfig.AuthConfig auth = config.auth();
        String type = auth.type == null ? "NONE" : auth.type.toUpperCase(Locale.ROOT);

        return switch (type) {
            case "NONE" -> new NoAuthProvider();
            case "BASIC" -> new BasicAuthProvider(
                    ConfigManager.resolveSecret(auth.basicUsernameEnvVar),
                    ConfigManager.resolveSecret(auth.basicPasswordEnvVar));
            case "BEARER" -> new BearerTokenAuthProvider(
                    () -> ConfigManager.resolveSecret(auth.bearerTokenEnvVar));
            case "API_KEY" -> new ApiKeyAuthProvider(
                    auth.apiKeyHeader,
                    ConfigManager.resolveSecret(auth.apiKeyEnvVar));
            case "OAUTH2" -> new OAuth2AuthProvider(
                    auth.tokenUrl,
                    ConfigManager.resolveSecret(auth.clientIdEnvVar),
                    ConfigManager.resolveSecret(auth.clientSecretEnvVar));
            default -> throw new IllegalArgumentException("Unsupported auth type: " + auth.type);
        };
    }
}
