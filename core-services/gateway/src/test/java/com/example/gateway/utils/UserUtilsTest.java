package com.example.gateway.utils;

import com.example.gateway.config.ApplicationProperties;
import org.egov.common.contract.request.User;
import org.egov.common.contract.user.UserDetailResponse;
import org.egov.common.utils.MultiStateInstanceUtil;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserUtilsTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private MultiStateInstanceUtil multiStateInstanceUtil;

    private UserUtils userUtils;

    @BeforeEach
    void setUp() {
        userUtils = new UserUtils(restTemplate, applicationProperties);
        ReflectionTestUtils.setField(userUtils, "multiStateInstanceUtil", multiStateInstanceUtil);
    }

    @Test
    void getUser_validToken_returnsUserFromAuthService() {
        when(applicationProperties.getAuthServiceHost()).thenReturn("http://auth-host");
        when(applicationProperties.getAuthUri()).thenReturn("/user/_details?access_token=");
        User expected = User.builder().uuid("user-1").userName("citizen").build();
        when(restTemplate.postForObject(
                "http://auth-host/user/_details?access_token=token-1", null, User.class)).thenReturn(expected);

        User actual = userUtils.getUser("token-1");

        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).postForObject(url.capture(), isNull(), eq(User.class));
        assertThat(url.getValue()).isEqualTo("http://auth-host/user/_details?access_token=token-1");
        assertThat(actual).isSameAs(expected);
    }

    @Test
    void getUser_restTemplateThrows_wrapsInCustomException() {
        when(applicationProperties.getAuthServiceHost()).thenReturn("http://auth-host");
        when(applicationProperties.getAuthUri()).thenReturn("/user/_details?access_token=");
        when(restTemplate.postForObject(
                "http://auth-host/user/_details?access_token=token-1", null, User.class))
                .thenThrow(new RuntimeException("auth down"));

        assertThatThrownBy(() -> userUtils.getUser("token-1"))
                .isInstanceOfSatisfying(CustomException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("Exception occurred while fetching user: ");
                    assertThat(exception.getMessage()).isEqualTo("auth down");
                });
    }

    @Test
    void fetchSystemUser_returnsFirstSystemUser() {
        when(applicationProperties.getUserSearchURI()).thenReturn("http://user-host/user/_search");
        when(multiStateInstanceUtil.getIsEnvironmentCentralInstance()).thenReturn(false);
        User systemUser = User.builder().uuid("sys-1").userName("system").build();
        UserDetailResponse response = org.mockito.Mockito.mock(UserDetailResponse.class);
        when(response.getUser()).thenReturn(List.of(systemUser));
        when(restTemplate.postForObject(eq("http://user-host/user/_search"), org.mockito.ArgumentMatchers.any(HttpEntity.class),
                eq(UserDetailResponse.class))).thenReturn(response);

        User actual = userUtils.fetchSystemUser("pg.city", "corr-1");

        assertThat(actual).isSameAs(systemUser);
    }

    @Test
    void fetchSystemUser_centralInstance_addsTenantHeader() {
        when(applicationProperties.getUserSearchURI()).thenReturn("http://user-host/user/_search");
        when(multiStateInstanceUtil.getIsEnvironmentCentralInstance()).thenReturn(true);
        UserDetailResponse response = org.mockito.Mockito.mock(UserDetailResponse.class);
        when(response.getUser()).thenReturn(Collections.emptyList());
        when(restTemplate.postForObject(eq("http://user-host/user/_search"), org.mockito.ArgumentMatchers.any(HttpEntity.class),
                eq(UserDetailResponse.class))).thenReturn(response);

        User actual = userUtils.fetchSystemUser("pg.city", "corr-2");

        assertThat(actual).isNull();
    }

    @Test
    void fetchSystemUser_restFailure_returnsNull() {
        when(applicationProperties.getUserSearchURI()).thenReturn("http://user-host/user/_search");
        when(multiStateInstanceUtil.getIsEnvironmentCentralInstance()).thenReturn(false);
        when(restTemplate.postForObject(eq("http://user-host/user/_search"), org.mockito.ArgumentMatchers.any(HttpEntity.class),
                eq(UserDetailResponse.class))).thenThrow(new RuntimeException("down"));

        assertThat(userUtils.fetchSystemUser("pg", "corr-3")).isNull();
    }
}
