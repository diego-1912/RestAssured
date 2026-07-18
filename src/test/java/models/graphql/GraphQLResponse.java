package models.graphql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * GraphQL-over-HTTP response envelope: { data, errors }. GraphQL servers commonly
 * return HTTP 200 even when {@code errors} is populated, and {@code data} can be
 * partially populated alongside errors - callers must check both explicitly rather
 * than relying on HTTP status alone.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphQLResponse<T> {

    private T data;
    private List<GraphQLError> errors;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public List<GraphQLError> getErrors() {
        return errors;
    }

    public void setErrors(List<GraphQLError> errors) {
        this.errors = errors;
    }

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public boolean hasData() {
        return data != null;
    }

    /** True when the server returned both errors and some usable data - a valid GraphQL outcome. */
    public boolean isPartialSuccess() {
        return hasErrors() && hasData();
    }

    public boolean isCleanSuccess() {
        return !hasErrors() && hasData();
    }
}
