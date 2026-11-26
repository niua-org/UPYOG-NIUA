package org.egov.demand.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.egov.common.contract.request.RequestInfo;
import org.egov.demand.config.ApplicationProperties;
import org.egov.demand.model.Demand;
import org.egov.demand.web.contract.User;
import org.egov.demand.web.contract.UserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ApplicationProperties properties;

    @InjectMocks
    private UserService userService;

    @Test
    void getUser_returnsUuidWhenUserFound() {
        when(properties.getUserServiceHostName()).thenReturn("http://user/");
        when(properties.getUserServiceSearchPath()).thenReturn("search");
        User user = new User();
        user.setUuid("uuid-1");
        UserResponse response = new UserResponse();
        response.setUser(List.of(user));
        when(restTemplate.postForObject(anyString(), any(), eq(UserResponse.class))).thenReturn(response);

        Map<String, String> result = userService.getUser(new RequestInfo(), "9999999999", "Citizen", "ap.kurnool");

        assertThat(result).containsEntry("id", "uuid-1");
    }

    @Test
    void getUser_returnsEmptyWhenNoUsers() {
        when(properties.getUserServiceHostName()).thenReturn("http://user/");
        when(properties.getUserServiceSearchPath()).thenReturn("search");
        UserResponse response = new UserResponse();
        response.setUser(Collections.emptyList());
        when(restTemplate.postForObject(anyString(), any(), eq(UserResponse.class))).thenReturn(response);

        assertThat(userService.getUser(new RequestInfo(), "9999999999", "Citizen", "ap.kurnool")).isEmpty();
    }

    @Test
    void getUser_returnsEmptyWhenRemoteCallFails() {
        when(properties.getUserServiceHostName()).thenReturn("http://user/");
        when(properties.getUserServiceSearchPath()).thenReturn("search");
        when(restTemplate.postForObject(anyString(), any(), eq(UserResponse.class)))
                .thenThrow(new RuntimeException("down"));

        assertThat(userService.getUser(new RequestInfo(), "9999999999", "Citizen", "ap.kurnool")).isEmpty();
    }

    @Test
    void createUser_returnsUuidOnSuccess() {
        when(properties.getUserServiceHostName()).thenReturn("http://user/");
        when(properties.getUserCreateEnpoint()).thenReturn("create");
        User payer = new User();
        payer.setName("Test User");
        payer.setMobileNumber("8888888888");
        Demand demand = Demand.builder().tenantId("ap.kurnool").payer(payer).build();

        User created = new User();
        created.setUuid("new-uuid");
        UserResponse response = new UserResponse();
        response.setUser(List.of(created));
        when(restTemplate.postForObject(anyString(), any(), eq(UserResponse.class))).thenReturn(response);

        assertThat(userService.createUser(demand, new RequestInfo())).isEqualTo("new-uuid");
    }

    @Test
    void createUser_returnsNullWhenRemoteCallFails() {
        when(properties.getUserServiceHostName()).thenReturn("http://user/");
        when(properties.getUserCreateEnpoint()).thenReturn("create");
        User payer = new User();
        payer.setName("Test User");
        payer.setMobileNumber("8888888888");
        Demand demand = Demand.builder().tenantId("ap.kurnool").payer(payer).build();
        when(restTemplate.postForObject(anyString(), any(), eq(UserResponse.class)))
                .thenThrow(new RuntimeException("down"));

        assertThat(userService.createUser(demand, new RequestInfo())).isNull();
    }
}
