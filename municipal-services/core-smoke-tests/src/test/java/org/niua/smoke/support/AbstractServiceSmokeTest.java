package org.niua.smoke.support;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;

@Tag("smoke")
public abstract class AbstractServiceSmokeTest {

    protected static RequestSpecFactory specFactory;
    protected static String authToken;
    protected ServiceDefinition service;

    protected abstract ServiceDefinition serviceDefinition();

    @BeforeAll
    static void globalSetup() {
        SmokeConfig.environment();
        specFactory = new RequestSpecFactory(SmokeConfig.baseUrl());
        authToken = AuthClient.obtainToken(specFactory);
    }

    @BeforeEach
    void loadService() {
        service = serviceDefinition();
    }

    @Test
    void health_up_retriesWarmUp() {
        String url = service.serviceUrl(SmokeConfig.baseUrl()) + service.getHealthPath();
        Polling.untilTrue(() -> {
            Response r = given().spec(specFactory.anonymous()).get(url).then().extract().response();
            return r.statusCode() == 200 && r.getBody().asString().toUpperCase().contains("UP");
        }, SmokeConfig.healthWarmUp());
    }

    @Test
    void version_matchesExpectedWhenProvided() {
        SmokeConfig.expectedVersion().ifPresent(expected -> {
            String url = service.serviceUrl(SmokeConfig.baseUrl()) + service.getVersionPath();
            Response response = given().spec(specFactory.anonymous()).get(url).then().extract().response();
            assertThat(response.statusCode()).isEqualTo(200);
            String body = response.getBody().asString();
            assertThat(body).contains(expected);
        });
    }

    @Test
    void auth_missingToken_returns401() {
        service.getCoreRead().ifPresent(endpoint -> {
            String url = service.serviceUrl(SmokeConfig.baseUrl()) + endpoint.path();
            given().spec(specFactory.anonymous())
                    .body(minimalReadBody())
                    .post(url)
                    .then()
                    .statusCode(anyOf(equalTo(401), equalTo(403)));
        });
    }

    @Test
    void auth_invalidToken_returns401() {
        service.getCoreRead().ifPresent(endpoint -> {
            String url = service.serviceUrl(SmokeConfig.baseUrl()) + endpoint.path();
            given().spec(specFactory.authenticated("invalid-smoke-token"))
                    .body(minimalReadBody())
                    .post(url)
                    .then()
                    .statusCode(anyOf(equalTo(401), equalTo(403)));
        });
    }

    @Test
    void coreRead_validToken_returns200WithContract() {
        service.getCoreRead().ifPresent(endpoint -> {
            String url = service.serviceUrl(SmokeConfig.baseUrl()) + endpoint.path();
            Response response = given()
                    .spec(specFactory.authenticated(authToken, TenantContext.smokeTenant()))
                    .body(minimalReadBody())
                    .post(url)
                    .then()
                    .extract()
                    .response();
            if (response.statusCode() >= 500) {
                throw new AssertionError(SmokeFailureReporter.formatFailure("POST", url, response, authToken));
            }
            assertThat(response.statusCode()).isBetween(200, 400);
            if (response.statusCode() == 200) {
                Object responseInfo = response.jsonPath().get("ResponseInfo");
                assertThat(responseInfo).isNotNull();
            }
        });
    }

    @Test
    void negative_invalidPayload_returns400WithErrorFormat() {
        service.getCoreWrite().ifPresent(endpoint -> {
            String url = service.serviceUrl(SmokeConfig.baseUrl()) + endpoint.path();
            Response response = given()
                    .spec(specFactory.authenticated(authToken, TenantContext.smokeTenant()))
                    .body("{}")
                    .post(url)
                    .then()
                    .extract()
                    .response();
            assertThat(response.statusCode()).isBetween(400, 422);
            String body = response.getBody().asString();
            assertThat(body).containsAnyOf("Errors", "error", "message", "CustomException");
        });
    }

    @Test
    void tenantIsolation_smokeTenantNotVisibleFromOtherTenant() {
        service.getCoreRead().ifPresent(endpoint -> {
            String url = service.serviceUrl(SmokeConfig.baseUrl()) + endpoint.path();
            Response response = given()
                    .spec(specFactory.authenticated(authToken, TenantContext.isolationTenant()))
                    .body(minimalReadBodyForTenant(TenantContext.smokeTenant()))
                    .post(url)
                    .then()
                    .extract()
                    .response();
            if (response.statusCode() == 200) {
                String body = response.getBody().asString();
                assertThat(body).doesNotContain(TenantContext.smokeDataPrefix());
            }
        });
    }

    protected Object minimalReadBody() {
        return minimalReadBodyForTenant(TenantContext.smokeTenant());
    }

    protected Object minimalReadBodyForTenant(String tenantId) {
        return TestDataFactory.requestInfoWrapper(tenantId);
    }
}
