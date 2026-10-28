package com.example.gateway.filters.pre.helpers;

import com.example.gateway.config.ApplicationProperties;
import com.example.gateway.utils.UserUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.gateway.constants.GatewayConstants.AUTH_BOOLEAN_FLAG_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthPreCheckFilterHelperTest {

    @Mock
    private UserUtils userUtils;

    private ApplicationProperties applicationProperties;
    private AuthPreCheckFilterHelper helper;

    @BeforeEach
    void setUp() {
        applicationProperties = new ApplicationProperties();
        applicationProperties.setOpenEndpointsWhitelist(List.of("/open"));
        applicationProperties.setMixedModeEndpointsWhitelist(List.of("/mixed"));
        helper = new AuthPreCheckFilterHelper(
                applicationProperties,
                List.of("/ignored-open"),
                List.of("/ignored-mixed"),
                new ObjectMapper(),
                userUtils);
    }

    @Test
    void apply_openEndpointWithBody_skipsAuth() {
        ServerWebExchange exchange = exchangeFor("/open");
        Map<String, Object> body = new HashMap<>();
        body.put("tenantId", "pg");

        Map<?, ?> published = Mono.from(helper.apply(exchange, body)).block();

        assertThat(published).isSameAs(body);
        assertThat(exchange.getAttributes().get(AUTH_BOOLEAN_FLAG_NAME)).isEqualTo(Boolean.FALSE);
    }

    @Test
    void apply_openEndpointWithNullBody_throwsNullPointerException() {
        ServerWebExchange exchange = exchangeFor("/open");

        assertThatThrownBy(() -> helper.apply(exchange, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void apply_constructorWhitelistIgnored_requiresToken() {
        ServerWebExchange exchange = exchangeFor("/ignored-open");

        assertThatThrownBy(() -> helper.apply(exchange, new HashMap<>()))
                .isInstanceOfSatisfying(CustomException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(HttpStatus.UNAUTHORIZED.toString());
                    assertThat(exception.getMessage())
                            .isEqualTo(AuthPreCheckFilterHelper.UNAUTHORIZED_USER_MESSAGE);
                });
    }

    @Test
    void apply_protectedEndpointWithoutToken_throwsUnauthorized() {
        ServerWebExchange exchange = exchangeFor("/secure");

        assertThatThrownBy(() -> helper.apply(exchange, new HashMap<>()))
                .isInstanceOf(CustomException.class);
        assertThat(exchange.getAttributes()).doesNotContainKey(AUTH_BOOLEAN_FLAG_NAME);
    }

    @Test
    void apply_protectedEndpointWithBodyToken_marksAuthRequired() {
        ServerWebExchange exchange = exchangeFor("/secure");
        Map<String, Object> body = new HashMap<>();
        body.put("RequestInfo", Map.of("authToken", "token-1"));

        Map<?, ?> published = Mono.from(helper.apply(exchange, body)).block();

        assertThat(published).isSameAs(body);
        assertThat(exchange.getAttributes().get(AUTH_BOOLEAN_FLAG_NAME)).isEqualTo(Boolean.TRUE);
    }

    @Test
    void apply_protectedEndpointWithHeaderTokenAndNullBody_returnsEmpty() {
        ServerWebExchange exchange = exchangeFor("/secure");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Auth-Token", "header-token");
        when(exchange.getRequest().getHeaders()).thenReturn(headers);

        Object published = Mono.from(helper.apply(exchange, null)).block();

        assertThat(published).isNull();
        assertThat(exchange.getAttributes().get(AUTH_BOOLEAN_FLAG_NAME)).isEqualTo(Boolean.TRUE);
    }

    @Test
    void apply_mixedEndpointWithoutToken_skipsAuth() {
        ServerWebExchange exchange = exchangeFor("/mixed");
        Map<String, Object> body = new HashMap<>();

        Map<?, ?> published = Mono.from(helper.apply(exchange, body)).block();

        assertThat(published).isSameAs(body);
        assertThat(exchange.getAttributes().get(AUTH_BOOLEAN_FLAG_NAME)).isEqualTo(Boolean.FALSE);
    }

    private static ServerWebExchange exchangeFor(String path) {
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        RequestPath requestPath = mock(RequestPath.class);
        when(requestPath.value()).thenReturn(path);
        when(request.getPath()).thenReturn(requestPath);
        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getAttributes()).thenReturn(new HashMap<>());
        return exchange;
    }
}
