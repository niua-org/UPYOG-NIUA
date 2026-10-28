package com.example.gateway.filters.pre.helpers;

import com.example.gateway.utils.CommonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.common.utils.MultiStateInstanceUtil;
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
import java.util.Map;

import static com.example.gateway.constants.GatewayConstants.CORRELATION_ID_KEY;
import static com.example.gateway.constants.GatewayConstants.REQUEST_INFO_FIELD_NAME_PASCAL_CASE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static com.example.gateway.constants.GatewayConstants.TENANTID_MDC;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestEnrichmentFilterHelperTest {

    @Mock
    private MultiStateInstanceUtil centralInstanceUtil;
    @Mock
    private CommonUtils commonUtils;

    private RequestEnrichmentFilterHelper helper;

    @BeforeEach
    void setUp() {
        helper = new RequestEnrichmentFilterHelper(centralInstanceUtil, commonUtils, new ObjectMapper());
    }

    @Test
    void apply_nullBody_returnsEmptyPublisher() {
        ServerWebExchange exchange = exchangeWithCorrelationId();

        Object result = Mono.from(helper.apply(exchange, null)).block();

        assertThat(result).isNull();
    }

    @Test
    void apply_nonCompatibleBodyWithUser_addsUserInfoHeader() {
        ServerWebExchange exchange = exchangeWithCorrelationId();
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        when(request.getHeaders()).thenReturn(headers);
        when(request.getMethod()).thenReturn(HttpMethod.POST);

        Map<String, Object> body = new HashMap<>();
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().uuid("u1").userName("citizen").build())
                .build();
        body.put(REQUEST_INFO_FIELD_NAME_PASCAL_CASE, new ObjectMapper().convertValue(requestInfo, Map.class));

        Mono.from(helper.apply(exchange, body)).block();

        verify(request, atLeastOnce()).mutate();
    }

    @Test
    void apply_bodyWithRequestInfo_returnsSameBodyAndStoresOriginal() {
        ServerWebExchange exchange = exchangeWithCorrelationId();
        Map<String, Object> body = new HashMap<>();
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().uuid("u1").name("Test User").build())
                .build();
        body.put(REQUEST_INFO_FIELD_NAME_PASCAL_CASE, new ObjectMapper().convertValue(requestInfo, Map.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> result = Mono.from(helper.apply(exchange, body)).block();

        assertThat(result).isSameAs(body);
        assertThat(exchange.getAttributes().get("ORIGINAL_REQUEST_BODY")).isSameAs(body);
    }

    private static ServerWebExchange exchangeWithCorrelationId() {
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        ServerHttpRequest.Builder builder = mock(ServerHttpRequest.Builder.class);
        RequestPath requestPath = mock(RequestPath.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        lenient().when(requestPath.value()).thenReturn("/service/_create");
        lenient().when(request.getPath()).thenReturn(requestPath);
        lenient().when(request.getHeaders()).thenReturn(headers);
        lenient().when(request.getMethod()).thenReturn(HttpMethod.POST);
        lenient().when(request.mutate()).thenReturn(builder);
        lenient().when(builder.headers(any())).thenReturn(builder);
        lenient().when(builder.build()).thenReturn(request);
        when(exchange.getRequest()).thenReturn(request);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(CORRELATION_ID_KEY, "corr-123");
        when(exchange.getAttributes()).thenReturn(attributes);
        return exchange;
    }
}
