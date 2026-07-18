package clients.rest;

import io.restassured.response.Response;
import models.Post;

import java.util.List;
import java.util.Map;

/**
 * Service-object client for the /posts resource. Test classes call these
 * business-meaningful methods rather than building requests inline.
 */
public class PostsApiClient extends BaseRestClient {

    public Response listPosts() {
        return get("/posts");
    }

    public Response listPostsByUser(int userId) {
        return spec().queryParam("userId", userId).get("/posts");
    }

    public Response getPost(int id) {
        return get("/posts/{id}", id);
    }

    public Response createPost(Post post) {
        return post("/posts", post);
    }

    public Response createPostWithHeaders(Post post, Map<String, String> extraHeaders) {
        return spec().headers(extraHeaders).body(post).post("/posts");
    }

    public Response updatePost(int id, Post post) {
        return put("/posts/{id}", post, id);
    }

    public Response patchPost(int id, Object partialBody) {
        return patch("/posts/{id}", partialBody, id);
    }

    public Response deletePost(int id) {
        return delete("/posts/{id}", id);
    }

    public List<Post> listPostsAsPojos() {
        return listPosts().then().extract().jsonPath().getList(".", Post.class);
    }

    public Post getPostAsPojo(int id) {
        return getPost(id).then().extract().as(Post.class);
    }
}
