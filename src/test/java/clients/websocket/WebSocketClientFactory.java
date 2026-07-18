package clients.websocket;

import config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.CorrelationContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Map;

/** Builds a {@link WebSocketTestClient} wired to the active environment's config. */
public final class WebSocketClientFactory {

    private static final Logger log = LoggerFactory.getLogger(WebSocketClientFactory.class);

    private WebSocketClientFactory() {
    }

    /** Creates and connects a client, propagating the current correlation id as a header. */
    public static WebSocketTestClient connect() {
        ConfigManager config = ConfigManager.getInstance();
        WebSocketTestClient client = build(config.websocketUrl());
        client.connect(Duration.ofMillis(config.websocketHandshakeTimeoutMs()));
        // enables the library's built-in ping/pong keep-alive + dead-connection detection
        client.setConnectionLostTimeout(30);
        return client;
    }

    public static WebSocketTestClient build(String url) {
        try {
            Map<String, String> headers = Map.of(CorrelationContext.HEADER_NAME, CorrelationContext.current());
            return new WebSocketTestClient(new URI(url), headers);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid WebSocket URL: " + url, e);
        }
    }

    /** Connects with retry/backoff, for scenarios exercising connection resilience. */
    public static WebSocketTestClient connectWithRetry() {
        ConfigManager config = ConfigManager.getInstance();
        int attempts = Math.max(1, config.websocketReconnectAttempts());
        long delayMs = config.websocketReconnectDelayMs();

        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return connect();
            } catch (RuntimeException e) {
                lastError = e;
                log.warn("WebSocket connect attempt {}/{} failed: {}", attempt, attempts, e.getMessage());
                if (attempt < attempts) {
                    sleep(delayMs);
                }
            }
        }
        throw new IllegalStateException("Failed to connect WebSocket after " + attempts + " attempts", lastError);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
