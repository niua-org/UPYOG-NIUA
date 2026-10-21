package org.egov.hrms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.utils.MultiStateInstanceUtil;
import org.egov.hrms.config.PropertiesManager;
import org.egov.hrms.repository.RestCallRepository;
import org.egov.hrms.web.contract.UserRequest;
import org.egov.hrms.web.contract.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultUserServiceTest {

    @Mock
    private PropertiesManager propertiesManager;
    @Mock
    private RestCallRepository restCallRepository;
    @Mock
    private MultiStateInstanceUtil centralInstanceUtil;

    @InjectMocks
    private DefaultUserService defaultUserService;

    @BeforeEach
    void setUp() {
        defaultUserService.setObjectMapper(new ObjectMapper());
        ReflectionTestUtils.setField(defaultUserService, "userCreateEndpoint", "/user/_create");
        ReflectionTestUtils.setField(defaultUserService, "userSearchEndpoint", "/user/_search");
        ReflectionTestUtils.setField(defaultUserService, "userUpdateEndpoint", "/user/_update");
        when(propertiesManager.getUserHost()).thenReturn("http://user/");
        when(propertiesManager.getIsDecryptionEnable()).thenReturn(false);
    }

    @Test
    void createUser_returnsUserResponse() {
        when(propertiesManager.getUserCreateEndpoint()).thenReturn("/user/_create");
        LinkedHashMap<String, Object> user = new LinkedHashMap<>();
        user.put("uuid", "u1");
        user.put("id", 1);
        user.put("createdDate", "01-01-2020 00:00:00");
        user.put("dob", "01/01/1990");
        LinkedHashMap<String, Object> responseMap = new LinkedHashMap<>();
        responseMap.put("user", List.of(user));
        when(restCallRepository.fetchResult(any(), any())).thenReturn(responseMap);

        UserResponse response = defaultUserService.createUser(UserRequest.builder().requestInfo(new RequestInfo()).build());

        assertThat(response).isNotNull();
        assertThat(response.getUser()).hasSize(1);
    }

    @Test
    void updateUser_returnsUserResponse() {
        when(propertiesManager.getUserUpdateEndpoint()).thenReturn("/user/_update");
        LinkedHashMap<String, Object> user = new LinkedHashMap<>();
        user.put("uuid", "u1");
        user.put("id", 1);
        user.put("createdDate", "01-01-2020 00:00:00");
        user.put("dob", "1990-01-01");
        LinkedHashMap<String, Object> responseMap = new LinkedHashMap<>();
        responseMap.put("user", List.of(user));
        when(restCallRepository.fetchResult(any(), any())).thenReturn(responseMap);

        UserResponse response = defaultUserService.updateUser(UserRequest.builder().requestInfo(new RequestInfo()).build());

        assertThat(response).isNotNull();
        assertThat(response.getUser()).hasSize(1);
    }

    @Test
    void getUser_buildsSearchRequest() {
        when(propertiesManager.getUserSearchEndpoint()).thenReturn("/user/_search");
        LinkedHashMap<String, Object> user = new LinkedHashMap<>();
        user.put("uuid", "u1");
        user.put("id", 1);
        user.put("createdDate", "01-01-2020 00:00:00");
        user.put("dob", "1990-01-01");
        LinkedHashMap<String, Object> responseMap = new LinkedHashMap<>();
        responseMap.put("user", List.of(user));
        when(restCallRepository.fetchResult(any(), any())).thenReturn(responseMap);

        Map<String, Object> criteria = new HashMap<>();
        criteria.put("tenantId", "pb.amritsar");
        criteria.put("mobileNumber", "9999999999");

        UserResponse response = defaultUserService.getUser(new RequestInfo(), criteria);

        assertThat(response.getUser()).hasSize(1);
    }

    @Test
    void getUser_whenUserCallFails_returnsEmptyResponse() {
        when(propertiesManager.getUserSearchEndpoint()).thenReturn("/user/_search");
        when(restCallRepository.fetchResult(any(), any())).thenThrow(new RuntimeException("down"));

        Map<String, Object> criteria = new HashMap<>();
        criteria.put("tenantId", "pb");

        UserResponse response = defaultUserService.getUser(new RequestInfo(), criteria);

        assertThat(response).isNotNull();
    }
}
