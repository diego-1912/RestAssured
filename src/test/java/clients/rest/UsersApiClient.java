package clients.rest;

import io.restassured.response.Response;
import models.User;

public class UsersApiClient extends BaseRestClient {

    public Response listUsers() {
        return get("/users");
    }

    public Response getUser(int id) {
        return get("/users/{id}", id);
    }

    public Response createUser(User user) {
        return post("/users", user);
    }

    public Response updateUser(int id, User user) {
        return put("/users/{id}", user, id);
    }

    public Response deleteUser(int id) {
        return delete("/users/{id}", id);
    }

    public User getUserAsPojo(int id) {
        return getUser(id).then().extract().as(User.class);
    }
}
