package org.upyog.mcp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.upyog.mcp.client.RequestInfoBuilder;
import org.upyog.mcp.client.RestGatewayClient;
import org.upyog.mcp.config.McpProperties;
import org.upyog.mcp.guard.ConfirmationTokenService;
import org.upyog.mcp.guard.SingleUseStore;
import org.upyog.mcp.guard.TenantValidator;
import org.upyog.mcp.mapper.PiiMasker;
import org.upyog.mcp.mapper.ResponseProjector;
import org.upyog.mcp.mapper.SchemaValidator;
import org.upyog.mcp.registry.BodyBuilder;
import org.upyog.mcp.registry.DescriptorLoader;
import org.upyog.mcp.registry.DescriptorRegistry;
import org.upyog.mcp.web.AuthenticatedUser;
import org.upyog.mcp.web.UserContextHolder;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PgrSearchAndPrepareTest {

    private final com.github.tomakehurst.wiremock.WireMockServer server =
            new com.github.tomakehurst.wiremock.WireMockServer(wireMockConfig().dynamicPort());

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
        if (server.isRunning()) {
            server.stop();
        }
    }

    @Test
    void searchSendsRequestInfoAndMasksResponse() throws Exception {
        server.start();
        com.github.tomakehurst.wiremock.client.WireMock.configureFor("localhost", server.port());
        server.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlPathEqualTo("/pgr-services/v2/request/_search"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("""
                        {"ServiceWrappers":[{"service":{"serviceRequestId":"PGR-1","applicationStatus":"PENDINGFORASSIGNMENT","serviceCode":"Garbage","description":"overflow","tenantId":"pg.citya","citizen":{"mobileNumber":"9999999999"}}}]}
                        """)));
        McpOperations operations = operations(server.port());
        UserContextHolder.set(citizen());
        Map<String, Object> result = operations.search("pgr", new ObjectMapper().readTree("""
                {"tenantId":"pg.citya","serviceRequestId":"PGR-1"}
                """), 0, 10);
        String json = new ObjectMapper().writeValueAsString(result);
        assertTrue(json.contains("PGR-1"));
        assertFalse(json.contains("9999999999"));
        verify(postRequestedFor(urlPathEqualTo("/pgr-services/v2/request/_search"))
                .withQueryParam("tenantId", equalTo("pg.citya"))
                .withRequestBody(containing("\"authToken\":\"token-1\""))
                .withHeader("x-correlation-id", containing("corr-1")));
    }

    @Test
    void prepareDoesNotCallCreate() throws Exception {
        server.start();
        com.github.tomakehurst.wiremock.client.WireMock.configureFor("localhost", server.port());
        AtomicInteger posts = new AtomicInteger();
        server.addMockServiceRequestListener((request, response) -> posts.incrementAndGet());
        McpOperations operations = operations(server.port());
        UserContextHolder.set(citizen());
        Map<String, Object> prepared = operations.prepare("pgr", "create", new ObjectMapper().readTree("""
                {"tenantId":"pg.citya","serviceCode":"Garbage","priority":"HIGH","description":"lane is dirty","address":{"locality":{"code":"JLC478"}}}
                """));
        assertEquals(true, prepared.get("readyForConfirmation"));
        assertEquals(0, posts.get());
        verify(0, postRequestedFor(urlPathEqualTo("/pgr-services/v2/request/_create")));
    }

    @Test
    void unknownFieldIsRejected() {
        server.start();
        com.github.tomakehurst.wiremock.client.WireMock.configureFor("localhost", server.port());
        McpOperations operations = operations(server.port());
        UserContextHolder.set(citizen());
        McpException exception = assertThrows(McpException.class, () -> operations.search("pgr",
                new ObjectMapper().readTree("{\"tenantId\":\"pg.citya\",\"url\":\"https://evil.example\"}"), 0, 10));
        assertEquals("INVALID_INPUT", exception.getCode());
    }

    private McpOperations operations(int port) {
        org.slf4j.MDC.put(RequestInfoBuilder.CORRELATION_MDC, "corr-1");
        ObjectMapper objectMapper = new ObjectMapper();
        McpProperties properties = new McpProperties();
        properties.setGatewayBaseUrl("http://localhost:" + port);
        properties.setTokenSecret("01234567890123456789012345678901");
        properties.setUiBaseUrl("http://localhost:3000");
        properties.setAllowedGatewayPrefixes(List.of("/pgr-services/", "/property-services/", "/adv-services/",
                "/chb-services/", "/billing-service/", "/egov-mdms-service/", "/filestore/", "/user/", "/access/"));
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(2)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(5));
        RestClient client = RestClient.builder().baseUrl(properties.getGatewayBaseUrl()).requestFactory(factory).build();
        RestGatewayClient gateway = new RestGatewayClient(client, client, objectMapper, CircuitBreakerRegistry.ofDefaults());
        DescriptorRegistry registry = new DescriptorRegistry();
        try {
            registry.replace(new DescriptorLoader(registry, properties).read("classpath:descriptors/*.yaml"));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        PiiMasker masker = new PiiMasker();
        return new McpOperations(registry, gateway, new RequestInfoBuilder(objectMapper, properties),
                new BodyBuilder(objectMapper),
                new SchemaValidator(objectMapper), new ResponseProjector(objectMapper, masker, properties), masker,
                new TenantValidator(), new ConfirmationTokenService(objectMapper, properties), new MemoryStore(),
                properties, objectMapper, new RateLimitService(properties));
    }

    private static AuthenticatedUser citizen() {
        return new AuthenticatedUser("user-1", "CITIZEN", "pg", List.of("CITIZEN"), List.of("pg"), "token-1");
    }

    private static final class MemoryStore implements SingleUseStore {
        private final ConcurrentHashMap<String, String> values = new ConcurrentHashMap<>();

        @Override
        public boolean consumeOnce(String key, Duration ttl) {
            return values.putIfAbsent(key, "1") == null;
        }

        @Override
        public void saveResult(String key, String json, Duration ttl) {
            values.put(key, json);
        }

        @Override
        public Optional<String> findResult(String key) {
            return Optional.ofNullable(values.get(key));
        }

        @Override
        public boolean writesEnabled() {
            return true;
        }
    }
}
