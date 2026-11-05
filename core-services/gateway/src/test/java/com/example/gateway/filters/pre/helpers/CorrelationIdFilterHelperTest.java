package com.example.gateway.filters.pre.helpers;

import com.example.gateway.config.ApplicationProperties;
import com.example.gateway.utils.CommonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.utils.MultiStateInstanceUtil;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.example.gateway.constants.GatewayConstants.CORRELATION_ID_KEY;
import static com.example.gateway.constants.GatewayConstants.TENANTID_MDC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterHelperTest {

    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private MultiStateInstanceUtil centralInstanceUtil;
    @Mock
    private CommonUtils commonUtils;

    private CorrelationIdFilterHelper helper;

    @BeforeEach
    void setUp() {
        helper = new CorrelationIdFilterHelper(
                applicationProperties, centralInstanceUtil, new ObjectMapper(), commonUtils);
    }

    @Test
    void apply_nullBody_returnsEmptyPublisher() {
        ServerWebExchange exchange = exchangeFor("/any");

        Object result = Mono.from(helper.apply(exchange, null)).block();

        assertThat(result).isNull();
        assertThat(exchange.getAttributes()).containsKey(CORRELATION_ID_KEY);
    }

    @Test
    void apply_nonNullBody_setsCorrelationIdAndReturnsBody() {
        ServerWebExchange exchange = exchangeFor("/any");
        Map<String, Object> body = new HashMap<>();
        body.put("tenantId", "pb");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = Mono.from(helper.apply(exchange, body)).block();

        assertThat(result).isSameAs(body);
        assertThat(exchange.getAttributes().get(CORRELATION_ID_KEY)).isNotNull();
    }

    @Test
    void apply_centralOpenEndpointWithoutExplicitTenant_usesDefaultPgTenant() {
        ServerWebExchange exchange = exchangeFor("/open-endpoint");
        when(applicationProperties.getOpenEndpointsWhitelist()).thenReturn(List.of("/open-endpoint"));
        when(applicationProperties.getMixedModeEndpointsWhitelist()).thenReturn(List.of());
        when(centralInstanceUtil.getIsEnvironmentCentralInstance()).thenReturn(true);
        when(commonUtils.getLowLevelTenantIdFromSet(Set.of("pg"))).thenReturn("pg");

        Mono.from(helper.apply(exchange, Map.of())).block();

        assertThat(exchange.getAttributes()).containsEntry(TENANTID_MDC, "pg");
    }

    @Test
    void apply_centralMixedEndpoint_resolvesTenantFromBody() {
        ServerWebExchange exchange = exchangeFor("/mixed-endpoint");
        Map<String, Object> body = Map.of("tenantId", "pg.cityb");
        when(applicationProperties.getOpenEndpointsWhitelist()).thenReturn(List.of());
        when(applicationProperties.getMixedModeEndpointsWhitelist()).thenReturn(List.of("/mixed-endpoint"));
        when(centralInstanceUtil.getIsEnvironmentCentralInstance()).thenReturn(true);
        when(commonUtils.getLowLevelTenantIdFromSet(Set.of("pg.cityb"))).thenReturn("pg.cityb");

        Mono.from(helper.apply(exchange, body)).block();

        assertThat(exchange.getAttributes()).containsEntry(TENANTID_MDC, "pg.cityb");
    }

    @Test
    void apply_centralOpenEndpointWithTenant_setsTenantMdcAttribute() {
        ServerWebExchange exchange = exchangeFor("/open-endpoint");
        Map<String, Object> body = Map.of("tenantId", "pg.citya");
        when(applicationProperties.getOpenEndpointsWhitelist()).thenReturn(List.of("/open-endpoint"));
        when(applicationProperties.getMixedModeEndpointsWhitelist()).thenReturn(List.of());
        when(centralInstanceUtil.getIsEnvironmentCentralInstance()).thenReturn(true);
        when(commonUtils.getLowLevelTenantIdFromSet(Set.of("pg.citya"))).thenReturn("pg.citya");

        Mono.from(helper.apply(exchange, body)).block();

        assertThat(exchange.getAttributes()).containsEntry(TENANTID_MDC, "pg.citya");
    }

    private static ServerWebExchange exchangeFor(String path) {
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        RequestPath requestPath = mock(RequestPath.class);
        when(requestPath.value()).thenReturn(path);
        when(requestPath.toString()).thenReturn(path);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        lenient().when(request.getHeaders()).thenReturn(headers);
        lenient().when(request.getMethod()).thenReturn(HttpMethod.POST);
        when(request.getPath()).thenReturn(requestPath);
        when(exchange.getRequest()).thenReturn(request);
        lenient().when(exchange.getAttributes()).thenReturn(new HashMap<>());
        return exchange;
    }
}
