package org.upyog.family.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
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

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequesterDxClientImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private FamilyConfiguration configuration;

    private ObjectMapper objectMapper = new ObjectMapper();

    private RequesterDxClientImpl client;

    private RequestInfo requestInfo;

    @BeforeEach
    void setUp() {
        client = new RequesterDxClientImpl(restTemplate, configuration, objectMapper);
        requestInfo = RequestInfo.builder().apiId("rainmaker").msgId("test-msg").build();
    }

    @Test
    @DisplayName("Blank consent token must be rejected immediately")
    void verifyConsentToken_BlankToken_ReturnsFalse() {
        assertFalse(client.verifyConsentToken("", "user-1", "FAM-001", "pg.citya", requestInfo));
        assertFalse(client.verifyConsentToken(null, "user-1", "FAM-001", "pg.citya", requestInfo));
        assertFalse(client.verifyConsentToken("   ", "user-1", "FAM-001", "pg.citya", requestInfo));
    }

    @Test
    @DisplayName("Security: Mock consent tokens MUST be rejected when mock consent is disabled (default)")
    void verifyConsentToken_MockToken_WhenMockDisabled_ReturnsFalse() {
        when(configuration.getMockConsentEnabled()).thenReturn(false);

        boolean result = client.verifyConsentToken("CONSENT-VERIFIED-12345", "user-1", "FAM-001", "pg.citya", requestInfo);
        assertFalse(result, "Mock token must be rejected when mockConsentEnabled is false");

        boolean mockOtpResult = client.verifyConsentToken("MOCK-OTP-9999", "user-1", "FAM-001", "pg.citya", requestInfo);
        assertFalse(mockOtpResult, "MOCK-OTP- token must be rejected when mockConsentEnabled is false");
    }

    @Test
    @DisplayName("Mock consent tokens are accepted only when family.consent.mock.enabled is explicitly true")
    void verifyConsentToken_MockToken_WhenMockEnabled_ReturnsTrue() {
        when(configuration.getMockConsentEnabled()).thenReturn(true);

        boolean result = client.verifyConsentToken("CONSENT-VERIFIED-TEST", "user-1", "FAM-001", "pg.citya", requestInfo);
        assertTrue(result, "Mock token should pass when mockConsentEnabled is explicitly true");
    }

    @Test
    @DisplayName("Verifies DX _verifyOtp contract receives referenceId, otp, and userUuid")
    @SuppressWarnings("unchecked")
    void verifyConsentToken_DxContract_SendsCorrectPayload() {
        when(configuration.getRequesterDxHost()).thenReturn("http://localhost:8280");
        when(configuration.getRequesterDxVerifyOtpPath()).thenReturn("/requester-services-dx/aadhaar/v1/_verifyOtp");

        Map<String, Object> dxResponse = new HashMap<>();
        dxResponse.put("isVerified", true);
        dxResponse.put("maskedAadhaar", "XXXXXXXX1234");

        when(restTemplate.postForObject(eq("http://localhost:8280/requester-services-dx/aadhaar/v1/_verifyOtp"),
                any(HttpEntity.class), eq(Map.class))).thenReturn(dxResponse);

        // Colon-delimited referenceId:otp
        boolean verified = client.verifyConsentToken("REF-98765:654321", "user-123", "FAM-001", "pg.citya", requestInfo);
        assertTrue(verified);

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), entityCaptor.capture(), eq(Map.class));

        Map<String, Object> body = (Map<String, Object>) entityCaptor.getValue().getBody();
        assertEquals("REF-98765", body.get("referenceId"));
        assertEquals("654321", body.get("otp"));
        assertEquals("user-123", body.get("userUuid"));
        assertEquals(requestInfo, body.get("RequestInfo"));
    }

    @Test
    @DisplayName("Fail-closed: Returns false when DX returns isVerified=false")
    @SuppressWarnings("unchecked")
    void verifyConsentToken_DxReturnsNotVerified_ReturnsFalse() {
        when(configuration.getRequesterDxHost()).thenReturn("http://localhost:8280");
        when(configuration.getRequesterDxVerifyOtpPath()).thenReturn("/requester-services-dx/aadhaar/v1/_verifyOtp");

        Map<String, Object> dxResponse = new HashMap<>();
        dxResponse.put("isVerified", false);

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(dxResponse);

        boolean verified = client.verifyConsentToken("REF-1234:111111", "user-1", "FAM-001", "pg.citya", requestInfo);
        assertFalse(verified);
    }

    @Test
    @DisplayName("Security Fail-Closed: Never fall back to true on RestTemplate exceptions (no length >= 6 bypass)")
    @SuppressWarnings("unchecked")
    void verifyConsentToken_OnException_ReturnsFalse() {
        when(configuration.getRequesterDxHost()).thenReturn("http://localhost:8280");
        when(configuration.getRequesterDxVerifyOtpPath()).thenReturn("/requester-services-dx/aadhaar/v1/_verifyOtp");

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("Connection refused"));

        // A token of length >= 6 must FAIL, never pass on exception
        boolean verified = client.verifyConsentToken("LONG-TOKEN-THAT-PREVIOUSLY-PASSED-ON-ERROR", "user-1", "FAM-001", "pg.citya", requestInfo);
        assertFalse(verified, "Must fail closed on exception");
    }

    @Test
    @DisplayName("Security: Verifies token against member's Aadhaar reference")
    @SuppressWarnings("unchecked")
    void verifyConsentToken_AadhaarValidation() {
        when(configuration.getRequesterDxHost()).thenReturn("http://localhost:8280");
        when(configuration.getRequesterDxVerifyOtpPath()).thenReturn("/requester-services-dx/aadhaar/v1/_verifyOtp");

        Map<String, Object> dxResponse = new HashMap<>();
        dxResponse.put("isVerified", true);
        dxResponse.put("maskedAadhaar", "XXXXXXXX4567");

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(dxResponse);

        // 1. Member Aadhaar ending with 4567 should pass
        boolean matchSuccess = client.verifyConsentToken("REF-MATCH:123456", "234567894567", "FAM-001", "pg.citya", requestInfo);
        assertTrue(matchSuccess, "Matching Aadhaar reference must succeed");

        // 2. Member Aadhaar ending with 9999 should FAIL even though DX isVerified=true
        boolean mismatchFailure = client.verifyConsentToken("REF-MISMATCH:123456", "234567899999", "FAM-001", "pg.citya", requestInfo);
        assertFalse(mismatchFailure, "Mismatched Aadhaar reference must fail");
    }

    @Test
    @DisplayName("Security: Prevents token replay across different members or families")
    @SuppressWarnings("unchecked")
    void verifyConsentToken_ReplayPrevention() {
        when(configuration.getRequesterDxHost()).thenReturn("http://localhost:8280");
        when(configuration.getRequesterDxVerifyOtpPath()).thenReturn("/requester-services-dx/aadhaar/v1/_verifyOtp");

        Map<String, Object> dxResponse = new HashMap<>();
        dxResponse.put("isVerified", true);
        dxResponse.put("maskedAadhaar", "XXXXXXXX1234");

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(dxResponse);

        // First use for citizen-Alice in FAM-001: Success
        boolean firstUse = client.verifyConsentToken("REF-REPLAY-TOKEN:123456", "citizen-Alice", "FAM-001", "pg.citya", requestInfo);
        assertTrue(firstUse);

        // Replay attempt using same token for citizen-Bob in FAM-001: REJECTED
        boolean replayDiffMember = client.verifyConsentToken("REF-REPLAY-TOKEN:123456", "citizen-Bob", "FAM-001", "pg.citya", requestInfo);
        assertFalse(replayDiffMember, "Replay attack for a different member must be rejected");

        // Replay attempt using same token for citizen-Alice in different family FAM-002: REJECTED
        boolean replayDiffFamily = client.verifyConsentToken("REF-REPLAY-TOKEN:123456", "citizen-Alice", "FAM-002", "pg.citya", requestInfo);
        assertFalse(replayDiffFamily, "Replay attack for a different family must be rejected");
    }
}
