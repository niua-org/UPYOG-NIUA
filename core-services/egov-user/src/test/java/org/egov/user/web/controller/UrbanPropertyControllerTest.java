package org.egov.user.web.controller;

import org.egov.user.domain.service.asset.UrbanPropertyService;
import org.egov.user.web.contract.property.UrbanPropertyConsentValidateRequest;
import org.egov.user.web.contract.property.UrbanPropertyConsentValidateResponse;
import org.egov.user.web.contract.property.UrbanPropertyOtpRequest;
import org.egov.user.web.contract.property.UrbanPropertyOtpResponse;
import org.egov.user.web.contract.property.UrbanPropertyOtpVerifyRequest;
import org.egov.user.web.contract.property.UrbanPropertyOtpVerifyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests validating endpoint invocation and contract response mapping in {@link UrbanPropertyController}.
 */
public class UrbanPropertyControllerTest {

    private UrbanPropertyService urbanPropertyService;
    private UrbanPropertyController controller;

    @BeforeEach
    void setUp() {
        urbanPropertyService = mock(UrbanPropertyService.class);
        controller = new UrbanPropertyController(urbanPropertyService);
    }

    /**
     * Verifies that POST /otp/_send successfully returns HTTP 200 with OTP reference details.
     */
    @Test
    @DisplayName("Should return HTTP 200 for sending property OTP")
    void testSendUrbanPropertyOtp() {
        UrbanPropertyOtpResponse mockResponse = UrbanPropertyOtpResponse.builder()
                .referenceId("REF-1")
                .propertyId("PT-1")
                .maskedMobileNumber("98******10")
                .build();
        when(urbanPropertyService.sendUrbanPropertyOtp(any())).thenReturn(mockResponse);

        UrbanPropertyOtpRequest request = UrbanPropertyOtpRequest.builder()
                .tenantId("pb.amritsar")
                .propertyId("PT-1")
                .build();

        ResponseEntity<UrbanPropertyOtpResponse> entity = controller.sendUrbanPropertyOtp(request);

        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertNotNull(entity.getBody());
        assertEquals("REF-1", entity.getBody().getReferenceId());
        verify(urbanPropertyService, times(1)).sendUrbanPropertyOtp(any());
    }

    /**
     * Verifies that POST /otp/_verify successfully returns HTTP 200 with consent token on valid OTP.
     */
    @Test
    @DisplayName("Should return HTTP 200 for verifying property OTP")
    void testVerifyUrbanPropertyOtp() {
        UrbanPropertyOtpVerifyResponse mockResponse = UrbanPropertyOtpVerifyResponse.builder()
                .verified(true)
                .propertyConsentToken("PROP-CONSENT-123")
                .propertyId("PT-1")
                .build();
        when(urbanPropertyService.verifyUrbanPropertyOtp(any())).thenReturn(mockResponse);

        UrbanPropertyOtpVerifyRequest request = UrbanPropertyOtpVerifyRequest.builder()
                .tenantId("pb.amritsar")
                .propertyId("PT-1")
                .referenceId("REF-1")
                .otp("123456")
                .build();

        ResponseEntity<UrbanPropertyOtpVerifyResponse> entity = controller.verifyUrbanPropertyOtp(request);

        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertNotNull(entity.getBody());
        assertTrue(entity.getBody().getVerified());
        assertEquals("PROP-CONSENT-123", entity.getBody().getPropertyConsentToken());
        verify(urbanPropertyService, times(1)).verifyUrbanPropertyOtp(any());
    }

    /**
     * Verifies that POST /consent/_validate successfully returns HTTP 200 with token verification status.
     */
    @Test
    @DisplayName("Should return HTTP 200 for validating consent token")
    void testValidateConsentToken() {
        UrbanPropertyConsentValidateResponse mockResponse = UrbanPropertyConsentValidateResponse.builder()
                .valid(true)
                .propertyId("PT-1")
                .build();
        when(urbanPropertyService.validateConsentToken(any())).thenReturn(mockResponse);

        UrbanPropertyConsentValidateRequest request = UrbanPropertyConsentValidateRequest.builder()
                .propertyId("PT-1")
                .consentToken("PROP-CONSENT-123")
                .build();

        ResponseEntity<UrbanPropertyConsentValidateResponse> entity = controller.validateConsentToken(request);

        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertNotNull(entity.getBody());
        assertTrue(entity.getBody().getValid());
        verify(urbanPropertyService, times(1)).validateConsentToken(any());
    }
}
