package utils;

import io.restassured.module.jsv.JsonSchemaValidator;
import org.hamcrest.Matcher;

/** Thin helper around rest-assured's json-schema-validator module for schemas/*.json files. */
public final class SchemaValidation {

    private SchemaValidation() {
    }

    /** @param classpathPath path under src/test/resources/schemas, e.g. "post-schema.json" */
    public static Matcher<?> matchesSchema(String classpathPath) {
        return JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/" + classpathPath);
    }
}
