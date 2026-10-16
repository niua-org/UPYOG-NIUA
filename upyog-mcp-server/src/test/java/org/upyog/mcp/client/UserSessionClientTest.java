package org.upyog.mcp.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.upyog.mcp.config.McpProperties;
import org.upyog.mcp.web.AuthenticatedUser;

import java.net.http.HttpClient;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.notContaining;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UserSessionClientTest {

    @Test
    void resolveUsesUiShapedUserSearchBody() throws Exception {
        var server = new com.github.tomakehurst.wiremock.WireMockServer(wireMockConfig().dynamicPort());
        server.start();
        com.github.tomakehurst.wiremock.client.WireMock.configureFor("localhost", server.port());
        server.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlPathEqualTo("/user/_search"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("""
                        {"responseInfo":{"status":"200"},"user":[{"uuid":"u-1","type":"CITIZEN","tenantId":"pg","roles":[{"code":"CITIZEN","tenantId":"pg"}]}]}
                        """)));

        ObjectMapper objectMapper = new ObjectMapper();
        McpProperties properties = new McpProperties();
        properties.setGatewayBaseUrl("http://localhost:" + server.port());
        properties.setRequestInfoApiId("Rainmaker");
        properties.setUserSessionTenantId("pg");
        HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(2)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(5));
        RestClient client = RestClient.builder().baseUrl(properties.getGatewayBaseUrl()).requestFactory(factory).build();
        RestGatewayClient gateway = new RestGatewayClient(client, client, objectMapper, CircuitBreakerRegistry.ofDefaults());
        UserSessionClient sessionClient = new UserSessionClient(gateway, properties, objectMapper);

        var userInfo = objectMapper.readTree("""
                {"id":811,"uuid":"u-1","userName":"8010012414","type":"CITIZEN","tenantId":"pg","roles":[{"code":"CITIZEN","tenantId":"pg"}],"active":true}
                """);
        AuthenticatedUser user = sessionClient.resolve("session-token-abc", "u-1", userInfo);
        assertEquals("u-1", user.getUuid());

        verify(postRequestedFor(urlPathEqualTo("/user/_search"))
                .withHeader("Auth-Token", absent())
                .withRequestBody(containing("\"pageSize\":\"100\""))
                .withRequestBody(containing("\"tenantId\":\"pg\""))
                .withRequestBody(containing("8010012414"))
                .withRequestBody(containing("|en_IN")));
        server.stop();
    }

    @Test
    void resolveMatchesCitizenUiBodyWithoutUserInfo() throws Exception {
        var server = new com.github.tomakehurst.wiremock.WireMockServer(wireMockConfig().dynamicPort());
        server.start();
        com.github.tomakehurst.wiremock.client.WireMock.configureFor("localhost", server.port());
        server.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlPathEqualTo("/user/_search"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("""
                        {"responseInfo":{"status":"200"},"user":[{"uuid":"u-2","type":"CITIZEN","tenantId":"pg","roles":[{"code":"CITIZEN","tenantId":"pg"}]}]}
                        """)));

        ObjectMapper objectMapper = new ObjectMapper();
        McpProperties properties = new McpProperties();
        properties.setGatewayBaseUrl("http://localhost:" + server.port());
        properties.setRequestInfoApiId("Rainmaker");
        properties.setUserSessionTenantId("pg");
        HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(2)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(5));
        RestClient client = RestClient.builder().baseUrl(properties.getGatewayBaseUrl()).requestFactory(factory).build();
        RestGatewayClient gateway = new RestGatewayClient(client, client, objectMapper, CircuitBreakerRegistry.ofDefaults());
        UserSessionClient sessionClient = new UserSessionClient(gateway, properties, objectMapper);

        AuthenticatedUser user = sessionClient.resolve("216b04af-cfe7-4642-b4b4-2024048a51fb", "u-2", null);
        assertEquals("u-2", user.getUuid());

        verify(postRequestedFor(urlPathEqualTo("/user/_search"))
                .withHeader("Auth-Token", absent())
                .withRequestBody(containing("\"authToken\":\"216b04af-cfe7-4642-b4b4-2024048a51fb\""))
                .withRequestBody(containing("\"uuid\":[\"u-2\"]"))
                // userInfo must always be present (egov-user returns 500 without it)
                .withRequestBody(containing("\"userInfo\""))
                .withRequestBody(containing("\"uuid\":\"u-2\""))
                .withRequestBody(containing("\"code\":\"CITIZEN\"")));
        server.stop();
    }

    @Test
    void normalizeStripsBearerPrefix() {
        assertEquals("abc", UserSessionClient.normalizeToken("Bearer abc"));
    }
}
