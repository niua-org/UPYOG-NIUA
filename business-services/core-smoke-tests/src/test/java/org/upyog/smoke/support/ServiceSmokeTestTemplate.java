package org.upyog.smoke.support;

import io.restassured.response.Response;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ServiceSmokeTestTemplate {

    private final String serviceName;
    private ServiceDefinition service;
    private String accessToken;

    protected ServiceSmokeTestTemplate(String serviceName) {
        this.serviceName = serviceName;
    }

    @BeforeAll
    void loadServiceAndAuth() {
        Assumptions.assumeTrue(ServiceFilter.matches(serviceName),
                () -> "Skipped by SERVICE filter: " + SmokeConfig.serviceFilter());
        List<ServiceDefinition> defs = ServiceRegistry.loadFiltered();
        this.service = defs.stream()
                .filter(d -> d.getName().equals(serviceName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Service not in filter: " + serviceName));

        String user = SmokeConfig.optional("smoke.user", "SMOKE_USER", "");
        String password = SmokeConfig.optional("smoke.password", "SMOKE_PASSWORD", "");
        String tokenFromEnv = SmokeConfig.optional("smoke.token", "SMOKE_TOKEN", "");
        if (!tokenFromEnv.isBlank()) {
            accessToken = tokenFromEnv;
        } else if (!user.isBlank() && !password.isBlank()) {
            accessToken = new AuthClient(SmokeConfig.baseUrl()).fetchAccessToken(
                    user, password, SmokeConfig.smokeTenantId());
        }
    }

    @Test
    void health_isUp_withRetry() {
        SmokePolling.untilTrue(() -> {
            Response response = given()
                    .spec(RequestSpecFactory.base())
                    .when()
                    .get(service.getHealthPath())
                    .then()
                    .extract()
                    .response();
            return response.statusCode() == 200
                    && response.getBody().asString().toUpperCase().contains("UP");
        }, serviceName + " health UP");
    }

    @Test
    void version_matchesExpected_whenConfigured() {
        Optional<String> expected = SmokeConfig.expectedVersion();
        if (expected.isEmpty()) {
            return;
        }
        Response response = given()
                .spec(RequestSpecFactory.base())
                .when()
                .get(service.getVersionPath())
                .then()
                .statusCode(200)
                .extract()
                .response();
        assertThat(response.getBody().asString()).contains(expected.get());
    }

    @Test
    void auth_missingToken_returns401_onProtectedRead() {
        if (service.getCoreRead() == null || accessToken == null) {
            return;
        }
        Map<String, Object> coreRead = service.getCoreRead();
        String path = (String) coreRead.get("path");
        given()
                .spec(RequestSpecFactory.base())
                .body(Map.of("RequestInfo", TestDataFactory.minimalRequestInfo()))
                .when()
                .post(path)
                .then()
                .statusCode(401);
    }

    @Test
    void auth_validToken_coreRead_returns200OrEmptyContract() {
        if (service.getCoreRead() == null || accessToken == null) {
            return;
        }
        Map<String, Object> coreRead = service.getCoreRead();
        String path = (String) coreRead.get("path");
        @SuppressWarnings("unchecked")
        Map<String, String> queryParams = (Map<String, String>) coreRead.get("queryParams");
        Map<String, String> resolvedQuery = new HashMap<>();
        if (queryParams != null) {
            queryParams.forEach((k, v) -> resolvedQuery.put(k, v.replace("${SMOKE_TENANT_ID}", SmokeConfig.smokeTenantId())));
        }

        Response response = given()
                .spec(RequestSpecFactory.authenticated(accessToken))
                .queryParams(resolvedQuery)
                .body(Map.of("RequestInfo", TestDataFactory.minimalRequestInfo()))
                .when()
                .post(path)
                .then()
                .extract()
                .response();

        assertThat(response.statusCode()).isIn(200, 201);
        assertThat(response.getBody().asString()).isNotBlank();
    }

    @Test
    void invalidPayload_returns400_standardErrorShape() {
        if (service.getCoreRead() == null || accessToken == null) {
            return;
        }
        String path = (String) service.getCoreRead().get("path");
        Response response = given()
                .spec(RequestSpecFactory.authenticated(accessToken))
                .body("{}")
                .when()
                .post(path)
                .then()
                .extract()
                .response();
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void tenantIsolation_otherTenant_returnsEmptyOr403() {
        if (service.getCoreRead() == null || accessToken == null) {
            return;
        }
        String path = (String) service.getCoreRead().get("path");
        Response response = given()
                .spec(RequestSpecFactory.authenticated(accessToken))
                .queryParam("tenantId", "smoke.isolation." + SmokeConfig.smokeTenantId())
                .body(Map.of("RequestInfo", TestDataFactory.minimalRequestInfo()))
                .when()
                .post(path)
                .then()
                .extract()
                .response();
        assertThat(response.statusCode()).isIn(200, 403);
        if (response.statusCode() == 200) {
            assertThat(response.getBody().asString()).doesNotContain("SMOKE_");
        }
    }
}
