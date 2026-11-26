package org.niua.smoke.support;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

public final class RequestSpecFactory {

    private final String baseUrl;

    public RequestSpecFactory(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public RequestSpecification anonymous() {
        return new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setContentType(ContentType.JSON)
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .build();
    }

    public RequestSpecification authenticated(String token) {
        return new RequestSpecBuilder()
                .addRequestSpecification(anonymous())
                .addHeader("Authorization", "Bearer " + token)
                .build();
    }

    public RequestSpecification authenticated(String token, String tenantId) {
        return new RequestSpecBuilder()
                .addRequestSpecification(authenticated(token))
                .addHeader("tenantId", tenantId)
                .build();
    }
}
