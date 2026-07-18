package specs;

import config.ConfigManager;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;
import org.hamcrest.Matchers;

/** Common response assertions shared by every REST test: status ranges and SLA. */
public final class ResponseSpecFactory {

    private ResponseSpecFactory() {
    }

    /** Generic 2xx + response-time SLA spec, suitable as a default for happy-path tests. */
    public static ResponseSpecification ok() {
        int slaMs = ConfigManager.getInstance().restSlaMs();
        return new ResponseSpecBuilder()
                .expectStatusCode(Matchers.allOf(Matchers.greaterThanOrEqualTo(200), Matchers.lessThan(300)))
                .expectResponseTime(Matchers.lessThan((long) slaMs))
                .build();
    }

    public static ResponseSpecification withStatus(int statusCode) {
        int slaMs = ConfigManager.getInstance().restSlaMs();
        return new ResponseSpecBuilder()
                .expectStatusCode(statusCode)
                .expectResponseTime(Matchers.lessThan((long) slaMs))
                .build();
    }

    public static ResponseSpecification withStatusRange(int inclusiveMin, int exclusiveMax) {
        return new ResponseSpecBuilder()
                .expectStatusCode(Matchers.allOf(
                        Matchers.greaterThanOrEqualTo(inclusiveMin),
                        Matchers.lessThan(exclusiveMax)))
                .build();
    }
}
