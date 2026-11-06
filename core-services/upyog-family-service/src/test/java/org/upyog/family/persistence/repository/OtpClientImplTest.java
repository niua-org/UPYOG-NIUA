package org.upyog.family.persistence.repository;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.upyog.family.config.FamilyConfiguration;
import org.upyog.family.domain.service.FamilyOtpSessionRegistry;
import org.upyog.family.util.KafkaProducer;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpClientImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private FamilyConfiguration configuration;

    @Mock
    private KafkaProducer kafkaProducer;

    private FamilyOtpSessionRegistry sessionRegistry;

    private OtpClientImpl otpClient;

    private RequestInfo requestInfo;

    @BeforeEach
    void setUp() {
        sessionRegistry = new FamilyOtpSessionRegistry();
        otpClient = new OtpClientImpl(restTemplate, configuration, kafkaProducer, sessionRegistry);
        requestInfo = RequestInfo.builder().apiId("rainmaker").build();
    }

    @Test
    @DisplayName("sendOtp in mock mode should return mock reference ID and register session")
    void sendOtp_MockMode_ReturnsMockRef() {
        when(configuration.getOtpHost()).thenReturn("http://localhost:8089");
        when(configuration.getOtpCreatePath()).thenReturn("/otp/v1/_create");
        when(configuration.getMockConsentEnabled()).thenReturn(true);
        when(configuration.getFamilyOtpReferenceTtlMs()).thenReturn(600_000L);
        when(configuration.getFamilyOtpVerifyMaxAttempts()).thenReturn(5);
        when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("Connection refused"));

        String ref = otpClient.sendOtp("9876543210", "pg.citya", requestInfo);

        assertNotNull(ref);
        assertTrue(ref.startsWith("FAM-OTP-REF-"));
    }

    @Test
    @DisplayName("validateOtp in mock mode requires valid referenceId and fixed OTP")
    void validateOtp_MockMode_RequiresReferenceId() {
        when(configuration.getMockConsentEnabled()).thenReturn(true);
        when(configuration.getMockFixedOtp()).thenReturn("123456");
        when(configuration.getFamilyOtpVerifyMaxAttempts()).thenReturn(3);

        assertFalse(otpClient.validateOtp("9876543210", "123456", "pg.citya", "UNKNOWN-REF", requestInfo));

        when(configuration.getOtpHost()).thenReturn("http://localhost:8089");
        when(configuration.getOtpCreatePath()).thenReturn("/otp/v1/_create");
        when(configuration.getFamilyOtpReferenceTtlMs()).thenReturn(600_000L);
        when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("Connection refused"));

        String ref = otpClient.sendOtp("9876543210", "pg.citya", requestInfo);
        assertTrue(otpClient.validateOtp("9876543210", "123456", "pg.citya", ref, requestInfo));
        assertFalse(otpClient.validateOtp("9876543210", "123456", "pg.citya", ref, requestInfo));
    }

    @Test
    @DisplayName("sendOtp calling egov-otp should return token UUID and push SMS notification")
    @SuppressWarnings("unchecked")
    void sendOtp_CallingEgovOtp_Success() {
        when(configuration.getOtpHost()).thenReturn("http://localhost:8089");
        when(configuration.getOtpCreatePath()).thenReturn("/otp/v1/_create");
        when(configuration.getSmsNotifTopic()).thenReturn("egov.core.notification.sms");
        when(configuration.getFamilyOtpReferenceTtlMs()).thenReturn(600_000L);
        when(configuration.getFamilyOtpVerifyMaxAttempts()).thenReturn(5);
        when(configuration.getSmsOtpMessageTemplate()).thenReturn("Your OTP is %s");

        Map<String, Object> innerOtp = new HashMap<>();
        innerOtp.put("UUID", "uuid-token-1234");
        innerOtp.put("otp", "654321");
        innerOtp.put("identity", "9876543210");

        Map<String, Object> otpResponse = new HashMap<>();
        otpResponse.put("otp", innerOtp);

        when(restTemplate.postForObject(eq("http://localhost:8089/otp/v1/_create"), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(otpResponse);

        String ref = otpClient.sendOtp("9876543210", "pg.citya", requestInfo);

        assertEquals("uuid-token-1234", ref);
        verify(kafkaProducer).push(eq("egov.core.notification.sms"), any());
    }

    @Test
    @DisplayName("sendOtp when gateway fails and mock is disabled should throw CustomException")
    void sendOtp_GatewayFails_ThrowsException() {
        when(configuration.getOtpHost()).thenReturn("http://localhost:8089");
        when(configuration.getOtpCreatePath()).thenReturn("/otp/v1/_create");
        when(configuration.getMockConsentEnabled()).thenReturn(false);
        when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("Connection timed out"));

        assertThrows(CustomException.class, () -> otpClient.sendOtp("9876543210", "pg.citya", requestInfo));
    }

    @Test
    @DisplayName("validateOtp with blank OTP or referenceId returns false")
    void validateOtp_BlankOtp_ReturnsFalse() {
        assertFalse(otpClient.validateOtp("9876543210", "", "pg.citya", "REF", requestInfo));
        assertFalse(otpClient.validateOtp("9876543210", "123456", "pg.citya", "", requestInfo));
    }
}
