package org.upyog.mcp.web;

/**
 * Headers set by the UPYOG API gateway on the hop chatbot → gateway → MCP server.
 */
public final class GatewayIdentityHeaders {

    public static final String PASS_THROUGH = "x-pass-through-gateway";
    public static final String USER_INFO = "x-user-info";
    public static final String AUTH_TOKEN = "Auth-Token";

    private GatewayIdentityHeaders() {
    }
}
