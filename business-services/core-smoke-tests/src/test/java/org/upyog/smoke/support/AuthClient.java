package org.upyog.smoke.support;

import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public class AuthClient {

    private final String userServiceBase;

    public AuthClient(String baseUrl) {
        this.userServiceBase = baseUrl + "/user/oauth/token";
    }

    public String fetchAccessToken(String username, String password, String tenantId) {
        Response response = given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("username", username)
                .formParam("password", password)
                .formParam("grant_type", "password")
                .formParam("scope", "read")
                .formParam("tenantId", tenantId)
                .when()
                .post(userServiceBase)
                .then()
                .extract()
                .response();

        if (response.statusCode() != 200) {
            throw new SmokeAssertionException(
                    "Auth token request failed",
                    "POST " + userServiceBase,
                    response.statusCode(),
                    excerpt(response),
                    correlationId(response));
        }
        return response.jsonPath().getString("access_token");
    }

    private static String excerpt(Response response) {
        String body = response.getBody().asString();
        if (body == null) {
            return "";
        }
        return body.length() > 500 ? body.substring(0, 500) + "..." : body;
    }

    private static String correlationId(Response response) {
        String header = response.getHeader("x-correlation-id");
        return header != null ? header : response.getHeader("X-Correlation-ID");
    }
}
