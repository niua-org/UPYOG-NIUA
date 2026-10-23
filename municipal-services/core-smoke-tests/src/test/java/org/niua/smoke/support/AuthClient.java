package org.niua.smoke.support;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Obtains a user token from the platform user OAuth/password endpoint (configurable).
 */
public final class AuthClient {

    private static final String DEFAULT_TOKEN_PATH = "/user/oauth/token";

    private AuthClient() {
    }

    public static String obtainToken(RequestSpecFactory factory) {
        return SmokeConfig.authTokenOverride().orElseGet(() -> loginWithPassword(factory));
    }

    private static String loginWithPassword(RequestSpecFactory factory) {
        String tokenUrl = SmokeConfig.baseUrl() + optionalEnv("SMOKE_TOKEN_PATH", DEFAULT_TOKEN_PATH);
        Response response = given()
                .spec(factory.anonymous())
                .contentType(ContentType.URLENC)
                .formParams(Map.of(
                        "username", SmokeConfig.smokeUser(),
                        "password", SmokeConfig.smokePassword(),
                        "grant_type", "password",
                        "scope", "read",
                        "tenantId", SmokeConfig.smokeTenantId(),
                        "userType", optionalEnv("SMOKE_USER_TYPE", "EMPLOYEE")
                ))
                .post(tokenUrl)
                .then()
                .extract()
                .response();

        if (response.statusCode() != 200) {
            throw new IllegalStateException(SmokeFailureReporter.formatFailure(
                    "POST", tokenUrl, response, SmokeConfig.smokePassword()));
        }
        return response.jsonPath().getString("access_token");
    }

    private static String optionalEnv(String key, String defaultValue) {
        return SmokeConfig.authTokenOverride().isPresent() ? defaultValue
                : System.getenv().getOrDefault(key, defaultValue);
    }
}
