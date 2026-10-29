package com.example.gateway.filters.pre;

import com.example.gateway.config.ApplicationProperties;
import com.example.gateway.filters.pre.helpers.AuthPreCheckFilterHelper;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthPreCheckFilterTest {

    @Mock
    private ModifyRequestBodyGatewayFilterFactory modifyRequestBodyFilterFactory;
    @Mock
    private AuthPreCheckFilterHelper authPreCheckFilterHelper;
    @Mock
    private ServerWebExchange exchange;
    @Mock
    private ServerHttpRequest request;
    @Mock
    private RequestPath requestPath;
    @Mock
    private GatewayFilterChain chain;

    private AuthPreCheckFilter filter;

    @BeforeEach
    void setUp() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.setOpenEndpointsWhitelist(List.of("/open"));
        filter = new AuthPreCheckFilter(modifyRequestBodyFilterFactory, authPreCheckFilterHelper, properties);
        when(exchange.getRequest()).thenReturn(request);
        when(request.getPath()).thenReturn(requestPath);
        when(chain.filter(exchange)).thenReturn(Mono.empty());
    }

    @Test
    void filter_openEndpoint_skipsAuth() {
        when(requestPath.value()).thenReturn("/open");
        HashMap<String, Object> attributes = new HashMap<>();
        when(exchange.getAttributes()).thenReturn(attributes);

        filter.filter(exchange, chain).block();

        assertThat(attributes.get(AUTH_BOOLEAN_FLAG_NAME)).isEqualTo(Boolean.FALSE);
        verify(chain).filter(exchange);
    }

    @Test
    void filter_mcpPath_enablesAuthWithoutBodyRewrite() {
        when(requestPath.value()).thenReturn("/upyog-mcp-server/mcp");
        HashMap<String, Object> attributes = new HashMap<>();
        when(exchange.getAttributes()).thenReturn(attributes);

        filter.filter(exchange, chain).block();

        assertThat(attributes.get(AUTH_BOOLEAN_FLAG_NAME)).isEqualTo(Boolean.TRUE);
        verify(chain).filter(exchange);
    }

    @Test
    void filter_jsonBody_usesModifyRequestBodyFilter() {
        when(requestPath.value()).thenReturn("/billing/_create");
        when(request.getHeaders()).thenReturn(new org.springframework.http.HttpHeaders());
        when(modifyRequestBodyFilterFactory.apply(any(ModifyRequestBodyGatewayFilterFactory.Config.class)))
                .thenReturn((exchange, chain) -> chain.filter(exchange));

        filter.filter(exchange, chain).block();

        verify(modifyRequestBodyFilterFactory).apply(any(ModifyRequestBodyGatewayFilterFactory.Config.class));
    }
}
