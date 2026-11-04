package org.upyog.family.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.upyog.family.config.FamilyConfiguration;
import org.upyog.family.util.KafkaProducer;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    private ObjectMapper objectMapper = new ObjectMapper();

    private OtpClientImpl otpClient;

    private RequestInfo requestInfo;

    @BeforeEach
    void setUp() {
        otpClient = new OtpClientImpl(restTemplate, configuration, kafkaProducer, objectMapper);
        requestInfo = RequestInfo.builder().apiId("rainmaker").build();
    }

    @Test
    @DisplayName("sendOtp in mock mode should return mock reference ID")
    void sendOtp_MockMode_ReturnsMockRef() {
        when(configuration.getOtpHost()).thenReturn("http://localhost:8089");
        when(configuration.getOtpCreatePath()).thenReturn("/otp/v1/_create");
        when(configuration.getMockConsentEnabled()).thenReturn(true);
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("Connection refused"));

        String ref = otpClient.sendOtp("9876543210", "pg.citya", requestInfo);

        assertNotNull(ref);
        assertTrue(ref.startsWith("FAM-OTP-REF-"));
    }

    @Test
    @DisplayName("sendOtp calling egov-otp should return token UUID and push SMS notification")
    @SuppressWarnings("unchecked")
    void sendOtp_CallingEgovOtp_Success() {
        when(configuration.getOtpHost()).thenReturn("http://localhost:8089");
        when(configuration.getOtpCreatePath()).thenReturn("/otp/v1/_create");
        when(configuration.getSmsNotifTopic()).thenReturn("egov.core.notification.sms");

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
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("Connection timed out"));

        assertThrows(CustomException.class, () -> otpClient.sendOtp("9876543210", "pg.citya", requestInfo));
    }

    @Test
    @DisplayName("validateOtp in mock mode with configured fixed OTP returns true")
    void validateOtp_MockMode_ReturnsTrue() {
        when(configuration.getMockConsentEnabled()).thenReturn(true);
        when(configuration.getMockFixedOtp()).thenReturn("123456");

        boolean result = otpClient.validateOtp("9876543210", "123456", "pg.citya", requestInfo);
        assertTrue(result);
    }

    @Test
    @DisplayName("sendOtp when egov-otp returns blank OTP value should throw CustomException")
    void sendOtp_BlankOtpVal_ThrowsCustomException() {
        when(configuration.getOtpHost()).thenReturn("http://localhost:8089");
        when(configuration.getOtpCreatePath()).thenReturn("/otp/v1/_create");

        Map<String, Object> innerOtp = new HashMap<>();
        innerOtp.put("UUID", "uuid-token-blank");
        innerOtp.put("otp", "");

        Map<String, Object> otpResponse = new HashMap<>();
        otpResponse.put("otp", innerOtp);

        when(restTemplate.postForObject(eq("http://localhost:8089/otp/v1/_create"), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(otpResponse);

        assertThrows(CustomException.class, () -> otpClient.sendOtp("9876543210", "pg.citya", requestInfo));
    }

    @Test
    @DisplayName("validateOtp with egov-otp returning isValidationSuccessful=true returns true")
    @SuppressWarnings("unchecked")
    void validateOtp_EgovOtpSuccess_ReturnsTrue() {
        when(configuration.getOtpHost()).thenReturn("http://localhost:8089");
        when(configuration.getOtpValidatePath()).thenReturn("/otp/v1/_validate");

        Map<String, Object> innerOtp = new HashMap<>();
        innerOtp.put("isValidationSuccessful", true);
        innerOtp.put("identity", "9876543210");

        Map<String, Object> response = new HashMap<>();
        response.put("otp", innerOtp);

        when(restTemplate.postForObject(eq("http://localhost:8089/otp/v1/_validate"), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        boolean result = otpClient.validateOtp("9876543210", "554433", "pg.citya", requestInfo);
        assertTrue(result);
    }

    @Test
    @DisplayName("validateOtp with blank OTP returns false")
    void validateOtp_BlankOtp_ReturnsFalse() {
        boolean result = otpClient.validateOtp("9876543210", "", "pg.citya", requestInfo);
        assertFalse(result);
    }
}
