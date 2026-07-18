package clients.rest.auth;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/**
 * OAuth2 client-credentials provider with in-memory token caching and automatic refresh.
 * The token is fetched lazily on first use and re-fetched once it is within
 * {@link #EXPIRY_SAFETY_MARGIN_SECONDS} of expiring, so tests never pay the token-fetch
 * cost more often than necessary.
 */
public class OAuth2AuthProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthProvider.class);
    private static final long EXPIRY_SAFETY_MARGIN_SECONDS = 30;

    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;
    private final ReentrantLock lock = new ReentrantLock();

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    public OAuth2AuthProvider(String tokenUrl, String clientId, String clientSecret) {
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public RequestSpecification apply(RequestSpecification spec) {
        return spec.header("Authorization", "Bearer " + getValidToken());
    }

    public String getValidToken() {
        if (isValid(cachedToken, expiresAt)) {
            return cachedToken;
        }
        lock.lock();
        try {
            if (isValid(cachedToken, expiresAt)) {
                return cachedToken;
            }
            refreshToken();
            return cachedToken;
        } finally {
            lock.unlock();
        }
    }

    private boolean isValid(String token, Instant expiry) {
        return token != null && Instant.now().isBefore(expiry.minusSeconds(EXPIRY_SAFETY_MARGIN_SECONDS));
    }

    private void refreshToken() {
        log.info("Fetching new OAuth2 access token from {}", tokenUrl);
        Response response = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "client_credentials")
                .formParam("client_id", clientId)
                .formParam("client_secret", clientSecret)
                .post(tokenUrl);

        response.then().statusCode(200);

        String token = response.jsonPath().getString("access_token");
        int expiresInSeconds = response.jsonPath().getInt("expires_in");

        this.cachedToken = token;
        this.expiresAt = Instant.now().plusSeconds(expiresInSeconds);
        log.info("Cached new access token, expires at {}", expiresAt);
    }
}
