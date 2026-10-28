package com.example.gateway.filters.pre.helpers;

import com.example.gateway.utils.URLProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreHookFilterHelperTest {

    private static final String REQUEST_URI = "http://localhost:8080/billing-service/bill/v2/_create";
    private static final String PRE_HOOK_URL = "http://localhost:8090/pre-hook";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;

    private PreHookFilterHelper helper;

    @BeforeEach
    void setUp() throws Exception {
        helper = new PreHookFilterHelper(restTemplate, objectMapper);
        setStaticPreHooksMap(Map.of(REQUEST_URI, PRE_HOOK_URL));
        when(exchange.getRequest()).thenReturn(request);
        when(request.getURI()).thenReturn(URI.create(REQUEST_URI));
    }

    @AfterEach
    void tearDown() throws Exception {
        setStaticPreHooksMap(null);
    }

    @Test
    void apply_success_returnsTransformedBody() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("tenantId", "pg");
        Map<String, Object> hookResponse = Map.of("transformed", true);
        when(objectMapper.writeValueAsString(body)).thenReturn("{\"tenantId\":\"pg\"}");
        when(restTemplate.postForObject(eq(PRE_HOOK_URL), any(), eq(String.class))).thenReturn("{\"transformed\":true}");
        when(objectMapper.convertValue("{\"transformed\":true}", Map.class)).thenReturn(hookResponse);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) Mono.from(helper.apply(exchange, body)).block();

        assertThat(result).isEqualTo(hookResponse);
    }

    @Test
    void apply_jsonSerializationFails_throwsParsingError() throws Exception {
        Map<String, Object> body = Map.of("key", "value");
        when(objectMapper.writeValueAsString(body)).thenThrow(new JsonProcessingException("bad json") {});

        assertThatThrownBy(() -> Mono.from(helper.apply(exchange, body)).block())
                .isInstanceOfSatisfying(CustomException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("PARSING_ERROR");
                    assertThat(exception.getMessage()).isEqualTo("Failed to parse request body");
                });
    }

    @Test
    void apply_preHookHttpError_throwsPreHookError() throws Exception {
        Map<String, Object> body = Map.of("tenantId", "pg");
        when(objectMapper.writeValueAsString(body)).thenReturn("{}");
        when(restTemplate.postForObject(eq(PRE_HOOK_URL), any(), eq(String.class)))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.BAD_REQUEST, "bad request", null, null, null));

        assertThatThrownBy(() -> Mono.from(helper.apply(exchange, body)).block())
                .isInstanceOfSatisfying(CustomException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("PRE_HOOK_ERROR");
                    assertThat(exception.getMessage()).contains("Pre-hook url threw an error");
                });
    }

    @Test
    void apply_unexpectedError_throwsPreHookError() throws Exception {
        Map<String, Object> body = Map.of("tenantId", "pg");
        when(objectMapper.writeValueAsString(body)).thenReturn("{}");
        when(restTemplate.postForObject(eq(PRE_HOOK_URL), any(), eq(String.class)))
                .thenThrow(new IllegalStateException("downstream unavailable"));

        assertThatThrownBy(() -> Mono.from(helper.apply(exchange, body)).block())
                .isInstanceOfSatisfying(CustomException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("PRE_HOOK_ERROR");
                    assertThat(exception.getMessage()).contains("downstream unavailable");
                });
    }

    private static void setStaticPreHooksMap(Map<String, String> map) throws Exception {
        Field field = URLProvider.class.getDeclaredField("urlPreHooksMap");
        field.setAccessible(true);
        field.set(null, map == null ? null : Collections.unmodifiableMap(map));
    }
}
