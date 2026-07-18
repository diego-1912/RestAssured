package models.graphql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import models.Post;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PostsPage {

    private List<Post> data;
    private PageMeta meta;

    public List<Post> getData() {
        return data;
    }

    public void setData(List<Post> data) {
        this.data = data;
    }

    public PageMeta getMeta() {
        return meta;
    }

    public void setMeta(PageMeta meta) {
        this.meta = meta;
    }
}
