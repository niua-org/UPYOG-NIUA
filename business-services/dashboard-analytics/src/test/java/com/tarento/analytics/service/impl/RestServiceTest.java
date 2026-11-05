package com.tarento.analytics.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestServiceTest {

    @Mock
    private RetryTemplate retryTemplate;

    @InjectMocks
    private RestService restService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(restService, "indexServiceHost", "http://localhost:9200/");
        ReflectionTestUtils.setField(restService, "indexServiceHostSearch", "/_search");
        ReflectionTestUtils.setField(restService, "userName", "elastic");
        ReflectionTestUtils.setField(restService, "password", "secret");
    }

    @Test
    void getESEncodedCredentials_returnsBasicPrefix() {
        assertThat(restService.getESEncodedCredentials()).startsWith("Basic ");
    }

    @Test
    void search_returnsJsonWhenRetrySucceeds() {
        when(retryTemplate.postForEntity(anyString(), any()))
                .thenReturn(ResponseEntity.ok(Map.of("hits", Map.of("total", 0))));

        JsonNode node = restService.search("idx", "{}");

        assertThat(node).isNotNull();
        assertThat(node.get("hits")).isNotNull();
    }

    @Test
    void search_returnsNullOnClientError() {
        when(retryTemplate.postForEntity(anyString(), any()))
                .thenThrow(HttpClientErrorException.create(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "bad", null, null, null));

        assertThat(restService.search("idx", "{}")).isNull();
    }

    @Test
    void post_addsBearerWhenTokenPresent() throws Exception {
        when(retryTemplate.postForEntity(anyString(), any()))
                .thenReturn(ResponseEntity.ok(Map.of("ok", true)));

        JsonNode request = objectMapper.readTree("{\"a\":1}");
        JsonNode response = restService.post("http://example/mdms", "token", request);

        assertThat(response.get("ok").asBoolean()).isTrue();
    }

    @Test
    void get_returnsParsedBody() {
        when(retryTemplate.getForEntity(anyString(), any()))
                .thenReturn(ResponseEntity.ok(Map.of("field", "value")));

        JsonNode response = restService.get("http://example", "token");

        assertThat(response.get("field").asText()).isEqualTo("value");
    }
}
