package org.upyog.smoke.support;

public class SmokeAssertionException extends RuntimeException {

    private final String request;
    private final int status;
    private final String bodyExcerpt;
    private final String correlationId;

    public SmokeAssertionException(String message, String request, int status, String bodyExcerpt, String correlationId) {
        super(formatMessage(message, request, status, bodyExcerpt, correlationId));
        this.request = request;
        this.status = status;
        this.bodyExcerpt = bodyExcerpt;
        this.correlationId = correlationId;
    }

    private static String formatMessage(String message, String request, int status, String bodyExcerpt, String correlationId) {
        return message + " | request=" + request
                + " | status=" + status
                + " | body=" + bodyExcerpt
                + " | correlationId=" + (correlationId == null ? "n/a" : correlationId);
    }
}
