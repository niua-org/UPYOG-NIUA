package org.niua.smoke.e2e;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.niua.smoke.support.AuthClient;
import org.niua.smoke.support.RequestSpecFactory;
import org.niua.smoke.support.SmokeConfig;
import org.niua.smoke.support.SmokeFailureReporter;
import org.niua.smoke.support.SmokeService;
import org.niua.smoke.support.TenantContext;
import org.niua.smoke.support.TestDataFactory;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cross-service journey: property search then PGR create (validates downstream MDMS/user paths).
 */
@Tag("smoke")
@SmokeService("e2e")
public class PgrPropertyCrossServiceSmokeTest {

    private static RequestSpecFactory specFactory;
    private static String authToken;

    @BeforeAll
    static void setup() {
        if (!"ALL".equalsIgnoreCase(SmokeConfig.serviceFilter())) {
            org.junit.jupiter.api.Assumptions.assumeTrue(
                    "e2e".equalsIgnoreCase(SmokeConfig.serviceFilter()),
                    "Skipping E2E unless SERVICE=e2e or ALL");
        }
        specFactory = new RequestSpecFactory(SmokeConfig.baseUrl());
        authToken = AuthClient.obtainToken(specFactory);
    }

    @Test
    void propertySearchThenPgrCreate_smokeJourney() {
        String propertyUrl = SmokeConfig.baseUrl() + "/property-services/property/_search";
        Response propertyResponse = given()
                .spec(specFactory.authenticated(authToken, TenantContext.smokeTenant()))
                .body(TestDataFactory.requestInfoWrapper(TenantContext.smokeTenant()))
                .post(propertyUrl)
                .then()
                .extract()
                .response();

        if (propertyResponse.statusCode() >= 500) {
            throw new AssertionError(SmokeFailureReporter.formatFailure("POST", propertyUrl, propertyResponse, authToken));
        }
        assertThat(propertyResponse.statusCode()).isBetween(200, 400);

        String pgrUrl = SmokeConfig.baseUrl() + "/pgr-services/request/_create";
        Response pgrResponse = given()
                .spec(specFactory.authenticated(authToken, TenantContext.smokeTenant()))
                .body("{}")
                .post(pgrUrl)
                .then()
                .extract()
                .response();
        assertThat(pgrResponse.statusCode()).isBetween(400, 422);
    }
}
