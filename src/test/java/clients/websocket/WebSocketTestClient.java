package clients.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Java-WebSocket based client for WS test scenarios. Incoming messages land on a
 * {@link BlockingQueue} so tests can wait for asynchronous pushes with a timeout instead
 * of polling or sleeping, mirroring the request/response ergonomics RestAssured gives
 * REST/GraphQL tests.
 */
public class WebSocketTestClient extends WebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(WebSocketTestClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
    private volatile Exception lastError;
    private volatile boolean closedByRemote;

    public WebSocketTestClient(URI serverUri) {
        super(serverUri);
    }

    public WebSocketTestClient(URI serverUri, Map<String, String> headers) {
        super(serverUri, headers);
    }

    // ---- lifecycle ----

    @Override
    public void onOpen(ServerHandshake handshake) {
        log.info("WebSocket connected: {} (status {})", getURI(), handshake.getHttpStatus());
    }

    @Override
    public void onMessage(String message) {
        log.debug("WebSocket message received: {}", message);
        messages.offer(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        closedByRemote = remote;
        log.info("WebSocket closed (code={}, reason='{}', remote={})", code, reason, remote);
    }

    @Override
    public void onError(Exception ex) {
        lastError = ex;
        log.error("WebSocket error", ex);
    }

    public void connect(Duration timeout) {
        try {
            boolean opened = connectBlocking(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!opened) {
                String detail = lastError != null ? ": " + lastError.getMessage() : "";
                throw new IllegalStateException("WebSocket handshake failed or timed out after " + timeout + detail);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while connecting WebSocket", e);
        }
    }

    public void disconnect(Duration timeout) {
        try {
            closeBlocking();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while closing WebSocket", e);
        }
    }

    /** Reconnects using the WebSocket protocol reconnect, re-establishing the same URI/headers. */
    public void reconnect(Duration timeout) {
        try {
            boolean opened = reconnectBlocking();
            if (!opened) {
                throw new IllegalStateException("WebSocket reconnect failed");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while reconnecting WebSocket", e);
        }
    }

    // ---- ping/pong health check ----

    /** Sends a protocol-level ping frame; the server is expected to reply with a pong automatically. */
    public void pingHealthCheck() {
        if (!isOpen()) {
            throw new IllegalStateException("Cannot ping - WebSocket is not open");
        }
        sendPing();
    }

    // ---- messaging ----

    public void sendText(String message) {
        send(message);
    }

    public void sendJson(Object payload) {
        try {
            send(MAPPER.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize WebSocket JSON payload", e);
        }
    }

    /** Blocks until a message arrives or the timeout elapses; fails the test on timeout. */
    public String awaitMessage(Duration timeout) {
        try {
            String message = messages.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (message == null) {
                throw new AssertionError("Timed out after " + timeout + " waiting for a WebSocket message");
            }
            return message;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for WebSocket message", e);
        }
    }

    /** Blocks until a message matching the predicate arrives (skipping ones that don't), or times out. */
    public String awaitMessageMatching(Predicate<String> predicate, Duration timeout) {
        long deadlineNanos = System.nanoTime() + timeout.toNanos();
        try {
            while (true) {
                long remainingMs = (deadlineNanos - System.nanoTime()) / 1_000_000;
                if (remainingMs <= 0) {
                    break;
                }
                String message = messages.poll(remainingMs, TimeUnit.MILLISECONDS);
                if (message == null) {
                    break;
                }
                if (predicate.test(message)) {
                    return message;
                }
                log.debug("Skipping non-matching WebSocket message: {}", message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for WebSocket message", e);
        }
        throw new AssertionError("Timed out after " + timeout + " waiting for a matching WebSocket message");
    }

    public <T> T awaitJson(Duration timeout, Class<T> type) {
        String raw = awaitMessage(timeout);
        try {
            return MAPPER.readValue(raw, type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize WebSocket message as " + type.getSimpleName()
                    + ": " + raw, e);
        }
    }

    public boolean hasPendingMessages() {
        return !messages.isEmpty();
    }

    public void clearMessages() {
        messages.clear();
    }

    public boolean wasClosedByRemote() {
        return closedByRemote;
    }
}
