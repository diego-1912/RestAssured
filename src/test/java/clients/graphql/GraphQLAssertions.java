package clients.graphql;

import models.graphql.GraphQLError;
import models.graphql.GraphQLResponse;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Assertion helpers for the GraphQL {data, errors} envelope. GraphQL servers commonly
 * return HTTP 200 with errors embedded in the body, so these checks replace the
 * status-code assertions REST tests would use.
 */
public final class GraphQLAssertions {

    private GraphQLAssertions() {
    }

    public static <T> void assertCleanSuccess(GraphQLResponse<T> response) {
        assertThat("Expected no GraphQL errors, but got: " + errorMessages(response),
                response.hasErrors(), is(false));
        assertThat("Expected non-null data", response.getData(), notNullValue());
    }

    public static <T> void assertHasErrors(GraphQLResponse<T> response) {
        assertThat("Expected GraphQL errors array to be non-empty", response.hasErrors(), is(true));
    }

    public static <T> void assertPartialSuccess(GraphQLResponse<T> response) {
        assertThat("Expected data to be present alongside errors", response.hasData(), is(true));
        assertThat("Expected errors to be present alongside data", response.hasErrors(), is(true));
    }

    public static <T> void assertErrorMessageContains(GraphQLResponse<T> response, String expectedSubstring) {
        assertThat(errorMessages(response), hasItem(containsString(expectedSubstring)));
    }

    public static <T> void assertErrorCount(GraphQLResponse<T> response, int expectedCount) {
        assertThat(response.getErrors(), hasSize(expectedCount));
    }

    public static <T> List<String> errorMessages(GraphQLResponse<T> response) {
        if (!response.hasErrors()) {
            return List.of();
        }
        return response.getErrors().stream().map(GraphQLError::getMessage).toList();
    }
}
