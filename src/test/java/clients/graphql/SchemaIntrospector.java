package clients.graphql;

import io.restassured.path.json.JsonPath;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Optional pre-flight validation: runs the standard GraphQL introspection query once,
 * caches {@code type -> field names}, and lets tests assert a query only references
 * fields that actually exist on the schema before it's sent - catching typos in
 * hand-written .graphql files earlier than a server-side error would.
 */
public class SchemaIntrospector {

    private static final String INTROSPECTION_QUERY = """
            query IntrospectionQuery {
              __schema {
                types {
                  name
                  fields { name }
                }
              }
            }
            """;

    private final GraphQLClient client;
    private Map<String, Set<String>> typeFields;

    public SchemaIntrospector(GraphQLClient client) {
        this.client = client;
    }

    public synchronized void loadSchema() {
        if (typeFields != null) {
            return;
        }
        JsonPath body = client.execute(INTROSPECTION_QUERY, Map.of()).jsonPath();
        Map<String, Set<String>> result = new HashMap<>();
        java.util.List<Map<String, Object>> types = body.getList("data.__schema.types");
        for (Map<String, Object> type : types) {
            String name = (String) type.get("name");
            Object fieldsObj = type.get("fields");
            Set<String> fieldNames = new HashSet<>();
            if (fieldsObj instanceof java.util.List<?> fields) {
                for (Object f : fields) {
                    if (f instanceof Map<?, ?> fieldMap) {
                        fieldNames.add((String) fieldMap.get("name"));
                    }
                }
            }
            result.put(name, fieldNames);
        }
        this.typeFields = result;
    }

    public boolean typeExists(String typeName) {
        loadSchema();
        return typeFields.containsKey(typeName);
    }

    public boolean fieldExists(String typeName, String fieldName) {
        loadSchema();
        Set<String> fields = typeFields.get(typeName);
        return fields != null && fields.contains(fieldName);
    }

    public void validateFieldsExist(String typeName, String... fieldNames) {
        loadSchema();
        if (!typeExists(typeName)) {
            throw new IllegalArgumentException("Type '" + typeName + "' does not exist in the schema");
        }
        for (String field : fieldNames) {
            if (!fieldExists(typeName, field)) {
                throw new IllegalArgumentException(
                        "Field '" + field + "' does not exist on type '" + typeName + "'");
            }
        }
    }
}
