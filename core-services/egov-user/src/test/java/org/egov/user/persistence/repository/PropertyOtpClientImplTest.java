package org.egov.user.persistence.repository;

import org.egov.common.contract.request.RequestInfo;
import org.egov.user.config.UserConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
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
}
