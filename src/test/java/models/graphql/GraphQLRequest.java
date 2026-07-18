package models.graphql;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Standard GraphQL-over-HTTP request envelope: { query, variables, operationName }.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GraphQLRequest {

    private String query;
    private Map<String, Object> variables;
    private String operationName;

    public GraphQLRequest() {
    }

    public GraphQLRequest(String query, Map<String, Object> variables) {
        this.query = query;
        this.variables = variables;
    }

    public GraphQLRequest(String query, Map<String, Object> variables, String operationName) {
        this.query = query;
        this.variables = variables;
        this.operationName = operationName;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }
}
