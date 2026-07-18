package tests.websocket;

import clients.websocket.WebSocketClientFactory;
import clients.websocket.WebSocketTestClient;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import models.Post;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.RetryingTest;
import tests.BaseTest;

import java.time.Duration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Epic("API Test Framework")
@Feature("WebSocket - Echo")
@Tag("websocket")
class EchoWebSocketTest extends BaseTest {

    private WebSocketTestClient client;

    @AfterEach
    void closeConnection() {
        if (client != null && client.isOpen()) {
            client.disconnect(Duration.ofSeconds(5));
        }
    }

    @RetryingTest(maxAttempts = 3)
    @Tag("smoke")
    @Description("Client connects, sends a text frame, and receives the echoed message back. "
            + "Retried up to 3x - WS timing against a public echo server can be flaky.")
    void connectAndEchoText() {
        client = WebSocketClientFactory.connect();

        client.sendText("hello from api-test-framework");
        String echoed = client.awaitMessage(Duration.ofSeconds(5));

        assertThat(echoed, equalTo("hello from api-test-framework"));
    }

    @Test
    @Tag("regression")
    @Description("JSON payloads round-trip using the same POJO models shared with REST/GraphQL tests")
    void connectAndEchoJson() {
        client = WebSocketClientFactory.connect();
        Post payload = new Post(1, "ws title", "ws body");

        client.sendJson(payload);
        Post echoed = client.awaitJson(Duration.ofSeconds(5), Post.class);

        assertThat(echoed.getTitle(), equalTo("ws title"));
        assertThat(echoed.getBody(), equalTo("ws body"));
    }

    @Test
    @Tag("regression")
    @Description("Ping/pong keeps the connection alive and reports healthy")
    void pingPongHealthCheck() {
        client = WebSocketClientFactory.connect();

        client.pingHealthCheck();

        assertThat(client.isOpen(), is(true));
    }

    @Test
    @Tag("regression")
    @Description("Connection can be cleanly closed and re-established")
    void disconnectAndReconnect() {
        client = WebSocketClientFactory.connect();
        assertThat(client.isOpen(), is(true));

        client.disconnect(Duration.ofSeconds(5));
        assertThat(client.isOpen(), is(false));

        client.reconnect(Duration.ofSeconds(5));
        assertThat(client.isOpen(), is(true));

        client.sendText("still alive");
        assertThat(client.awaitMessage(Duration.ofSeconds(5)), equalTo("still alive"));
    }

    @Test
    @Tag("regression")
    @Description("awaitMessageMatching skips unrelated frames and returns the first match")
    void awaitMessageMatching_skipsNonMatchingFrames() {
        client = WebSocketClientFactory.connect();

        client.sendText("noise-1");
        client.sendText("noise-2");
        client.sendText("target-message");

        String match = client.awaitMessageMatching(msg -> msg.startsWith("target"), Duration.ofSeconds(5));

        assertThat(match, equalTo("target-message"));
    }
}
