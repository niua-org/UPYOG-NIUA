package org.egov.mdms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.reciept.consumer.config.PropertiesManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private PropertiesManager propertiesManager;
    @Spy
    private ObjectMapper mapper = new ObjectMapper();
    @InjectMocks
    private TokenService tokenService;

    @BeforeEach
    void stubProperties() {
        when(propertiesManager.getUserHostUrl()).thenReturn("http://user/");
        when(propertiesManager.getTokenGenUrl()).thenReturn("/oauth/token");
        when(propertiesManager.getTokenAuhorizationtKey()).thenReturn("Basic abc");
        when(propertiesManager.getSiUser()).thenReturn("system");
        when(propertiesManager.getSiPassword()).thenReturn("pass");
        when(propertiesManager.getSiUserType()).thenReturn("SYSTEM");
        when(propertiesManager.getSiScope()).thenReturn("read");
        when(propertiesManager.getSiGrantType()).thenReturn("password");
    }

    @Test
    void generateAdminToken_success() {
        Map<String, Object> response = new HashMap<>();
        response.put("access_token", "token-123");
        response.put("UserRequest", Map.of("uuid", "u1"));
        when(restTemplate.postForObject(anyString(), any(), eq(Object.class))).thenReturn(response);

        assertThat(tokenService.generateAdminToken("pg.citya")).isEqualTo("token-123");
        verify(propertiesManager).setSiAuthToken("token-123");
    }

    @Test
    void generateAdminToken_nullResponse() {
        when(restTemplate.postForObject(anyString(), any(), eq(Object.class))).thenReturn(null);

        assertThat(tokenService.generateAdminToken("pg.citya")).isNull();
    }

    @Test
    void generateAdminToken_restFailure_returnsNull() {
        when(restTemplate.postForObject(anyString(), any(), eq(Object.class)))
                .thenThrow(new RestClientException("down"));

        assertThat(tokenService.generateAdminToken("pg.citya")).isNull();
    }
}
