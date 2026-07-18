package clients.rest.auth;

import io.restassured.specification.RequestSpecification;

public class NoAuthProvider implements AuthenticationProvider {

    @Override
    public RequestSpecification apply(RequestSpecification spec) {
        return spec;
    }
}
