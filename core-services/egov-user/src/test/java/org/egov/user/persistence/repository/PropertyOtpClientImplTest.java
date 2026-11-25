package org.egov.user.persistence.repository;

import org.egov.common.contract.request.RequestInfo;
import org.egov.user.config.UserConfiguration;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.service.utils.UserConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests validating OTP verification and mock fixed OTP behavior in {@link PropertyOtpClientImpl}.
 */
class PropertyOtpClientImplTest {

    private UserConfiguration userConfiguration;
    private RestTemplate restTemplate;
    private PropertyOtpClientImpl client;

    @BeforeEach
    void setUp() {
        userConfiguration = mock(UserConfiguration.class);
        restTemplate = mock(RestTemplate.class);
        client = new PropertyOtpClientImpl(userConfiguration, restTemplate, null, null);
    }

    /**
     * Verifies that when fixed OTP is enabled, validation passes for the configured mock value and fails otherwise.
     */
    @Test
    @DisplayName("Fixed OTP enabled should validate successfully against configured value")
    void testValidateOtp_FixedEnabled_Success() {
        when(userConfiguration.getPropertyOtpFixedEnabled()).thenReturn(true);
        when(userConfiguration.getPropertyOtpFixedValue()).thenReturn("123456");

        assertTrue(client.validateOtp("9876543210", "123456", "pb.amritsar", new RequestInfo()));
        assertFalse(client.validateOtp("9876543210", "654321", "pb.amritsar", new RequestInfo()));
    }

    @Test
    @DisplayName("sendOtp when fixed OTP enabled does not invoke RestTemplate")
    void testSendOtp_FixedEnabled_NoHttpCall() {
        when(userConfiguration.getPropertyOtpFixedEnabled()).thenReturn(true);

        client.sendOtp("9876543210", "pb.amritsar", new RequestInfo());
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("sendOtp fails closed throwing CustomException when egov-otp service throws exception")
    void testSendOtp_RemoteError_ThrowsException() {
        when(userConfiguration.getPropertyOtpFixedEnabled()).thenReturn(false);
        when(userConfiguration.getOtpServiceHost()).thenReturn("http://egov-otp:8080");
        when(userConfiguration.getOtpCreateEndpoint()).thenReturn("/otp/v1/_create");
        when(restTemplate.postForObject(anyString(), any(), any())).thenThrow(new RuntimeException("Connection refused"));

        CustomException ex = assertThrows(CustomException.class, () ->
                client.sendOtp("9876543210", "pb.amritsar", new RequestInfo()));
        assertEquals(UserConstants.ERROR_OTP_DISPATCH_FAILED, ex.getCode());
    }

    @Test
    @DisplayName("sendOtp fails closed throwing CustomException when egov-otp service returns empty response")
    void testSendOtp_EmptyResponse_ThrowsException() {
        when(userConfiguration.getPropertyOtpFixedEnabled()).thenReturn(false);
        when(userConfiguration.getOtpServiceHost()).thenReturn("http://egov-otp:8080");
        when(userConfiguration.getOtpCreateEndpoint()).thenReturn("/otp/v1/_create");
        when(restTemplate.postForObject(anyString(), any(), any())).thenReturn(Collections.emptyMap());

        CustomException ex = assertThrows(CustomException.class, () ->
                client.sendOtp("9876543210", "pb.amritsar", new RequestInfo()));
        assertEquals(UserConstants.ERROR_OTP_DISPATCH_FAILED, ex.getCode());
    }
}
