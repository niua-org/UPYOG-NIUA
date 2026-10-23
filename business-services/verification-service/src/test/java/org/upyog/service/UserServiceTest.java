package org.upyog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.upyog.config.MainConfiguration;
import org.upyog.repository.ServiceRequestRepository;
import org.upyog.web.models.UserSearchRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private MainConfiguration config;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(new ObjectMapper(), serviceRequestRepository, config);
        when(config.getUserSearchEndpoint()).thenReturn("/user/_search");
        when(config.getUserHost()).thenReturn("http://user");
    }

    @Test
    void fetchUserUUIDs_returnsUuidWhenUserFound() {
        Map<String, Object> response = Map.of("user", List.of(Map.of("uuid", "uuid-123")));
        when(serviceRequestRepository.fetchResult(any(), any())).thenReturn(response);

        Map<String, String> result = userService.fetchUserUUIDs("9999999999", new RequestInfo(), "pb");

        assertThat(result).containsEntry("9999999999", "uuid-123");
    }

    @Test
    void getBaseUserSearchRequest_setsDefaults() {
        UserSearchRequest request = userService.getBaseUserSearchRequest("pb.amritsar", new RequestInfo());

        assertThat(request.getUserType()).isEqualTo("CITIZEN");
        assertThat(request.getTenantId()).isEqualTo("pb.amritsar");
        assertThat(request.getActive()).isTrue();
    }

    @Test
    void getUser_whenResponseNotMap_returnsEmptyResponse() {
        when(serviceRequestRepository.fetchResult(any(), any())).thenReturn("unexpected");

        UserSearchRequest searchRequest = UserSearchRequest.builder().tenantId("pb").build();
        assertThat(userService.getUser(searchRequest).getUser()).isNull();
    }

    @Test
    void searchByUserName_invokesUserSearch() {
        when(config.getInternalMicroserviceUserType()).thenReturn("SYSTEM");
        when(serviceRequestRepository.fetchResult(any(), any())).thenReturn("unexpected");

        assertThat(userService.searchByUserName("internal", "pb").getUser()).isNull();
    }
}
