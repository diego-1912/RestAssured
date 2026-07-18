package models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Shared across REST and GraphQL tests - both APIs expose the same "post" shape,
 * so a single POJO is reused for request bodies and response deserialization in both.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Post {

    private Integer id;
    private Integer userId;
    private String title;
    private String body;

    public Post() {
    }

    public Post(Integer userId, String title, String body) {
        this.userId = userId;
        this.title = title;
        this.body = body;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "Post{id=%s, userId=%s, title='%s'}".formatted(id, userId, title);
    }
}
