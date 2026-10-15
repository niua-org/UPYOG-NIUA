package org.upyog.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Runtime settings for gateway access, confirmation tokens, limits, and Redis.
 * Secrets come from the environment ({@code UPYOG_MCP_TOKEN_SECRET}, {@code REDIS_URI}).
 */
@ConfigurationProperties(prefix = "upyog.mcp")
public class McpProperties {

    private String gatewayBaseUrl = "https://niuatt.niua.in";
    private String uiBaseUrl = "https://niuatt.niua.in";
    private String tokenSecret = "";
    private Duration confirmationTtl = Duration.ofMinutes(5);
    private Duration readTimeout = Duration.ofSeconds(8);
    private Duration writeTimeout = Duration.ofSeconds(20);
    private Duration connectTimeout = Duration.ofSeconds(3);
    private int maxPageSize = 20;
    private int maxResponseChars = 20_000;
    private int maxFreeTextChars = 1_000;
    private int maxUploadBytes = 2_000_000;
    private int rateLimitPerMinute = 60;
    /**
     * When true, MCP trusts {@code x-pass-through-gateway} and {@code x-user-info} from the API gateway
     * and does not call {@code POST /user/_search} on the inbound MCP hop.
     */
    private boolean trustGatewayIdentity = true;
    /** {@code RequestInfo.apiId} on gateway calls (UI uses {@code Rainmaker}). */
    private String requestInfoApiId = "Rainmaker";
    /** State tenant on {@code POST /user/_search} (for example {@code pg} on NIUA UAT). */
    private String userSessionTenantId = "pg";
    private Redis redis = new Redis();
    private List<String> allowedGatewayPrefixes = new ArrayList<>();

    public String getGatewayBaseUrl() {
        return gatewayBaseUrl;
    }

    public void setGatewayBaseUrl(String gatewayBaseUrl) {
        this.gatewayBaseUrl = gatewayBaseUrl;
    }

    public String getUiBaseUrl() {
        return uiBaseUrl;
    }

    public void setUiBaseUrl(String uiBaseUrl) {
        this.uiBaseUrl = uiBaseUrl;
    }

    public String getTokenSecret() {
        return tokenSecret;
    }

    public void setTokenSecret(String tokenSecret) {
        this.tokenSecret = tokenSecret;
    }

    public Duration getConfirmationTtl() {
        return confirmationTtl;
    }

    public void setConfirmationTtl(Duration confirmationTtl) {
        this.confirmationTtl = confirmationTtl;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Duration getWriteTimeout() {
        return writeTimeout;
    }

    public void setWriteTimeout(Duration writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    public int getMaxResponseChars() {
        return maxResponseChars;
    }

    public void setMaxResponseChars(int maxResponseChars) {
        this.maxResponseChars = maxResponseChars;
    }

    public int getMaxFreeTextChars() {
        return maxFreeTextChars;
    }

    public void setMaxFreeTextChars(int maxFreeTextChars) {
        this.maxFreeTextChars = maxFreeTextChars;
    }

    public int getMaxUploadBytes() {
        return maxUploadBytes;
    }

    public void setMaxUploadBytes(int maxUploadBytes) {
        this.maxUploadBytes = maxUploadBytes;
    }

    public int getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(int rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public boolean isTrustGatewayIdentity() {
        return trustGatewayIdentity;
    }

    public void setTrustGatewayIdentity(boolean trustGatewayIdentity) {
        this.trustGatewayIdentity = trustGatewayIdentity;
    }

    public String getRequestInfoApiId() {
        return requestInfoApiId;
    }

    public void setRequestInfoApiId(String requestInfoApiId) {
        this.requestInfoApiId = requestInfoApiId;
    }

    public String getUserSessionTenantId() {
        return userSessionTenantId;
    }

    public void setUserSessionTenantId(String userSessionTenantId) {
        this.userSessionTenantId = userSessionTenantId;
    }

    public Redis getRedis() {
        return redis;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    public List<String> getAllowedGatewayPrefixes() {
        return allowedGatewayPrefixes;
    }

    public void setAllowedGatewayPrefixes(List<String> allowedGatewayPrefixes) {
        this.allowedGatewayPrefixes = allowedGatewayPrefixes;
    }

    /** Redis settings for single-use confirmation tokens and idempotent write results. */
    public static class Redis {
        /** When false, {@link org.upyog.mcp.guard.RejectingSingleUseStore} blocks {@code confirm_action}. */
        private boolean enabled;
        /** Lettuce connection URI ({@code REDIS_URI} / {@code upyog.mcp.redis.uri}). */
        private String uri = "redis://localhost:6379";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }
    }
}
