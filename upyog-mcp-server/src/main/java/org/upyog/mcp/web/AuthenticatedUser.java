package org.upyog.mcp.web;

import java.util.List;

/**
 * Caller identity loaded from egov-user {@code POST /user/_details}.
 * <p>
 * The access token is kept only so the server can place it in {@code RequestInfo.authToken}.
 * {@link #toString()} omits the token, mobile number, and name.
 */
public final class AuthenticatedUser {

    private final String uuid;
    private final String type;
    private final String tenantId;
    private final List<String> roleCodes;
    private final List<String> roleTenants;
    private final String authToken;

    /**
     * @param uuid        egov-user uuid from {@code /user/_details}
     * @param type        user type (for example CITIZEN, EMPLOYEE)
     * @param tenantId    primary tenant on the user record
     * @param roleCodes   role codes from the user profile
     * @param roleTenants tenant id bound to each role
     * @param authToken   session access token; used only when building {@code RequestInfo}
     */
    public AuthenticatedUser(String uuid, String type, String tenantId, List<String> roleCodes,
                             List<String> roleTenants, String authToken) {
        this.uuid = uuid;
        this.type = type;
        this.tenantId = tenantId;
        this.roleCodes = List.copyOf(roleCodes);
        this.roleTenants = List.copyOf(roleTenants);
        this.authToken = authToken;
    }

    /** @return egov-user uuid */
    public String getUuid() {
        return uuid;
    }

    /** @return user type from egov-user */
    public String getType() {
        return type;
    }

    /** @return primary tenant id on the user record */
    public String getTenantId() {
        return tenantId;
    }

    /** @return immutable list of role codes */
    public List<String> getRoleCodes() {
        return roleCodes;
    }

    /** @return immutable list of tenant ids associated with roles */
    public List<String> getRoleTenants() {
        return roleTenants;
    }

    /** @return UPYOG access token for gateway {@code RequestInfo.authToken} */
    public String getAuthToken() {
        return authToken;
    }

    /**
     * @param code role code to test (case-insensitive)
     * @return {@code true} when the user holds that role
     */
    public boolean hasRole(String code) {
        return roleCodes.stream().anyMatch(role -> role.equalsIgnoreCase(code));
    }

    @Override
    public String toString() {
        return "AuthenticatedUser{uuid=" + uuid + ", type=" + type + "}";
    }
}
