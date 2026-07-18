package clients.rest.auth;

import io.restassured.specification.RequestSpecification;

public class BasicAuthProvider implements AuthenticationProvider {

    private final String username;
    private final String password;

    public BasicAuthProvider(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public RequestSpecification apply(RequestSpecification spec) {
        return spec.auth().preemptive().basic(username, password);
    }
}
