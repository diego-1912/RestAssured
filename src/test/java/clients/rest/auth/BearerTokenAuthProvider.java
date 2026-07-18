package clients.rest.auth;

import io.restassured.specification.RequestSpecification;

import java.util.function.Supplier;

/**
 * Applies a bearer token supplied lazily via {@link Supplier}, so a static token,
 * an env-var-backed token, or a caching {@link OAuth2AuthProvider}-style supplier
 * can all share this implementation.
 */
public class BearerTokenAuthProvider implements AuthenticationProvider {

    private final Supplier<String> tokenSupplier;

    public BearerTokenAuthProvider(Supplier<String> tokenSupplier) {
        this.tokenSupplier = tokenSupplier;
    }

    public BearerTokenAuthProvider(String staticToken) {
        this(() -> staticToken);
    }

    @Override
    public RequestSpecification apply(RequestSpecification spec) {
        return spec.header("Authorization", "Bearer " + tokenSupplier.get());
    }
}
