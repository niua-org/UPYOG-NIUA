package org.upyog.mcp.service;

/**
 * Business error returned to the assistant. The message is safe to show.
 * Stack traces, downstream bodies, and internal URLs stay on the server.
 */
public class McpException extends RuntimeException {

    private final String code;
    private final boolean retryable;
    private final String suggestedNextStep;

    /**
     * @param code               stable machine code returned to the assistant
     * @param message            citizen-safe text
     * @param retryable          whether the assistant may retry the same read
     * @param suggestedNextStep  guidance for the next user-facing question
     */
    public McpException(String code, String message, boolean retryable, String suggestedNextStep) {
        super(message);
        this.code = code;
        this.retryable = retryable;
        this.suggestedNextStep = suggestedNextStep;
    }

    /** @return error code (for example {@code TENANT_DENIED} is mapped to {@code INVALID_INPUT} at the tool layer) */
    public String getCode() {
        return code;
    }

    /** @return whether the assistant should retry without changing business fields */
    public boolean isRetryable() {
        return retryable;
    }

    /** @return next step phrasing safe to show or paraphrase to the user */
    public String getSuggestedNextStep() {
        return suggestedNextStep;
    }
}
