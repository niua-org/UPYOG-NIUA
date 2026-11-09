package com.example.gateway.utils;

import com.example.gateway.model.EventLogRequest;
import com.example.gateway.producer.Producer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static com.example.gateway.constants.GatewayConstants.CORRELATION_ID_KEY;
import static com.example.gateway.constants.GatewayConstants.CURRENT_REQUEST_START_TIME;
import static com.example.gateway.constants.GatewayConstants.CURRENT_REQUEST_TENANTID;
import static com.example.gateway.constants.GatewayConstants.REQUEST_INFO_FIELD_NAME_PASCAL_CASE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventLoggerUtilTest {

    @Mock
    private Producer producer;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    private EventLoggerUtil eventLoggerUtil;

    @BeforeEach
    void setUp() {
        eventLoggerUtil = new EventLoggerUtil();
        injectProducer();
        eventLoggerUtil.init();
    }

    @Test
    void logCurrentRequest_startTimeStoredUnderGatewayConstant_doesNotPublish() {
        stubExchange();
        when(exchange.getAttribute(CURRENT_REQUEST_START_TIME)).thenReturn(1L);

        Object result = eventLoggerUtil.logCurrentRequest(exchange, requestBody(), "event-log");

        assertThat(result).isNull();
        verifyNoInteractions(producer);
    }

    @Test
    void logCurrentRequest_literalStartTimeAttributePresent_publishesEvent() {
        stubExchange();
        when(exchange.getAttribute("CURRENT_REQUEST_START_TIME")).thenReturn(1L);
        when(exchange.getAttribute(CORRELATION_ID_KEY)).thenReturn("corr-1");
        when(exchange.getAttribute(CURRENT_REQUEST_TENANTID)).thenReturn("pg.citya");

        Object result = eventLoggerUtil.logCurrentRequest(exchange, requestBody(), "event-log");

        assertThat(result).isNull();
        ArgumentCaptor<String> topic = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(producer).push(topic.capture(), payload.capture());
        assertThat(topic.getValue()).isEqualTo("event-log");
        EventLogRequest event = (EventLogRequest) payload.getValue();
        assertThat(event.getMethod()).isEqualTo("POST");
        assertThat(event.getCorrelationId()).isEqualTo("corr-1");
        assertThat(event.getTenantId()).isEqualTo("pg.citya");
        assertThat(event.getStatusCode()).isEqualTo(200);
        assertThat(event.getUid()).isZero();
        assertThat(event.getRequestBody()).isInstanceOf(Map.class);
    }

    private void stubExchange() {
        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        when(request.getMethod()).thenReturn(HttpMethod.POST);
        when(request.getQueryParams()).thenReturn(new LinkedMultiValueMap<>());
        when(request.getURI()).thenReturn(URI.create("http://localhost/pt-services/property/_search"));
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);
        when(exchange.getAttributes()).thenReturn(new HashMap<>());
    }

    private static Map<String, Object> requestBody() {
        Map<String, Object> body = new HashMap<>();
        body.put(REQUEST_INFO_FIELD_NAME_PASCAL_CASE, new HashMap<>());
        return body;
    }

    private void injectProducer() {
        try {
            var field = EventLoggerUtil.class.getDeclaredField("producer");
            field.setAccessible(true);
            field.set(eventLoggerUtil, producer);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
