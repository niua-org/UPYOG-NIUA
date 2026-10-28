package com.ingestpipeline.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

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
        HttpEntity<String> entity = new HttpEntity<>("{}");
        ResponseEntity<Object> expected = new ResponseEntity<>("ok", HttpStatus.CREATED);
        when(restTemplate.postForEntity("http://es/doc", entity, Object.class)).thenReturn(expected);

        ResponseEntity<Object> response = retryTemplate.postForEntity("http://es/doc", entity);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(restTemplate).postForEntity(eq("http://es/doc"), eq(entity), eq(Object.class));
    }

    @Test
    void getForEntity_delegatesToRestTemplateExchange() {
        HttpEntity<Void> headers = new HttpEntity<>(null);
        ResponseEntity<Object> expected = new ResponseEntity<>("hits", HttpStatus.OK);
        when(restTemplate.exchange("http://es/search", HttpMethod.GET, headers, Object.class)).thenReturn(expected);

        ResponseEntity<Object> response = retryTemplate.getForEntity("http://es/search", headers);

        assertThat(response.getBody()).isEqualTo("hits");
        verify(restTemplate).exchange(eq("http://es/search"), eq(HttpMethod.GET), eq(headers), eq(Object.class));
    }
}
