package clients.rest;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import specs.RequestSpecFactory;

/**
 * Thin base for service-object REST clients. Subclasses expose endpoint-specific,
 * business-meaningful methods (see {@link PostsApiClient}) instead of leaking raw
 * given()/when()/then() chains into test classes.
 */
public abstract class BaseRestClient {

    protected RequestSpecification spec() {
        return RequestSpecFactory.restSpec();
    }

    protected Response get(String path, Object... pathParams) {
        return spec().get(path, pathParams);
    }

    protected Response post(String path, Object body, Object... pathParams) {
        return spec().body(body).post(path, pathParams);
    }

    protected Response put(String path, Object body, Object... pathParams) {
        return spec().body(body).put(path, pathParams);
    }

    protected Response patch(String path, Object body, Object... pathParams) {
        return spec().body(body).patch(path, pathParams);
    }

    protected Response delete(String path, Object... pathParams) {
        return spec().delete(path, pathParams);
    }
}
