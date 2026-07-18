package clients.rest.filters;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Retries idempotent requests (GET/HEAD/OPTIONS/PUT/DELETE by default) that fail with a
 * transient status code (429, 502, 503, 504) or throw an I/O exception, using exponential
 * backoff. POST is excluded by default since it's typically non-idempotent; pass a custom
 * method set to override.
 */
public class RetryFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RetryFilter.class);
    private static final Set<Integer> DEFAULT_RETRYABLE_STATUS = Set.of(429, 502, 503, 504);
    private static final Set<String> DEFAULT_RETRYABLE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "PUT", "DELETE");

    private final int maxAttempts;
    private final long initialBackoffMs;
    private final double backoffMultiplier;
    private final Set<Integer> retryableStatusCodes;
    private final Set<String> retryableMethods;

    public RetryFilter(int maxAttempts, long initialBackoffMs, double backoffMultiplier) {
        this(maxAttempts, initialBackoffMs, backoffMultiplier, DEFAULT_RETRYABLE_STATUS, DEFAULT_RETRYABLE_METHODS);
    }

    public RetryFilter(int maxAttempts, long initialBackoffMs, double backoffMultiplier,
                        Set<Integer> retryableStatusCodes, Set<String> retryableMethods) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.initialBackoffMs = initialBackoffMs;
        this.backoffMultiplier = backoffMultiplier;
        this.retryableStatusCodes = retryableStatusCodes;
        this.retryableMethods = retryableMethods;
    }

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                            FilterableResponseSpecification responseSpec,
                            FilterContext ctx) {
        if (!retryableMethods.contains(requestSpec.getMethod().toUpperCase())) {
            return ctx.next(requestSpec, responseSpec);
        }

        long backoff = initialBackoffMs;
        RuntimeException lastError = null;
        Response response = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                response = ctx.next(requestSpec, responseSpec);
                if (!retryableStatusCodes.contains(response.getStatusCode()) || attempt == maxAttempts) {
                    return response;
                }
                log.warn("Retryable status {} on {} {} (attempt {}/{}), backing off {}ms",
                        response.getStatusCode(), requestSpec.getMethod(), requestSpec.getURI(),
                        attempt, maxAttempts, backoff);
            } catch (RuntimeException e) {
                lastError = e;
                if (attempt == maxAttempts) {
                    throw e;
                }
                log.warn("Request error on {} {} (attempt {}/{}): {} - backing off {}ms",
                        requestSpec.getMethod(), requestSpec.getURI(), attempt, maxAttempts, e.getMessage(), backoff);
            }

            sleep(backoff);
            backoff = (long) (backoff * backoffMultiplier);
        }

        if (response != null) {
            return response;
        }
        throw lastError;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during retry backoff", e);
        }
    }
}
