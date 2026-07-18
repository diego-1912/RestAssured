package tests.flows;

import clients.graphql.GraphQLClient;
import clients.graphql.GraphQLQueryLoader;
import clients.rest.PostsApiClient;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import models.Post;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tests.BaseTest;
import utils.CorrelationContext;
import utils.PollingUtils;

import java.time.Duration;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Demonstrates the cross-cutting utilities working together across API types: a single
 * correlation id is attached by {@code CorrelationIdFilter} to both the REST and GraphQL
 * call in this test (see clients.rest.filters.CorrelationIdFilter and
 * clients.graphql.GraphQLClient), and {@link PollingUtils} shows the retry/backoff
 * pattern used when a read needs to wait for eventual consistency after a write or an
 * event pushed over WebSocket.
 */
@Epic("API Test Framework")
@Feature("Cross-cutting utilities")
@Tag("regression")
class CrossCuttingUtilsTest extends BaseTest {

    private final PostsApiClient postsApi = new PostsApiClient();
    private final GraphQLClient graphQLClient = new GraphQLClient();

    @Test
    @Description("The same correlation id is attached to both REST and GraphQL calls within one test")
    void correlationId_isSharedAcrossRestAndGraphQLCallsInOneTest() {
        String correlationId = CorrelationContext.current();

        postsApi.getPost(1).then().statusCode(200);
        assertThat("REST call must not rotate the test's correlation id",
                CorrelationContext.current(), equalTo(correlationId));

        String query = GraphQLQueryLoader.load("queries/getPost.graphql");
        graphQLClient.execute(query, Map.of("id", 1)).then().statusCode(200);
        assertThat("GraphQL call must not rotate the test's correlation id",
                CorrelationContext.current(), equalTo(correlationId));
    }

    @Test
    @Description("PollingUtils retries a read until a condition is met, for eventually-consistent flows")
    void pollingUtils_waitsUntilConditionIsMet() {
        Post post = PollingUtils.waitUntil(
                () -> postsApi.getPostAsPojo(1),
                p -> p.getTitle() != null && !p.getTitle().isBlank(),
                Duration.ofSeconds(10),
                Duration.ofMillis(500));

        assertThat(post.getTitle(), notNullValue());
    }
}
