package com.example.gateway.filters.pre;

import com.example.gateway.config.ApplicationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyRequestBodyGatewayFilterFactory;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;

import static com.example.gateway.constants.GatewayConstants.AUTH_BOOLEAN_FLAG_NAME;
import static com.example.gateway.constants.GatewayConstants.RBAC_BOOLEAN_FLAG_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RbacPreCheckFilterTest {

    @Mock
    private ModifyRequestBodyGatewayFilterFactory modifyRequestBodyFilterFactory;
    @Mock
    private ServerWebExchange exchange;
    @Mock
    private ServerHttpRequest request;
    @Mock
    private RequestPath requestPath;
    @Mock
    private GatewayFilterChain chain;

    private ApplicationProperties applicationProperties;
    private RbacPreCheckFilter filter;

    @BeforeEach
    void setUp() {
        applicationProperties = new ApplicationProperties();
        applicationProperties.setOpenEndpointsWhitelist(List.of("/open"));
        applicationProperties.setMixedModeEndpointsWhitelist(List.of("/mixed"));
        filter = new RbacPreCheckFilter(
                modifyRequestBodyFilterFactory,
                List.of("/anonymous"),
                applicationProperties);
        when(exchange.getRequest()).thenReturn(request);
        when(request.getPath()).thenReturn(requestPath);
        when(chain.filter(exchange)).thenReturn(Mono.empty());
    }

    @Test
    void filter_openEndpoint_disablesRbac() {
        when(requestPath.value()).thenReturn("/open");
        HashMap<String, Object> attributes = new HashMap<>();
        when(exchange.getAttributes()).thenReturn(attributes);

        filter.filter(exchange, chain).block();

        assertThat(attributes.get(RBAC_BOOLEAN_FLAG_NAME)).isEqualTo(false);
        verify(chain).filter(exchange);
    }

    @Test
    void filter_authDisabled_disablesRbac() {
        when(requestPath.value()).thenReturn("/protected");
        HashMap<String, Object> attributes = new HashMap<>();
        when(exchange.getAttributes()).thenReturn(attributes);
        when(exchange.getAttribute(AUTH_BOOLEAN_FLAG_NAME)).thenReturn(Boolean.FALSE);

        filter.filter(exchange, chain).block();

        assertThat(attributes.get(RBAC_BOOLEAN_FLAG_NAME)).isEqualTo(false);
    }

    @Test
    void filter_protectedEndpoint_enablesRbac() {
        when(requestPath.value()).thenReturn("/protected");
        HashMap<String, Object> attributes = new HashMap<>();
        when(exchange.getAttributes()).thenReturn(attributes);

        filter.filter(exchange, chain).block();

        assertThat(attributes.get(RBAC_BOOLEAN_FLAG_NAME)).isEqualTo(true);
    }
}
