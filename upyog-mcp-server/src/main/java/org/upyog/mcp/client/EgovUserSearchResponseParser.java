package org.upyog.mcp.client;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Parses {@code UserRes} from {@code POST /user/_search} per egov-user runtime JSON.
 */
public final class EgovUserSearchResponseParser {

    private EgovUserSearchResponseParser() {
    }

    /** @return first user object from search response, or missing node if not found */
    public static JsonNode firstUser(JsonNode response) {
        JsonNode users = response.path(EgovUserContracts.SEARCH_RESPONSE_USERS);
        if (!users.isArray() || users.isEmpty()) {
            users = response.path(EgovUserContracts.SEARCH_RESPONSE_USERS_SWAGGER);
        }
        if (!users.isArray() || users.isEmpty()) {
            users = response.path(EgovUserContracts.SEARCH_RESPONSE_USERS_LEGACY);
        }
        if (!users.isArray() || users.isEmpty()) {
            return response.path("missing");
        }
        return users.get(0);
    }
}
