package com.example.gateway.filters.pre.helpers;

import com.example.gateway.utils.UpyogMcpGatewaySupport;
import com.example.gateway.utils.UserUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.User;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UpyogMcpGatewayAuthHelperTest {

    @Mock
    private UserUtils userUtils;
    @Mock
    private GatewayFilterChain chain;
    @Mock
    private ServerWebExchange exchange;
    @Mock
    private ServerHttpRequest request;
    @Mock
    private ServerWebExchange.Builder exchangeBuilder;
    @Mock
    private ServerHttpRequest.Builder requestBuilder;

    private UpyogMcpGatewayAuthHelper helper;

    @BeforeEach
    void setUp() {
        helper = new UpyogMcpGatewayAuthHelper(userUtils, new ObjectMapper());
        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getAttributes()).thenReturn(new HashMap<>());
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void authenticateAndForward_missingToken_throwsUnauthorized() {
        when(request.getHeaders()).thenReturn(new HttpHeaders());

        assertThatThrownBy(() -> helper.authenticateAndForward(exchange, chain).block())
                .isInstanceOf(CustomException.class);
    }

    @Test
    void authenticateAndForward_validToken_forwardsWithGatewayRequestInfo() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Auth-Token", "token-1");
        when(request.getHeaders()).thenReturn(headers);
        User user = User.builder().uuid("user-1").userName("citizen").build();
        when(userUtils.getUser("token-1")).thenReturn(user);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request(org.mockito.ArgumentMatchers.<java.util.function.Consumer<ServerHttpRequest.Builder>>any()))
                .thenReturn(exchangeBuilder);
        when(exchangeBuilder.build()).thenReturn(exchange);

        helper.authenticateAndForward(exchange, chain).block();

        verify(userUtils).getUser("token-1");
        verify(chain).filter(any());
    }
}
