package org.upyog.mcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.upyog.mcp.client.GatewayClient;
import org.upyog.mcp.client.RequestInfoBuilder;
import org.upyog.mcp.client.RestGatewayClient;
import org.upyog.mcp.guard.RedisSingleUseStore;
import org.upyog.mcp.guard.RejectingSingleUseStore;
import org.upyog.mcp.guard.SingleUseStore;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Wires the gateway HTTP clients, circuit breakers, and the confirmation store.
 * HTTP/1.1 is forced so the client matches the UPYOG gateway.
 */
@Configuration
public class ClientConfiguration {

    @Bean
    RestClient readClient(McpProperties properties) {
        return client(properties, properties.getReadTimeout());
    }

    @Bean
    RestClient writeClient(McpProperties properties) {
        return client(properties, properties.getWriteTimeout());
    }

    @Bean
    CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(20)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();
        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    GatewayClient gatewayClient(RestClient readClient, RestClient writeClient, ObjectMapper objectMapper,
                                CircuitBreakerRegistry circuitBreakerRegistry) {
        return new RestGatewayClient(readClient, writeClient, objectMapper, circuitBreakerRegistry);
    }

    @Bean
    RequestInfoBuilder requestInfoBuilder(ObjectMapper objectMapper, McpProperties properties) {
        return new RequestInfoBuilder(objectMapper, properties);
    }

    @Bean
    @ConditionalOnProperty(name = "upyog.mcp.redis.enabled", havingValue = "true")
    SingleUseStore redisSingleUseStore(McpProperties properties) {
        return new RedisSingleUseStore(properties);
    }

    @Bean
    @ConditionalOnProperty(name = "upyog.mcp.redis.enabled", havingValue = "false", matchIfMissing = true)
    SingleUseStore rejectingSingleUseStore() {
        return new RejectingSingleUseStore();
    }

    private RestClient client(McpProperties properties, Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.getConnectTimeout())
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        String base = properties.getGatewayBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String uiBase = properties.getUiBaseUrl();
        if (uiBase.endsWith("/")) {
            uiBase = uiBase.substring(0, uiBase.length() - 1);
        }
        // NIUA UAT gateway may reject server-side clients without same-origin browser headers.
        return RestClient.builder()
                .baseUrl(base)
                .defaultHeader("Origin", uiBase)
                .defaultHeader("Referer", uiBase + "/")
                .requestFactory(factory)
                .build();
    }
}
