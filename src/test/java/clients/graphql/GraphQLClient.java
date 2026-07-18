package clients.graphql;

import clients.rest.filters.CorrelationIdFilter;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import config.ConfigManager;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import models.graphql.GraphQLRequest;
import models.graphql.GraphQLResponse;

import java.util.Map;

/**
 * Thin wrapper around RestAssured for GraphQL-over-HTTP: every operation is a POST with
 * a {query, variables, operationName} JSON body against a single endpoint, and the
 * response is always HTTP 200 with a {data, errors} envelope even on failure - so
 * status-code assertions alone are not enough and callers must inspect the envelope.
 */
public class GraphQLClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Raw request/response - use when you need to assert on HTTP status/headers/time directly. */
    public Response execute(String query, Map<String, Object> variables) {
        return execute(query, variables, null);
    }

    public Response execute(String query, Map<String, Object> variables, String operationName) {
        GraphQLRequest body = new GraphQLRequest(query, variables, operationName);
        return spec().body(body).post();
    }

    /** Deserializes the {data, errors} envelope, with `data` mapped to the given type. */
    public <T> GraphQLResponse<T> executeTyped(String query, Map<String, Object> variables, Class<T> dataType) {
        Response response = execute(query, variables);
        return parse(response, dataType);
    }

    public <T> GraphQLResponse<T> executeTyped(String query, Map<String, Object> variables,
                                                String operationName, Class<T> dataType) {
        Response response = execute(query, variables, operationName);
        return parse(response, dataType);
    }

    private <T> GraphQLResponse<T> parse(Response response, Class<T> dataType) {
        JavaType type = MAPPER.getTypeFactory().constructParametricType(GraphQLResponse.class, dataType);
        try {
            return MAPPER.readValue(response.asString(), type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse GraphQL response envelope: " + response.asString(), e);
        }
    }

    /**
     * Firing an HTTP method directly on a spec built by {@link RequestSpecBuilder}
     * (without routing it through {@code given()} first) trips a known
     * rest-assured/Groovy bug - see rest-assured/rest-assured#938 - so the built spec is
     * always wrapped via {@code RestAssured.given().spec(...)} before use.
     */
    private RequestSpecification spec() {
        ConfigManager config = ConfigManager.getInstance();
        RequestSpecification builtSpec = new RequestSpecBuilder()
                .setBaseUri(config.graphqlBaseUrl())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .setConfig(RestAssuredConfig.config().httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", config.graphqlConnectTimeoutMs())
                        .setParam("http.socket.timeout", config.graphqlReadTimeoutMs())))
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .addFilter(new CorrelationIdFilter())
                .addFilter(new AllureRestAssured())
                .build();
        return io.restassured.RestAssured.given().spec(builtSpec);
    }
}
