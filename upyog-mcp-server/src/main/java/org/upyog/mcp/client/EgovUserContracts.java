package org.upyog.mcp.client;

/**
 * Gateway-relative egov-user API mapping.
 * <p>
 * Swagger source: {@code core-services/docs/egov-user-contract.yml} (also under
 * {@code src/main/resources/spec/egov-user-contract.yml}). The contract {@code basePath}
 * ({@code /users/v110}) is legacy; UPYOG gateway routes use {@code /user/**}.
 */
public final class EgovUserContracts {

    /** Gateway prefix for egov-user (see gateway {@code routes.properties}). */
    public static final String GATEWAY_MODULE_PREFIX = "/user";

    /** Contract path {@code POST /_search} → {@code POST /user/_search}. */
    public static final String SEARCH_PATH = GATEWAY_MODULE_PREFIX + "/_search";

    /** Token lookup used inside the gateway and on internal auth host. */
    public static final String DETAILS_PATH = GATEWAY_MODULE_PREFIX + "/_details";

    /** Contract {@code UserRes.Users}; runtime {@link org.egov.user.web.contract.UserSearchResponse} serializes as {@code user}. */
    public static final String SEARCH_RESPONSE_USERS = "user";

    /** Alternate key from older clients. */
    public static final String SEARCH_RESPONSE_USERS_LEGACY = "userSearchResponseContent";

    /** Contract {@code UserRes} / swagger {@code Users} (capital U). */
    public static final String SEARCH_RESPONSE_USERS_SWAGGER = "Users";

    private EgovUserContracts() {
    }
}
