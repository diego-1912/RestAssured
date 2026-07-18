package tests;

import config.ConfigManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import utils.CorrelationContext;
import utils.TestDataCleanup;

/**
 * Common setup/teardown shared by REST, GraphQL and WebSocket tests: starts a fresh
 * correlation id per test (propagated across all three API types via request headers),
 * puts it in the logging MDC, and drains any registered cleanup tasks afterwards.
 */
public abstract class BaseTest {

    protected static final Logger log = LoggerFactory.getLogger(BaseTest.class);

    @BeforeEach
    void baseSetUp(TestInfo testInfo) {
        String correlationId = CorrelationContext.startNew();
        MDC.put("correlationId", correlationId);
        MDC.put("env", ConfigManager.getInstance().environment().name());
        log.info("=== Starting test: {} (correlationId={}) ===", testInfo.getDisplayName(), correlationId);
    }

    @AfterEach
    void baseTearDown(TestInfo testInfo) {
        try {
            TestDataCleanup.runAll();
        } finally {
            log.info("=== Finished test: {} ===", testInfo.getDisplayName());
            CorrelationContext.clear();
            MDC.clear();
        }
    }
}
