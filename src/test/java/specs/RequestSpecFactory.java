package specs;

import clients.rest.auth.AuthProviderFactory;
import clients.rest.auth.AuthenticationProvider;
import clients.rest.filters.CorrelationIdFilter;
import clients.rest.filters.RetryFilter;
import config.ConfigManager;
import config.EnvironmentConfig;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * Builds the shared {@link RequestSpecification} for REST calls: base URI, default
 * content type, timeouts, logging/correlation/retry filters and auth - so individual
 * API clients only need to describe their endpoint-specific behaviour.
 */
public final class RequestSpecFactory {

    private RequestSpecFactory() {
    }

    /**
     * Returns a request-ready {@link RequestSpecification} obtained via
     * {@code RestAssured.given().spec(...)}. Firing an HTTP method directly on a spec
     * built by {@link RequestSpecBuilder} (without routing it through {@code given()}
     * first) trips a known rest-assured/Groovy bug - see rest-assured/rest-assured#938 -
     * so every caller must get its spec from this factory rather than building one itself.
     */
    public static RequestSpecification restSpec() {
        ConfigManager config = ConfigManager.getInstance();
        EnvironmentConfig.RetryConfig retry = config.retry();
        AuthenticationProvider auth = AuthProviderFactory.fromConfig(config);

        RequestSpecification builtSpec = new RequestSpecBuilder()
                .setBaseUri(config.restBaseUrl())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .setConfig(restAssuredConfig(config))
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .addFilter(new CorrelationIdFilter())
                .addFilter(new RetryFilter(retry.maxAttempts, retry.backoffMs, retry.backoffMultiplier))
                .addFilter(new AllureRestAssured())
                .build();

        RequestSpecification runnableSpec = RestAssured.given().spec(builtSpec);
        return auth.apply(runnableSpec);
    }

    private static RestAssuredConfig restAssuredConfig(ConfigManager config) {
        return RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", config.restConnectTimeoutMs())
                        .setParam("http.socket.timeout", config.restReadTimeoutMs()));
    }
}
