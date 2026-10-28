package com.example.gateway.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExceptionUtilsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void raiseErrorFilterException_nullThrowableAndNotFound_writesResourceNotFoundBody() throws Exception {
        Fixture fixture = fixture(HttpStatus.NOT_FOUND);

        Mono<Void> result = ExceptionUtils.raiseErrorFilterException(fixture.exchange, null);

        assertThat(result).isNotNull();
        verify(fixture.response).setStatusCode(HttpStatus.NOT_FOUND);
        Map<String, Object> error = firstError(fixture.body());
        assertThat(error.get("code")).isEqualTo("ResourceNotFoundException");
        assertThat(String.valueOf(error.get("message"))).contains("/missing-resource");
    }

    @Test
    void raiseErrorFilterException_nullThrowableAndOtherStatus_returnsNull() {
        Fixture fixture = fixture(HttpStatus.INTERNAL_SERVER_ERROR);

        Mono<Void> result = ExceptionUtils.raiseErrorFilterException(fixture.exchange, null);

        assertThat(result).isNull();
    }

    @Test
    void raiseErrorFilterException_nullThrowableAndBadRequestWithInvalidTokenText_setsUnauthorized() throws Exception {
        ServerHttpResponse response = mock(ServerHttpResponse.class, org.mockito.Mockito.withSettings()
                .name("existing body mentions InvalidAccessTokenException"));
        Fixture fixture = fixture(HttpStatus.BAD_REQUEST, response);

        Mono<Void> result = ExceptionUtils.raiseErrorFilterException(fixture.exchange, null);

        assertThat(result).isNotNull();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        assertThat(fixture.bodyText()).contains("InvalidAccessTokenException");
    }

    @Test
    void raiseErrorFilterException_nullThrowableAndBadRequestWithoutTokenText_returnsNull() {
        Fixture fixture = fixture(HttpStatus.BAD_REQUEST);

        Mono<Void> result = ExceptionUtils.raiseErrorFilterException(fixture.exchange, null);

        assertThat(result).isNull();
    }

    @Test
    void raiseErrorFilterException_notFoundWrappingResourceAccess_setsBadGateway() throws Exception {
        Fixture fixture = fixture(HttpStatus.OK);
        NotFoundException exception = new NotFoundException("no route", new ResourceAccessException("down"));

        Mono<Void> result = ExceptionUtils.raiseErrorFilterException(fixture.exchange, exception);

        assertThat(result).isNotNull();
        verify(fixture.response).setStatusCode(HttpStatus.BAD_GATEWAY);
        assertThat(firstError(fixture.body()).get("code")).isEqualTo("ResourceAccessException");
        assertThat(firstError(fixture.body()).get("message")).isEqualTo("The backend service is unreachable");
    }

    @Test
    void raiseErrorFilterException_httpHostConnectException_setsBadGateway() throws Exception {
        Fixture fixture = fixture(HttpStatus.OK);

        Mono<Void> result = ExceptionUtils.raiseErrorFilterException(fixture.exchange, new HttpHostConnectException());

        assertThat(result).isNotNull();
        verify(fixture.response).setStatusCode(HttpStatus.BAD_GATEWAY);
        assertThat(firstError(fixture.body()).get("code")).isEqualTo("HttpHostConnectException");
    }

    @Test
    void raiseErrorFilterException_nullPointerException_setsInternalServerError() throws Exception {
        Fixture fixture = fixture(HttpStatus.OK);

        Mono<Void> result = ExceptionUtils.raiseErrorFilterException(fixture.exchange, new NullPointerException("missing user"));

        assertThat(result).isNotNull();
        verify(fixture.response).setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(firstError(fixture.body()).get("message")).isEqualTo("missing user");
    }

    /**
     * Spring 6 {@link HttpClientErrorException#create} returns nested types whose simple name is
     * e.g. {@code BadRequest}/{@code Forbidden}, while {@link ExceptionUtils} matches
     * {@code HttpClientErrorException} by simple name only — so these map to 500 today.
     */
    @Test
    void raiseErrorFilterException_httpClientErrorWithInvalidToken_setsUnauthorized() {
        Fixture fixture = fixture(HttpStatus.OK);
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "bad",
                HttpHeaders.EMPTY,
                "InvalidAccessTokenException".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        Mono<Void> result = ExceptionUtils.raiseErrorFilterException(fixture.exchange, exception);

        assertThat(result).isNotNull();
        verify(fixture.response).setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void raiseErrorFilterException_httpClientError_usesExceptionStatus() {
        Fixture fixture = fixture(HttpStatus.OK);
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.FORBIDDEN,
                "forbidden",
                HttpHeaders.EMPTY,
                "denied".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        Mono<Void> result = ExceptionUtils.raiseErrorFilterException(fixture.exchange, exception);

        assertThat(result).isNotNull();
        verify(fixture.response).setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(fixture.bodyText()).contains("Forbidden");
    }

    @Test
    void raiseErrorFilterException_invalidAccessTokenException_setsUnauthorized() throws Exception {
        Fixture fixture = fixture(HttpStatus.OK);

        Mono<Void> result = ExceptionUtils.raiseErrorFilterException(fixture.exchange, new InvalidAccessTokenException("expired"));

        assertThat(result).isNotNull();
        verify(fixture.response).setStatusCode(HttpStatus.UNAUTHORIZED);
        assertThat(firstError(fixture.body()).get("code")).isEqualTo("InvalidAccessTokenException");
    }

    @Test
    void raiseErrorFilterException_rateLimitExceededException_setsTooManyRequests() throws Exception {
        Fixture fixture = fixture(HttpStatus.OK);

        Mono<Void> result = ExceptionUtils.raiseErrorFilterException(fixture.exchange, new RateLimitExceededException());

        assertThat(result).isNotNull();
        verify(fixture.response).setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(firstError(fixture.body()).get("message")).isEqualTo("Rate limit exceeded");
    }

    @Test
    void raiseErrorFilterException_jsonParseException_setsBadRequest() throws Exception {
        Fixture fixture = fixture(HttpStatus.OK);

        Mono<Void> result = ExceptionUtils.raiseErrorFilterException(fixture.exchange, new JsonParseException());

        assertThat(result).isNotNull();
        verify(fixture.response).setStatusCode(HttpStatus.BAD_REQUEST);
        assertThat(firstError(fixture.body()).get("message")).isEqualTo("Bad request");
    }

    @Test
    void raiseErrorFilterException_customException_alwaysSetsUnauthorized() throws Exception {
        Fixture fixture = fixture(HttpStatus.OK);
        CustomException exception = new CustomException("400 BAD_REQUEST", "tenant rejected");

        Mono<Void> result = ExceptionUtils.raiseErrorFilterException(fixture.exchange, exception);

        assertThat(result).isNotNull();
        verify(fixture.response).setStatusCode(HttpStatus.UNAUTHORIZED);
        assertThat(firstError(fixture.body()).get("message")).isEqualTo("tenant rejected");
    }

    @Test
    void raiseErrorFilterException_unknownException_setsInternalServerError() throws Exception {
        Fixture fixture = fixture(HttpStatus.OK);

        Mono<Void> result = ExceptionUtils.raiseErrorFilterException(fixture.exchange, new IllegalStateException("boom"));

        assertThat(result).isNotNull();
        verify(fixture.response).setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(firstError(fixture.body()).get("code")).isEqualTo("IllegalStateException");
    }

    @Test
    void raiseErrorFilterException_statusWriteFails_returnsNull() {
        Fixture fixture = fixture(HttpStatus.OK);
        org.mockito.Mockito.doThrow(new IllegalStateException("cannot write"))
                .when(fixture.response).setStatusCode(any());

        Mono<Void> result = ExceptionUtils.raiseErrorFilterException(fixture.exchange, new IllegalStateException("boom"));

        assertThat(result).isNull();
    }

    private Map<String, Object> firstError(Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> errors = (List<Map<String, Object>>) body.get("Errors");
        return errors.get(0);
    }

    private Fixture fixture(HttpStatus status) {
        return fixture(status, mock(ServerHttpResponse.class));
    }

    private Fixture fixture(HttpStatus status, ServerHttpResponse response) {
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        DataBufferFactory bufferFactory = mock(DataBufferFactory.class);
        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(request.getPath()).thenReturn(RequestPath.parse("/missing-resource", null));
        when(response.getStatusCode()).thenReturn(status);
        when(response.bufferFactory()).thenReturn(bufferFactory);
        when(response.writeWith(any())).thenReturn(Mono.empty());
        ArgumentCaptor<byte[]> bodyBytes = ArgumentCaptor.forClass(byte[].class);
        when(bufferFactory.wrap(bodyBytes.capture())).thenReturn(mock(DataBuffer.class));
        return new Fixture(exchange, response, bodyBytes);
    }

    private final class Fixture {
        private final ServerWebExchange exchange;
        private final ServerHttpResponse response;
        private final ArgumentCaptor<byte[]> bodyBytes;

        private Fixture(ServerWebExchange exchange, ServerHttpResponse response, ArgumentCaptor<byte[]> bodyBytes) {
            this.exchange = exchange;
            this.response = response;
            this.bodyBytes = bodyBytes;
        }

        private String bodyText() {
            return new String(bodyBytes.getValue(), StandardCharsets.UTF_8);
        }

        private Map<String, Object> body() throws Exception {
            return objectMapper.readValue(bodyText(), new TypeReference<Map<String, Object>>() {
            });
        }
    }

    private static class HttpHostConnectException extends RuntimeException {
        private HttpHostConnectException() {
            super("connect failed");
        }
    }

    private static class InvalidAccessTokenException extends RuntimeException {
        private InvalidAccessTokenException(String message) {
            super(message);
        }
    }

    private static class RateLimitExceededException extends RuntimeException {
        private RateLimitExceededException() {
            super("limited");
        }
    }

    private static class JsonParseException extends RuntimeException {
        private JsonParseException() {
            super("bad json");
        }
    }
}
