package org.egov.mdms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.reciept.consumer.config.PropertiesManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private PropertiesManager propertiesManager;

    @InjectMocks
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(tokenService, "mapper", new ObjectMapper());
        when(propertiesManager.getTokenAuhorizationtKey()).thenReturn("Basic abc");
        when(propertiesManager.getSiUser()).thenReturn("user");
        when(propertiesManager.getSiScope()).thenReturn("scope");
        when(propertiesManager.getSiPassword()).thenReturn("pass");
        when(propertiesManager.getSiGrantType()).thenReturn("password");
        when(propertiesManager.getSiUserType()).thenReturn("EMPLOYEE");
        when(propertiesManager.getTokenGenUrl()).thenReturn("/oauth/token");
        when(propertiesManager.getUserHostUrl()).thenReturn("http://user/");
    }

    @Test
    void generateAdminToken_returnsAccessToken() {
        Map<String, Object> response = new HashMap<>();
        response.put("access_token", "token-123");
        response.put("UserRequest", Map.of("uuid", "u1", "type", "EMPLOYEE"));
        when(restTemplate.postForObject(anyString(), any(), eq(Object.class))).thenReturn(response);

        String token = tokenService.generateAdminToken("pb.amritsar");

        assertThat(token).isEqualTo("token-123");
    }

    @Test
    void generateAdminToken_restFailure_returnsNull() {
        when(restTemplate.postForObject(anyString(), any(), eq(Object.class)))
                .thenThrow(new RestClientException("down"));

        assertThat(tokenService.generateAdminToken("pb.amritsar")).isNull();
    }
}
