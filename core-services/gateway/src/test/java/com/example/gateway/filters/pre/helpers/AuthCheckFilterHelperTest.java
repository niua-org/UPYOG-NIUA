package com.example.gateway.filters.pre.helpers;

import com.example.gateway.utils.UserUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.common.contract.request.User;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.gateway.constants.GatewayConstants.REQUEST_INFO_FIELD_NAME_PASCAL_CASE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthCheckFilterHelperTest {

    @Mock
    private UserUtils userUtils;

    @Mock
    private ServerWebExchange exchange;

    private AuthCheckFilterHelper helper;

    @BeforeEach
    void setUp() {
        helper = new AuthCheckFilterHelper(new ObjectMapper(), userUtils);
    }

    @Test
    void apply_bodyToken_attachesFetchedUser() {
        User user = User.builder()
                .uuid("user-1")
                .userName("citizen")
                .roles(List.of(Role.builder().code("CITIZEN").tenantId("pg.citya").build()))
                .build();
        when(userUtils.getUser("token-1")).thenReturn(user);
        Map<String, Object> body = new HashMap<>();
        body.put(REQUEST_INFO_FIELD_NAME_PASCAL_CASE, Map.of("authToken", "token-1"));

        @SuppressWarnings("unchecked")
        Map<String, Object> published = (Map<String, Object>) Mono.from(helper.apply(exchange, body)).block();

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(userUtils).getUser(token.capture());
        assertThat(token.getValue()).isEqualTo("token-1");
        RequestInfo requestInfo = (RequestInfo) published.get(REQUEST_INFO_FIELD_NAME_PASCAL_CASE);
        assertThat(requestInfo.getAuthToken()).isEqualTo("token-1");
        assertThat(requestInfo.getUserInfo()).isSameAs(user);
    }

    @Test
    void apply_nullBodyWithHeaderToken_doesNotCopyTokenOntoRequestInfo() {
        User user = User.builder().uuid("user-1").userName("citizen").roles(List.of()).build();
        when(userUtils.getUser("header-token")).thenReturn(user);
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Auth-Token", "header-token");
        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);

        @SuppressWarnings("unchecked")
        Map<String, Object> published = (Map<String, Object>) Mono.from(helper.apply(exchange, null)).block();

        RequestInfo requestInfo = (RequestInfo) published.get(REQUEST_INFO_FIELD_NAME_PASCAL_CASE);
        assertThat(requestInfo.getUserInfo()).isSameAs(user);
        assertThat(requestInfo.getAuthToken()).isNull();
    }

    @Test
    void apply_userLookupReturnsNull_throwsAuthenticationError() {
        when(userUtils.getUser("token-1")).thenReturn(null);
        Map<String, Object> body = new HashMap<>();
        body.put(REQUEST_INFO_FIELD_NAME_PASCAL_CASE, Map.of("authToken", "token-1"));

        assertThatThrownBy(() -> helper.apply(exchange, body))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("AUTHENTICATION_ERROR"));
    }

    @Test
    void apply_userLookupThrows_wrapsInAuthenticationError() {
        when(userUtils.getUser("token-1")).thenThrow(new RuntimeException("auth down"));
        Map<String, Object> body = new HashMap<>();
        body.put(REQUEST_INFO_FIELD_NAME_PASCAL_CASE, Map.of("authToken", "token-1"));

        assertThatThrownBy(() -> helper.apply(exchange, body))
                .isInstanceOfSatisfying(CustomException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("AUTHENTICATION_ERROR");
                    assertThat(exception.getMessage()).isEqualTo("auth down");
                });
    }
}
