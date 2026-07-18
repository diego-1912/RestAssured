package models.graphql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import models.Post;

/** Shape of the "data" object for a `post(id: ID!)` query - wraps the REST-shared Post POJO. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostResult {

    private Post post;

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }
}
