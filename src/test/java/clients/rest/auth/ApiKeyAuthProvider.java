package clients.rest.auth;

import io.restassured.specification.RequestSpecification;

public class ApiKeyAuthProvider implements AuthenticationProvider {

    private final String headerName;
    private final String apiKey;

    public ApiKeyAuthProvider(String headerName, String apiKey) {
        this.headerName = headerName;
        this.apiKey = apiKey;
    }

    @Override
    public RequestSpecification apply(RequestSpecification spec) {
        return spec.header(headerName, apiKey);
    }
}
