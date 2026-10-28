package org.egov.user.domain.service;

import org.egov.user.domain.exception.InvalidAccessTokenException;
import org.egov.user.domain.model.SecureUser;
import org.egov.user.domain.model.UserDetail;
import org.egov.user.persistence.repository.ActionRestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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

    private TokenService tokenService;

    @BeforeEach
    void setUp() throws Exception {
        Constructor<TokenService> constructor = TokenService.class.getDeclaredConstructor(
                OAuth2AuthorizationService.class, ActionRestRepository.class, UserService.class);
        constructor.setAccessible(true);
        tokenService = constructor.newInstance(authorizationService, actionRestRepository, userService);
        ReflectionTestUtils.setField(tokenService, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(tokenService, "isRoleStateLevel", false);
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
}
