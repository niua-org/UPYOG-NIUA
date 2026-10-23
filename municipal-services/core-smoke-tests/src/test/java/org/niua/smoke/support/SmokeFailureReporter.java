package org.niua.smoke.support;

import io.restassured.response.Response;

public final class SmokeFailureReporter {

    private SmokeFailureReporter() {
    }

    public static String formatFailure(String method, String url, Response response, String secretToMask) {
        String body = response.getBody() == null ? "" : response.getBody().asString();
        if (body.length() > 500) {
            body = body.substring(0, 500) + "...";
        }
        if (secretToMask != null) {
            body = body.replace(secretToMask, SmokeConfig.maskSecret(secretToMask));
        }
        String correlationId = response.getHeader("x-correlation-id");
        if (correlationId == null) {
            correlationId = response.getHeader("X-Correlation-ID");
        }
        return String.format(
                "Smoke HTTP failure%n  %s %s%n  status=%d%n  correlationId=%s%n  body=%s",
                method, url, response.statusCode(), correlationId, body);
    }
}
