package models.graphql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Shape of the "data" object for a `posts(options: ...)` query. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostsPageResult {

    private PostsPage posts;

    public PostsPage getPosts() {
        return posts;
    }

    public void setPosts(PostsPage posts) {
        this.posts = posts;
    }
}
