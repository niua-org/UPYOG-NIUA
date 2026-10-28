package com.example.gateway.filters.pre.helpers;

import com.example.gateway.config.ApplicationProperties;
import com.example.gateway.utils.CommonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.common.contract.request.User;
import org.egov.common.utils.MultiStateInstanceUtil;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.net.URI;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.example.gateway.constants.GatewayConstants.CORRELATION_ID_KEY;
import static com.example.gateway.constants.GatewayConstants.CURRENT_REQUEST_SANITIZED_BODY;
import static com.example.gateway.constants.GatewayConstants.REQUEST_INFO_FIELD_NAME_PASCAL_CASE;
import static com.example.gateway.constants.GatewayConstants.TENANTID_MDC;
import static com.example.gateway.utils.UpyogMcpGatewaySupport.GATEWAY_REQUEST_INFO_ATTR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RbacFilterHelperTest {

    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private MultiStateInstanceUtil centralInstanceUtil;
    @Mock
    private CommonUtils commonUtils;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private RestTemplate restTemplate;

    private RbacFilterHelper helper;

    @BeforeEach
    void setUp() {
        helper = new RbacFilterHelper(objectMapper, centralInstanceUtil, commonUtils, applicationProperties, restTemplate);
    }

    @Test
    void apply_authorizedUser_returnsBodyAndStoresSanitizedBody() {
        ServerWebExchange exchange = exchangeFor("/billing-service/bill/v1/_search");
        Map<String, Object> body = new HashMap<>();
        body.put(REQUEST_INFO_FIELD_NAME_PASCAL_CASE, Map.of());
        RequestInfo requestInfo = requestInfoWithUser("user-1", "pb");
        when(objectMapper.convertValue(body.get(REQUEST_INFO_FIELD_NAME_PASCAL_CASE), RequestInfo.class))
                .thenReturn(requestInfo);
        when(centralInstanceUtil.getIsEnvironmentCentralInstance()).thenReturn(false);
        when(commonUtils.validateRequestAndSetRequestTenantId(any(), eq(body))).thenReturn(Set.of("pb"));
        when(applicationProperties.getAuthorizationUrl()).thenReturn("http://access-control/actions/test");
        when(restTemplate.postForEntity(eq("http://access-control/actions/test"), any(), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        @SuppressWarnings("unchecked")
        Map<String, Object> result = Mono.from(helper.apply(exchange, body)).block();

        assertThat(result).isSameAs(body);
        assertThat(exchange.getAttributes().get(CURRENT_REQUEST_SANITIZED_BODY)).isSameAs(body);
    }

    @Test
    void apply_missingUser_throwsRuntimeException() {
        ServerWebExchange exchange = exchangeFor("/secure");
        Map<String, Object> body = new HashMap<>();
        body.put(REQUEST_INFO_FIELD_NAME_PASCAL_CASE, Map.of());
        RequestInfo requestInfo = RequestInfo.builder().userInfo(null).build();
        when(objectMapper.convertValue(body.get(REQUEST_INFO_FIELD_NAME_PASCAL_CASE), RequestInfo.class))
                .thenReturn(requestInfo);
        when(commonUtils.validateRequestAndSetRequestTenantId(any(), eq(body))).thenReturn(Set.of("pb"));

        assertThatThrownBy(() -> Mono.from(helper.apply(exchange, body)).block())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User information not found");
    }

    @Test
    void apply_mcpPathWithoutUser_throwsRuntimeException() {
        ServerWebExchange exchange = exchangeFor("/upyog-mcp-server/mcp");
        Map<String, Object> body = new HashMap<>();

        assertThatThrownBy(() -> Mono.from(helper.apply(exchange, body)).block())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User information not found");
    }

    @Test
    void apply_mcpPathWithoutTenantInUserOrRequest_throwsTenantMissing() {
        ServerWebExchange exchange = exchangeFor("/upyog-mcp-server/mcp");
        Map<String, Object> body = new HashMap<>();
        User user = User.builder().uuid("user-mcp").roles(List.of()).build();
        RequestInfo requestInfo = RequestInfo.builder().userInfo(user).build();
        when(exchange.getAttribute(GATEWAY_REQUEST_INFO_ATTR)).thenReturn(requestInfo);
        when(commonUtils.getTenantIdsFromRequest(any(), eq(body))).thenReturn(Set.of());

        assertThatThrownBy(() -> Mono.from(helper.apply(exchange, body)).block())
                .isInstanceOfSatisfying(CustomException.class, ex ->
                        assertThat(ex.getMessage()).contains("Could not resolve tenantId"));
    }

    @Test
    void apply_accessControlUnknownError_treatedAsUnauthorized() {
        ServerWebExchange exchange = exchangeFor("/secure");
        Map<String, Object> body = new HashMap<>();
        body.put(REQUEST_INFO_FIELD_NAME_PASCAL_CASE, Map.of());
        when(objectMapper.convertValue(body.get(REQUEST_INFO_FIELD_NAME_PASCAL_CASE), RequestInfo.class))
                .thenReturn(requestInfoWithUser("user-3", "pb"));
        when(centralInstanceUtil.getIsEnvironmentCentralInstance()).thenReturn(false);
        when(commonUtils.validateRequestAndSetRequestTenantId(any(), eq(body))).thenReturn(Set.of("pb"));
        when(applicationProperties.getAuthorizationUrl()).thenReturn("http://access-control/actions/test");
        when(restTemplate.postForEntity(any(), any(), eq(Void.class)))
                .thenThrow(new IllegalStateException("network down"));

        assertThatThrownBy(() -> Mono.from(helper.apply(exchange, body)).block())
                .isInstanceOfSatisfying(CustomException.class, ex ->
                        assertThat(ex.getMessage()).contains("not authorized"));
    }

    @Test
    void apply_accessControlReturnsForbidden_throwsUnauthorizedCustomException() {
        ServerWebExchange exchange = exchangeFor("/secure");
        Map<String, Object> body = new HashMap<>();
        body.put(REQUEST_INFO_FIELD_NAME_PASCAL_CASE, Map.of());
        when(objectMapper.convertValue(body.get(REQUEST_INFO_FIELD_NAME_PASCAL_CASE), RequestInfo.class))
                .thenReturn(requestInfoWithUser("user-2", "pb"));
        when(centralInstanceUtil.getIsEnvironmentCentralInstance()).thenReturn(false);
        when(commonUtils.validateRequestAndSetRequestTenantId(any(), eq(body))).thenReturn(Set.of("pb"));
        when(applicationProperties.getAuthorizationUrl()).thenReturn("http://access-control/actions/test");
        when(restTemplate.postForEntity(any(), any(), eq(Void.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> Mono.from(helper.apply(exchange, body)).block())
                .isInstanceOfSatisfying(CustomException.class, ex ->
                        assertThat(ex.getMessage()).contains("not authorized"));
    }

    private static RequestInfo requestInfoWithUser(String uuid, String tenantId) {
        Role role = Role.builder().code("EMPLOYEE").name("Employee").tenantId(tenantId).build();
        User user = User.builder().uuid(uuid).tenantId(tenantId).roles(List.of(role)).build();
        return RequestInfo.builder().userInfo(user).build();
    }

    private static ServerWebExchange exchangeFor(String path) {
        ServerWebExchange exchange = org.mockito.Mockito.mock(ServerWebExchange.class);
        ServerHttpRequest request = org.mockito.Mockito.mock(ServerHttpRequest.class);
        RequestPath requestPath = org.mockito.Mockito.mock(RequestPath.class);
        lenient().when(requestPath.value()).thenReturn(path);
        lenient().when(request.getURI()).thenReturn(URI.create("http://localhost" + path));
        lenient().when(request.getMethod()).thenReturn(HttpMethod.POST);
        lenient().when(request.getPath()).thenReturn(requestPath);
        when(exchange.getRequest()).thenReturn(request);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(CORRELATION_ID_KEY, "test-correlation-id");
        lenient().when(exchange.getAttributes()).thenReturn(attributes);
        return exchange;
    }
}
