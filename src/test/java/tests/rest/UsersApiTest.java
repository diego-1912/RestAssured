package tests.rest;

import clients.rest.UsersApiClient;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import models.User;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import specs.ResponseSpecFactory;
import tests.BaseTest;

import static org.hamcrest.Matchers.*;

@Epic("API Test Framework")
@Feature("REST - Users")
@Tag("rest")
class UsersApiTest extends BaseTest {

    private final UsersApiClient usersApi = new UsersApiClient();

    @Test
    @Tag("smoke")
    @Description("GET /users/{id} returns a user matching the JSON schema, deserializable to a POJO")
    void getUser_returnsUserMatchingSchema() {
        usersApi.getUser(1)
                .then().spec(ResponseSpecFactory.ok())
                .body(utils.SchemaValidation.matchesSchema("user-schema.json"))
                .body("id", equalTo(1))
                .body("email", containsString("@"));

        User user = usersApi.getUserAsPojo(1);
        assertThatUserIsWellFormed(user);
    }

    @Test
    @Tag("regression")
    void listUsers_returnsMultipleUsers() {
        usersApi.listUsers()
                .then().spec(ResponseSpecFactory.ok())
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Tag("regression")
    void getUser_notFound_returns404() {
        usersApi.getUser(999_999)
                .then().spec(ResponseSpecFactory.withStatus(404));
    }

    private void assertThatUserIsWellFormed(User user) {
        org.hamcrest.MatcherAssert.assertThat(user.getId(), notNullValue());
        org.hamcrest.MatcherAssert.assertThat(user.getEmail(), containsString("@"));
    }
}
