package com.example.gateway.filters.pre.helpers;

import com.example.gateway.config.ApplicationProperties;
import com.example.gateway.utils.CommonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.utils.MultiStateInstanceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import org.egov.tracer.model.CustomException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.example.gateway.constants.GatewayConstants.CORRELATION_ID_KEY;
import static com.example.gateway.constants.GatewayConstants.TENANTID_MDC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CorrIdFormDataFilterHelperTest {

    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private MultiStateInstanceUtil centralInstanceUtil;
    @Mock
    private CommonUtils commonUtils;
    @Mock
    private ServerWebExchange exchange;
    @Mock
    private ServerHttpRequest request;
    @Mock
    private RequestPath requestPath;

    private CorrIdFormDataFilterHelper helper;

    @BeforeEach
    void setUp() {
        helper = new CorrIdFormDataFilterHelper(
                applicationProperties, centralInstanceUtil, new ObjectMapper(), commonUtils);
        when(exchange.getRequest()).thenReturn(request);
        when(request.getPath()).thenReturn(requestPath);
        when(requestPath.value()).thenReturn("/filestore/v1/files");
        when(requestPath.toString()).thenReturn("/filestore/v1/files");
    }

    @Test
    void apply_nonCentralInstance_setsCorrelationIdOnExchange() {
        when(centralInstanceUtil.getIsEnvironmentCentralInstance()).thenReturn(false);
        when(applicationProperties.getOpenEndpointsWhitelist()).thenReturn(List.of());
        when(applicationProperties.getMixedModeEndpointsWhitelist()).thenReturn(List.of());
        Map<String, Object> attributes = new HashMap<>();
        when(exchange.getAttributes()).thenReturn(attributes);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        @SuppressWarnings("unchecked")
        MultiValueMap<String, String> result =
                (MultiValueMap<String, String>) Mono.from(helper.apply(exchange, body)).block();

        assertThat(result).isSameAs(body);
        assertThat(attributes.get(CORRELATION_ID_KEY)).isNotNull();
    }

    @Test
    void apply_centralOpenEndpointWithTenant_setsTenantMdc() {
        when(centralInstanceUtil.getIsEnvironmentCentralInstance()).thenReturn(true);
        when(applicationProperties.getOpenEndpointsWhitelist()).thenReturn(List.of("/filestore/v1/files"));
        when(applicationProperties.getMixedModeEndpointsWhitelist()).thenReturn(List.of());
        when(commonUtils.getTenantIdsFromRequest(request, Map.of("tenantId", "pg.citya")))
                .thenReturn(Set.of("pg.citya"));
        when(commonUtils.getLowLevelTenantIdFromSet(Set.of("pg.citya"))).thenReturn("pg.citya");
        Map<String, Object> attributes = new HashMap<>();
        when(exchange.getAttributes()).thenReturn(attributes);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("tenantId", "pg.citya");

        Mono.from(helper.apply(exchange, body)).block();

        assertThat(attributes).containsEntry(TENANTID_MDC, "pg.citya");
    }

    @Test
    void apply_centralOpenEndpointWithoutTenant_throwsInvalidTenantId() {
        when(centralInstanceUtil.getIsEnvironmentCentralInstance()).thenReturn(true);
        when(applicationProperties.getOpenEndpointsWhitelist()).thenReturn(List.of("/filestore/v1/files"));
        when(applicationProperties.getMixedModeEndpointsWhitelist()).thenReturn(List.of());
        when(commonUtils.getTenantIdsFromRequest(any(), any())).thenReturn(Set.of());
        when(exchange.getAttributes()).thenReturn(new HashMap<>());

        assertThatThrownBy(() -> Mono.from(helper.apply(exchange, new LinkedMultiValueMap<>())).block())
                .isInstanceOfSatisfying(CustomException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("INVALID_TENANT_ID"));
    }

    @Test
    void apply_centralMixedEndpointWithoutTenant_stillSetsCorrelationId() {
        when(requestPath.value()).thenReturn("/mixed/upload");
        when(requestPath.toString()).thenReturn("/mixed/upload");
        when(centralInstanceUtil.getIsEnvironmentCentralInstance()).thenReturn(true);
        when(applicationProperties.getOpenEndpointsWhitelist()).thenReturn(List.of());
        when(applicationProperties.getMixedModeEndpointsWhitelist()).thenReturn(List.of("/mixed/upload"));
        when(commonUtils.getTenantIdsFromRequest(request, Map.of())).thenReturn(Set.of());
        when(commonUtils.getLowLevelTenantIdFromSet(Set.of())).thenReturn(null);
        Map<String, Object> attributes = new HashMap<>();
        when(exchange.getAttributes()).thenReturn(attributes);

        Mono.from(helper.apply(exchange, new LinkedMultiValueMap<>())).block();

        assertThat(attributes.get(CORRELATION_ID_KEY)).isNotNull();
    }
}
