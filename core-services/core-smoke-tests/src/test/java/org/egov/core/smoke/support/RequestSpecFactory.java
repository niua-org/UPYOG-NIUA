package org.egov.core.smoke.support;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.RequestSpecification;

import java.util.regex.Pattern;

/**
 * Shared REST Assured specification. Timeouts are explicit. Failure logs include
 * method, URL, status, a short body excerpt, and correlation id. Secrets are masked.
 */
public final class RequestSpecFactory {

    public static final int CONNECT_TIMEOUT_MS = 10_000;
    public static final int READ_TIMEOUT_MS = 20_000;
    public static final String CORRELATION_HEADER = "X-Correlation-Id";

    private static final int BODY_EXCERPT = 400;
    private static final Pattern SECRET = Pattern.compile(
            "(?i)(authorization\\s*[:=]\\s*)(bearer\\s+)?[^\\s,;\"']+|(password|token|access_token|authToken)(\"?\\s*[:=]\\s*\"?)[^\\s,\"'}]+");

    private RequestSpecFactory() {
    }

    public static RequestSpecification spec(SmokeConfig config) {
        return new RequestSpecBuilder()
                .setBaseUri(config.baseUrl())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addHeader(CORRELATION_HEADER, correlationId())
                .setConfig(RestAssuredConfig.config().httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", CONNECT_TIMEOUT_MS)
                        .setParam("http.socket.timeout", READ_TIMEOUT_MS)))
                .addFilter(new FailureLogFilter())
                .log(LogDetail.URI)
                .build();
    }

    public static String correlationId() {
        return "smoke-" + System.currentTimeMillis();
    }

    public static String mask(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return SECRET.matcher(raw).replaceAll("$1$2$3$4***");
    }

    static final class FailureLogFilter implements Filter {
        @Override
        public Response filter(
                FilterableRequestSpecification requestSpec,
                FilterableResponseSpecification responseSpec,
                FilterContext ctx) {
            Response response = ctx.next(requestSpec, responseSpec);
            int status = response.getStatusCode();
            if (status >= 400) {
                String correlation = requestSpec.getHeaders().hasHeaderWithName(CORRELATION_HEADER)
                        ? requestSpec.getHeaders().getValue(CORRELATION_HEADER)
                        : "";
                String body = response.getBody() == null ? "" : response.getBody().asString();
                if (body.length() > BODY_EXCERPT) {
                    body = body.substring(0, BODY_EXCERPT) + "...";
                }
                System.err.println(mask(String.format(
                        "SMOKE_HTTP_FAILURE method=%s url=%s status=%d correlation=%s body=%s",
                        requestSpec.getMethod(),
                        requestSpec.getURI(),
                        status,
                        correlation,
                        body)));
            }
            return response;
        }
    }
}
