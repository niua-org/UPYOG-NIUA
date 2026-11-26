package com.tarento.analytics.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetryTemplateTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private RetryTemplate retryTemplate;

    @Test
    void postForEntity_delegatesToRestTemplate() {
        when(restTemplate.postForEntity(eq("http://es"), eq("body"), eq(Object.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        ResponseEntity<Object> response = retryTemplate.postForEntity("http://es", "body");

        assertThat(response.getBody()).isEqualTo("ok");
        verify(restTemplate).postForEntity("http://es", "body", Object.class);
    }

    @Test
    void getForEntity_delegatesToRestTemplate() {
        HttpEntity<String> entity = new HttpEntity<>("{}");
        when(restTemplate.exchange(eq("http://es"), eq(HttpMethod.GET), eq(entity), eq(Object.class)))
                .thenReturn(ResponseEntity.ok(Map.of()));

        ResponseEntity<Object> response = retryTemplate.getForEntity("http://es", entity);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
