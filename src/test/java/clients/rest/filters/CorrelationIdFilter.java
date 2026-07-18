package clients.rest.filters;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import utils.CorrelationContext;

/**
 * Stamps every outgoing REST call with the current test's correlation id so it can be
 * cross-referenced against GraphQL calls and WebSocket push messages in the same flow.
 */
public class CorrelationIdFilter implements Filter {

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                            FilterableResponseSpecification responseSpec,
                            FilterContext ctx) {
        requestSpec.header(CorrelationContext.HEADER_NAME, CorrelationContext.current());
        return ctx.next(requestSpec, responseSpec);
    }
}
