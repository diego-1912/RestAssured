package clients.rest.auth;

import io.restassured.specification.RequestSpecification;

/** Applies whatever auth mechanism it represents to a RestAssured request spec. */
public interface AuthenticationProvider {

    RequestSpecification apply(RequestSpecification spec);
}
