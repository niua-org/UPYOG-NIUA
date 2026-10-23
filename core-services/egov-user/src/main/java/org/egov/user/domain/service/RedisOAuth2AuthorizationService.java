package org.egov.user.domain.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.util.Assert;

import java.time.Duration;

/**
 * Redis-backed implementation of Spring Security {@link OAuth2AuthorizationService}.
 * Manages OAuth2 authorization entities, access tokens, and refresh tokens in Redis cache with TTL.
 */
public class RedisOAuth2AuthorizationService implements OAuth2AuthorizationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String AUTHORIZATION_KEY_PREFIX = "oauth2:authorization:";
    private static final String TOKEN_KEY_PREFIX = "access_token:";

    public RedisOAuth2AuthorizationService(RedisTemplate<String, Object> redisTemplate) {
        Assert.notNull(redisTemplate, "redisTemplate cannot be null");
        this.redisTemplate = redisTemplate;
    }

    /**
     * Saves the {@link OAuth2Authorization} and indexes by access token and refresh token in Redis.
     *
     * @param authorization the authorization instance to save
     */
    @Override
    public void save(OAuth2Authorization authorization) {
        String authorizationKey = AUTHORIZATION_KEY_PREFIX + authorization.getId();
        redisTemplate.opsForValue().set(authorizationKey, authorization, Duration.ofMinutes(60));
        
        // Index by access token
        if (authorization.getAccessToken() != null) {
            String accessTokenKey = TOKEN_KEY_PREFIX + authorization.getAccessToken().getToken().getTokenValue();
            redisTemplate.opsForValue().set(accessTokenKey, authorization.getId(), Duration.ofMinutes(60));
        }
        
        // Index by refresh token
        if (authorization.getRefreshToken() != null) {
            String refreshTokenKey = TOKEN_KEY_PREFIX + authorization.getRefreshToken().getToken().getTokenValue();
            redisTemplate.opsForValue().set(refreshTokenKey, authorization.getId(), Duration.ofHours(24));
        }
    }

    /**
     * Removes the authorization record and its associated token indexes from Redis.
     *
     * @param authorization the authorization instance to delete
     */
    @Override
    public void remove(OAuth2Authorization authorization) {
        String authorizationKey = AUTHORIZATION_KEY_PREFIX + authorization.getId();
        redisTemplate.delete(authorizationKey);
        
        if (authorization.getAccessToken() != null) {
            String accessTokenKey = TOKEN_KEY_PREFIX + authorization.getAccessToken().getToken().getTokenValue();
            redisTemplate.delete(accessTokenKey);
        }
        
        if (authorization.getRefreshToken() != null) {
            String refreshTokenKey = TOKEN_KEY_PREFIX + authorization.getRefreshToken().getToken().getTokenValue();
            redisTemplate.delete(refreshTokenKey);
        }
    }

    /**
     * Finds an {@link OAuth2Authorization} by its primary identifier in Redis.
     *
     * @param id the authorization ID
     * @return the resolved authorization, or null if not found
     */
    @Override
    public OAuth2Authorization findById(String id) {
        String key = AUTHORIZATION_KEY_PREFIX + id;
        return (OAuth2Authorization) redisTemplate.opsForValue().get(key);
    }

    /**
     * Resolves an {@link OAuth2Authorization} by searching for a matching token value in Redis.
     *
     * @param token     the access or refresh token value
     * @param tokenType token classification (access_token or refresh_token)
     * @return the associated authorization, or null if not found
     */
    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        String tokenKey = TOKEN_KEY_PREFIX + token;
        Object tokenValue = redisTemplate.opsForValue().get(tokenKey);

        if (tokenValue != null) {
            String authorizationId;
            if (tokenValue instanceof String) {
                authorizationId = (String) tokenValue;
            } else {
                authorizationId = tokenValue.toString();
            }
            return findById(authorizationId);
        }

        return null;
    }
}