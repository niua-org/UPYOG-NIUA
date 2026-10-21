package com.example.gateway.utils;

import com.example.gateway.config.ApplicationProperties;
import org.egov.common.contract.request.User;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

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

    private UserUtils userUtils;

    @BeforeEach
    void setUp() {
        userUtils = new UserUtils(restTemplate, applicationProperties);
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
    void fetchSystemUser_uninjectedMultiStateUtil_throwsNullPointerException() {
        assertThatThrownBy(() -> userUtils.fetchSystemUser("pg", "corr-1"))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(restTemplate);
    }
}
