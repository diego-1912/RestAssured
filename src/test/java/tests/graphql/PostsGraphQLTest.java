package tests.graphql;

import clients.graphql.GraphQLAssertions;
import clients.graphql.GraphQLClient;
import clients.graphql.GraphQLQueryLoader;
import clients.graphql.SchemaIntrospector;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import models.graphql.CreatePostResult;
import models.graphql.GraphQLResponse;
import models.graphql.PostResult;
import models.graphql.PostsPageResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tests.BaseTest;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Epic("API Test Framework")
@Feature("GraphQL - Posts")
@Tag("graphql")
class PostsGraphQLTest extends BaseTest {

    private final GraphQLClient graphQLClient = new GraphQLClient();

    @Test
    @Tag("smoke")
    @Description("post(id) query returns clean data with no errors, deserialized via the shared Post POJO")
    void getPost_returnsCleanData() {
        String query = GraphQLQueryLoader.load("queries/getPost.graphql");

        GraphQLResponse<PostResult> response =
                graphQLClient.executeTyped(query, Map.of("id", 1), PostResult.class);

        GraphQLAssertions.assertCleanSuccess(response);
        assertThat(response.getData().getPost().getId(), equalTo(1));
        assertThat(response.getData().getPost().getTitle(), not(emptyString()));
    }

    @Test
    @Tag("regression")
    @Description("Querying a field that doesn't exist on the schema returns an errors array. "
            + "Some servers (like this one) reject query-validation failures with HTTP 400 rather "
            + "than 200, unlike execution-time errors which ride along a 200 - so tests must always "
            + "check the errors envelope rather than assuming a fixed status code.")
    void getPost_withUnknownField_returnsEmbeddedErrors() {
        String query = GraphQLQueryLoader.load("queries/getPostInvalidField.graphql");

        Response raw = graphQLClient.execute(query, Map.of("id", 1));
        assertThat(raw.statusCode(), anyOf(equalTo(200), equalTo(400)));

        GraphQLResponse<Map> response = graphQLClient.executeTyped(query, Map.of("id", 1), Map.class);
        GraphQLAssertions.assertHasErrors(response);
        GraphQLAssertions.assertErrorMessageContains(response, "thisFieldDoesNotExist");
    }

    @Test
    @Tag("regression")
    @Description("posts(options) query returns a page of results plus pagination meta")
    void listPosts_returnsPageWithMeta() {
        String query = GraphQLQueryLoader.load("queries/listPosts.graphql");

        GraphQLResponse<PostsPageResult> response =
                graphQLClient.executeTyped(query, Map.of("page", 1, "limit", 5), PostsPageResult.class);

        GraphQLAssertions.assertCleanSuccess(response);
        assertThat(response.getData().getPosts().getData(), hasSize(5));
        assertThat(response.getData().getPosts().getMeta().getTotalCount(), greaterThan(0));
    }

    @Test
    @Tag("regression")
    @Description("createPost mutation accepts variables and returns the created post")
    void createPost_mutationSucceeds() {
        String mutation = GraphQLQueryLoader.load("mutations/createPost.graphql");
        Map<String, Object> input = Map.of(
                "title", "Framework-generated title",
                "body", "Framework-generated body");

        GraphQLResponse<CreatePostResult> response =
                graphQLClient.executeTyped(mutation, Map.of("input", input), CreatePostResult.class);

        GraphQLAssertions.assertCleanSuccess(response);
        assertThat(response.getData().getCreatePost().getTitle(), equalTo("Framework-generated title"));
    }

    @Test
    @Tag("regression")
    @Description("Optional pre-flight schema introspection catches typo'd fields before sending a query")
    void schemaIntrospection_validatesQueriedFields() {
        SchemaIntrospector introspector = new SchemaIntrospector(graphQLClient);

        introspector.validateFieldsExist("Post", "id", "title", "body");

        assertThat(introspector.fieldExists("Post", "thisFieldDoesNotExist"), is(false));
    }
}
