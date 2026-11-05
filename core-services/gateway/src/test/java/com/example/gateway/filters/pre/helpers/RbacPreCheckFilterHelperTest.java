package com.example.gateway.filters.pre.helpers;

import com.example.gateway.config.ApplicationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.gateway.constants.GatewayConstants.RBAC_BOOLEAN_FLAG_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RbacPreCheckFilterHelperTest {

    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private ServerWebExchange exchange;
    @Mock
    private ServerHttpRequest request;
    @Mock
    private RequestPath requestPath;

    private RbacPreCheckFilterHelper helper;

    @BeforeEach
    void setUp() {
        helper = new RbacPreCheckFilterHelper(List.of("/public/ping"), applicationProperties);
        when(exchange.getRequest()).thenReturn(request);
        when(request.getPath()).thenReturn(requestPath);
    }

    @Test
    void apply_openEndpoint_setsRbacFlagFalse() {
        when(requestPath.value()).thenReturn("/user/_login");
        when(applicationProperties.getOpenEndpointsWhitelist()).thenReturn(List.of("/user/_login"));
        Map<String, Object> attributes = new HashMap<>();
        when(exchange.getAttributes()).thenReturn(attributes);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) Mono.from(helper.apply(exchange, Map.of())).block();

        assertThat(body).isNotNull();
        assertThat(attributes.get(RBAC_BOOLEAN_FLAG_NAME)).isEqualTo(false);
    }

    @Test
    void apply_protectedEndpoint_setsRbacFlagTrue() {
        when(requestPath.value()).thenReturn("/billing-service/bill/v2/_search");
        when(applicationProperties.getOpenEndpointsWhitelist()).thenReturn(List.of());
        Map<String, Object> attributes = new HashMap<>();
        when(exchange.getAttributes()).thenReturn(attributes);

        Mono.from(helper.apply(exchange, Map.of())).block();

        assertThat(attributes.get(RBAC_BOOLEAN_FLAG_NAME)).isEqualTo(true);
    }

    @Test
    void apply_anonymousWhitelist_setsRbacFlagFalse() {
        when(requestPath.value()).thenReturn("/public/ping");
        when(applicationProperties.getOpenEndpointsWhitelist()).thenReturn(List.of());
        Map<String, Object> attributes = new HashMap<>();
        when(exchange.getAttributes()).thenReturn(attributes);

        Mono.from(helper.apply(exchange, Map.of())).block();

        assertThat(attributes.get(RBAC_BOOLEAN_FLAG_NAME)).isEqualTo(false);
    }
}
