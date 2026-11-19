package org.egov.core.smoke.support;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.Optional;

/**
 * Resolves a bearer token from {@code SMOKE_TOKEN}, or from
 * {@code SMOKE_USER} + {@code SMOKE_PASSWORD} posted to {@code SMOKE_AUTH_PATH}.
 * The path is required from the environment. This class does not embed a URL.
 */
public final class AuthClient {

    private final SmokeConfig config;

    private AuthClient(SmokeConfig config) {
        this.config = config;
    }

    public static AuthClient from(SmokeConfig config) {
        return new AuthClient(config);
    }

    public String accessToken() {
        if (config.hasStaticToken()) {
            return config.token().orElseThrow();
        }
        String authPath = optional("SMOKE_AUTH_PATH").orElse("");
        if (authPath.isBlank()) {
            throw new IllegalStateException(
                    "SMOKE_TOKEN is unset and SMOKE_AUTH_PATH is unset; refusing to guess a token URL");
        }
        String userType = optional("SMOKE_USER_TYPE").orElse("");
        if (userType.isBlank()) {
            throw new IllegalStateException(
                    "Password grant requires SMOKE_USER_TYPE (the token endpoint rejects a missing userType)");
        }
        Response response = io.restassured.RestAssured.given()
                .spec(RequestSpecFactory.spec(config))
                .contentType(ContentType.URLENC)
                .formParam("grant_type", "password")
                .formParam("username", config.user().orElseThrow())
                .formParam("password", config.password().orElseThrow())
                .formParam("tenantId", config.tenantId())
                .formParam("userType", userType)
                .post(authPath.startsWith("/") ? authPath : "/" + authPath);
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Token request failed with status " + response.statusCode());
        }
        String token = response.jsonPath().getString("access_token");
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Token response did not contain access_token");
        }
        return token;
    }

    private static Optional<String> optional(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            value = System.getProperty(name);
        }
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }
}
