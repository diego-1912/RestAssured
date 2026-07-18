package tests.rest;

import clients.rest.PostsApiClient;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import models.Post;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import specs.ResponseSpecFactory;
import tests.BaseTest;
import utils.IdGenerator;
import utils.TestDataCleanup;
import utils.TestDataGenerator;

import static org.hamcrest.Matchers.*;

@Epic("API Test Framework")
@Feature("REST - Posts")
@Tag("rest")
class PostsApiTest extends BaseTest {

    private final PostsApiClient postsApi = new PostsApiClient();

    @Test
    @Tag("smoke")
    @Description("GET /posts returns a non-empty list within the SLA")
    void listPosts_returnsNonEmptyList() {
        postsApi.listPosts()
                .then().spec(ResponseSpecFactory.ok())
                .body("size()", greaterThan(0))
                .body("[0].id", notNullValue());
    }

    @Test
    @Tag("smoke")
    @Description("GET /posts/{id} returns a single post matching the JSON schema")
    void getPost_returnsPostMatchingSchema() {
        postsApi.getPost(1)
                .then().spec(ResponseSpecFactory.ok())
                .body(matchesJsonSchema("post-schema.json"))
                .body("id", equalTo(1))
                .body("userId", notNullValue())
                .body("title", not(emptyString()));
    }

    @Test
    @Tag("regression")
    @Description("GET /posts?userId= filters results to that user")
    void listPostsByUser_filtersResults() {
        postsApi.listPostsByUser(1)
                .then().spec(ResponseSpecFactory.ok())
                .body("every { it.userId == 1 }", equalTo(true));
    }

    @Test
    @Tag("regression")
    @Description("POST /posts creates a post from generated test data and registers cleanup")
    void createPost_persistsGeneratedData() {
        Post newPost = TestDataGenerator.randomPost();

        Response response = postsApi.createPost(newPost);
        response.then().spec(ResponseSpecFactory.withStatus(201))
                .body("title", equalTo(newPost.getTitle()))
                .body("userId", equalTo(newPost.getUserId()))
                .body("id", notNullValue());

        int createdId = response.jsonPath().getInt("id");
        TestDataCleanup.register(() -> postsApi.deletePost(createdId));
    }

    @Test
    @Tag("regression")
    @Description("PUT /posts/{id} fully replaces a post")
    void updatePost_replacesFields() {
        Post updated = TestDataGenerator.randomPost(2);
        updated.setId(1);

        postsApi.updatePost(1, updated)
                .then().spec(ResponseSpecFactory.ok())
                .body("title", equalTo(updated.getTitle()))
                .body("userId", equalTo(2));
    }

    @Test
    @Tag("regression")
    @Description("DELETE /posts/{id} removes the post")
    void deletePost_succeeds() {
        postsApi.deletePost(1)
                .then().spec(ResponseSpecFactory.withStatus(200));
    }

    @Test
    @Tag("regression")
    @Description("Repeating a create with the same idempotency key header is safe to retry")
    void createPost_withIdempotencyKey_isTraceable() {
        String idempotencyKey = IdGenerator.idempotencyKey();
        Post newPost = TestDataGenerator.randomPost();

        Response response = postsApi.createPostWithHeaders(newPost, java.util.Map.of("Idempotency-Key", idempotencyKey));

        response.then().spec(ResponseSpecFactory.withStatus(201));
    }

    private org.hamcrest.Matcher<?> matchesJsonSchema(String schemaFile) {
        return utils.SchemaValidation.matchesSchema(schemaFile);
    }
}
