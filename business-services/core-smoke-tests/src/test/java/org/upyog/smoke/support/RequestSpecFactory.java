package org.upyog.smoke.support;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import java.util.concurrent.TimeUnit;

import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static io.restassured.config.RestAssuredConfig.config;

public final class RequestSpecFactory {

    private RequestSpecFactory() {
    }

    public static RequestSpecification authenticated(String token) {
        return base()
                .header("Authorization", "Bearer " + token);
    }

    public static RequestSpecification base() {
        return new RequestSpecBuilder()
                .setBaseUri(SmokeConfig.baseUrl())
                .setContentType(ContentType.JSON)
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .setConfig(config().httpClient(httpClientConfig()
                        .setParam("http.connection.timeout", (int) SmokeConfig.HTTP_TIMEOUT.toMillis())
                        .setParam("http.socket.timeout", (int) SmokeConfig.HTTP_TIMEOUT.toMillis())))
                .build();
    }
}
