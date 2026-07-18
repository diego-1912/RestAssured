package models.graphql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import models.Post;

/** Shape of the "data" object for a `createPost(input: ...)` mutation. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreatePostResult {

    private Post createPost;

    public Post getCreatePost() {
        return createPost;
    }

    public void setCreatePost(Post createPost) {
        this.createPost = createPost;
    }
}
