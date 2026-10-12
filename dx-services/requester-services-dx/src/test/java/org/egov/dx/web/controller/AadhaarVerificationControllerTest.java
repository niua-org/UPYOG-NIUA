package org.egov.dx.web.controller;

import org.egov.common.contract.request.RequestInfo;
import org.egov.dx.adapter.vendor.AadhaarGatewayAdapter;
import org.egov.dx.adapter.vendor.dto.VendorSendOtpRequest;
import org.egov.dx.adapter.vendor.dto.VendorSendOtpResponse;
import org.egov.dx.adapter.vendor.dto.VendorVerifyOtpRequest;
import org.egov.dx.adapter.vendor.dto.VendorVerifyOtpSuccessResponse;
import org.egov.dx.util.AadhaarConstants;
import org.egov.dx.validator.AadhaarVerificationValidator;
import org.egov.dx.web.models.AadhaarVerificationRequest;
import org.egov.dx.web.models.AadhaarVerificationResponse;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AadhaarVerificationControllerTest {

    private AadhaarGatewayAdapter gatewayAdapter;
    private AadhaarVerificationValidator validator;
    private AadhaarVerificationController controller;

    @BeforeEach
    void setUp() {
        gatewayAdapter = mock(AadhaarGatewayAdapter.class);
        validator = mock(AadhaarVerificationValidator.class);
        controller = new AadhaarVerificationController(gatewayAdapter, validator);
    }

    @Test
    void testSendOtp_Success() {
        AadhaarVerificationRequest request = AadhaarVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .aadhaarNumber("548796521458")
                .build();

        VendorSendOtpResponse mockVendorRes = VendorSendOtpResponse.builder()
                .code(200)
                .transactionId("TXN-12345")
                .data(VendorSendOtpResponse.VendorSendOtpResponseDetails.builder()
                        .referenceId(789101L)
                        .message("OTP sent successfully")
                        .build())
                .build();

        when(gatewayAdapter.sendOtp(any(VendorSendOtpRequest.class))).thenReturn(mockVendorRes);

        ResponseEntity<AadhaarVerificationResponse> responseEntity = controller.sendOtp(request);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        AadhaarVerificationResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals("OTP_SENT", body.getStatus());
        assertEquals("789101", body.getReferenceId());
        assertEquals("TXN-12345", body.getTxnId());
        verify(validator, times(1)).validateSendOtpRequest(request);
        verify(gatewayAdapter, times(1)).sendOtp(any(VendorSendOtpRequest.class));
    }

    @Test
    void testSendOtp_ValidationError_ReturnsBadRequest() {
        AadhaarVerificationRequest request = AadhaarVerificationRequest.builder().build();

        doThrow(new CustomException(AadhaarConstants.ERR_CODE_INVALID_AADHAAR, "Invalid Aadhaar"))
                .when(validator).validateSendOtpRequest(request);

        ResponseEntity<AadhaarVerificationResponse> responseEntity = controller.sendOtp(request);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        AadhaarVerificationResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals(AadhaarConstants.ERR_CODE_INVALID_AADHAAR, body.getStatus());
        assertEquals("Invalid Aadhaar", body.getMessage());
        verify(gatewayAdapter, never()).sendOtp(any());
    }

    @Test
    void testVerifyOtp_Success() {
        AadhaarVerificationRequest request = AadhaarVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .referenceId("789101")
                .otp("123456")
                .build();

        VendorVerifyOtpSuccessResponse mockVendorRes = VendorVerifyOtpSuccessResponse.builder()
                .code(200)
                .transactionId("TXN-12345")
                .data(VendorVerifyOtpSuccessResponse.VendorVerifyOtpSuccessResponseDetails.builder()
                        .status("VALID")
                        .aadhaarNumber("XXXXXXXX1234")
                        .name("John Doe")
                        .dob("1990-01-01")
                        .gender("M")
                        .build())
                .build();

        when(gatewayAdapter.verifyOtp(any(VendorVerifyOtpRequest.class))).thenReturn(mockVendorRes);

        ResponseEntity<AadhaarVerificationResponse> responseEntity = controller.verifyOtp(request);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        AadhaarVerificationResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals("VERIFIED", body.getStatus());
        assertTrue(body.getIsVerified());
        assertNotNull(body.getDemographics());
        assertEquals("John Doe", body.getDemographics().getName());
        verify(validator, times(1)).validateVerifyOtpRequest(request);
    }

    @Test
    void testVerifyOtp_GatewayFailure_ReturnsBadRequest() {
        AadhaarVerificationRequest request = AadhaarVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .referenceId("789101")
                .otp("000000")
                .build();

        doThrow(new CustomException(AadhaarConstants.STATUS_INVALID_OTP, "Invalid OTP"))
                .when(gatewayAdapter).verifyOtp(any(VendorVerifyOtpRequest.class));

        ResponseEntity<AadhaarVerificationResponse> responseEntity = controller.verifyOtp(request);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        AadhaarVerificationResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals(AadhaarConstants.STATUS_INVALID_OTP, body.getStatus());
        assertEquals("Invalid OTP", body.getMessage());
    }

    @Test
    void testVerifyOtp_ResyncFlow_Success() {
        AadhaarVerificationRequest request = AadhaarVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .referenceId("789101")
                .otp("123456")
                .isResync(true)
                .userUuid("usr-12345")
                .build();

        VendorVerifyOtpSuccessResponse mockVendorRes = VendorVerifyOtpSuccessResponse.builder()
                .code(200)
                .transactionId("TXN-RESYNC-999")
                .data(VendorVerifyOtpSuccessResponse.VendorVerifyOtpSuccessResponseDetails.builder()
                        .status("VALID")
                        .aadhaarNumber("XXXXXXXX1234")
                        .name("Jane Citizen")
                        .dob("1995-08-15")
                        .gender("F")
                        .careOf("D/O John Citizen")
                        .build())
                .build();

        when(gatewayAdapter.verifyOtp(any(VendorVerifyOtpRequest.class))).thenReturn(mockVendorRes);

        ResponseEntity<AadhaarVerificationResponse> responseEntity = controller.verifyOtp(request);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        AadhaarVerificationResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals("VERIFIED", body.getStatus());
        assertTrue(body.getIsVerified());
        assertNotNull(body.getDemographics());
        assertEquals("Jane Citizen", body.getDemographics().getName());
        assertEquals("D/O John Citizen", body.getDemographics().getCareOf());
        verify(validator, times(1)).validateVerifyOtpRequest(request);
    }
}
