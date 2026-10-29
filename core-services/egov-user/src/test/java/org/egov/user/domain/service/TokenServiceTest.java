package org.egov.user.domain.service;

import org.egov.user.domain.exception.InvalidAccessTokenException;
import org.egov.user.domain.model.Action;
import org.egov.user.domain.model.SecureUser;
import org.egov.user.domain.model.UserDetail;
import org.egov.user.persistence.repository.ActionRestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private OAuth2AuthorizationService authorizationService;
    @Mock
    private ActionRestRepository actionRestRepository;
    @Mock
    private UserService userService;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    private TokenService tokenService;

    @BeforeEach
    void setUp() throws Exception {
        Constructor<TokenService> constructor = TokenService.class.getDeclaredConstructor(
                OAuth2AuthorizationService.class, ActionRestRepository.class, UserService.class);
        constructor.setAccessible(true);
        tokenService = constructor.newInstance(authorizationService, actionRestRepository, userService);
        ReflectionTestUtils.setField(tokenService, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(tokenService, "isRoleStateLevel", false);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void getUser_blankToken_throwsInvalidAccessTokenException() {
        assertThatThrownBy(() -> tokenService.getUser(""))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void getUser_jwtAuthorizationWithSecureUser_returnsUserDetail() {
        SecureUser secureUser = new SecureUser(
                org.egov.user.web.contract.auth.User.builder().uuid("u-1").userName("alice").build());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(secureUser, null);
        OAuth2Authorization authorization = mock(OAuth2Authorization.class);
        when(authorization.getAttribute(org.springframework.security.core.Authentication.class.getName()))
                .thenReturn(authentication);
        when(authorizationService.findByToken("jwt-token", OAuth2TokenType.ACCESS_TOKEN)).thenReturn(authorization);

        UserDetail detail = tokenService.getUser("jwt-token");

        assertThat(detail.getSecureUser()).isSameAs(secureUser);
    }

    @Test
    void getUser_opaqueToken_resolvesUserAndActions() {
        Map<String, Object> tokenMetadata = opaqueTokenMetadata();
        when(authorizationService.findByToken("opaque-token", OAuth2TokenType.ACCESS_TOKEN)).thenReturn(null);
        when(valueOperations.get("access_token:opaque-token")).thenReturn(tokenMetadata);
        org.egov.user.domain.model.User decrypted = org.egov.user.domain.model.User.builder()
                .id(9L)
                .uuid("uuid-9")
                .username("alice")
                .tenantId("pb.amritsar")
                .roles(Set.of(org.egov.user.domain.model.Role.builder().code("CITIZEN").name("Citizen").build()))
                .build();
        when(userService.decryptUserWithContext(any(), any())).thenReturn(decrypted);
        when(actionRestRepository.getActionByRoleCodes(any(), eq("pb.amritsar"), any()))
                .thenReturn(List.of(Action.builder().name("Create").url("/create").build()));

        UserDetail detail = tokenService.getUser("opaque-token");

        assertThat(detail.getSecureUser().getUser().getUserName()).isEqualTo("alice");
        assertThat(detail.getActions()).hasSize(1);
        verify(actionRestRepository).getActionByRoleCodes(any(), eq("pb.amritsar"), any());
    }

    @Test
    void getUser_opaqueToken_stateLevelTenant_usesStatePrefixForActions() {
        ReflectionTestUtils.setField(tokenService, "isRoleStateLevel", true);
        Map<String, Object> userRequest = new HashMap<>();
        userRequest.put("id", 1);
        userRequest.put("uuid", "uuid-1");
        userRequest.put("userName", "bob");
        userRequest.put("tenantId", "pb.amritsar");
        userRequest.put("active", true);
        userRequest.put("roles", Collections.emptyList());
        Map<String, Object> tokenMetadata = Map.of("UserRequest", userRequest);
        when(authorizationService.findByToken("state-token", OAuth2TokenType.ACCESS_TOKEN)).thenReturn(null);
        when(valueOperations.get("access_token:state-token")).thenReturn(tokenMetadata);
        when(userService.decryptUserWithContext(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(actionRestRepository.getActionByRoleCodes(any(), eq("pb"), any())).thenReturn(Collections.emptyList());

        tokenService.getUser("state-token");

        verify(actionRestRepository).getActionByRoleCodes(any(), eq("pb"), any());
    }

    @Test
    void getUser_missingRedisMetadata_throwsInvalidAccessTokenException() {
        when(authorizationService.findByToken("missing", OAuth2TokenType.ACCESS_TOKEN)).thenReturn(null);
        when(valueOperations.get("access_token:missing")).thenReturn(null);

        assertThatThrownBy(() -> tokenService.getUser("missing"))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void getUser_opaqueTokenWithoutUserRequest_throwsInvalidAccessTokenException() {
        when(authorizationService.findByToken("bad-token", OAuth2TokenType.ACCESS_TOKEN)).thenReturn(null);
        when(valueOperations.get("access_token:bad-token")).thenReturn(Map.of("other", "value"));

        assertThatThrownBy(() -> tokenService.getUser("bad-token"))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    private Map<String, Object> opaqueTokenMetadata() {
        Map<String, Object> role = Map.of("code", "CITIZEN", "name", "Citizen", "tenantId", "pb");
        Map<String, Object> userRequest = new HashMap<>();
        userRequest.put("id", 9);
        userRequest.put("uuid", "uuid-9");
        userRequest.put("userName", "alice");
        userRequest.put("tenantId", "pb.amritsar");
        userRequest.put("active", true);
        userRequest.put("roles", List.of(role));
        return Map.of("UserRequest", userRequest);
    }
}
