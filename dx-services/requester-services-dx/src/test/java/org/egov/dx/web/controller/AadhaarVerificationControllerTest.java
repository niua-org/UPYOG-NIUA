package org.egov.dx.web.controller;

import org.egov.common.contract.request.RequestInfo;
import org.egov.dx.adapter.vendor.AadhaarGatewayAdapter;
import org.egov.dx.adapter.vendor.dto.VendorSendOtpRequest;
import org.egov.dx.adapter.vendor.dto.VendorSendOtpResponse;
import org.egov.dx.adapter.vendor.dto.VendorVerifyOtpRequest;
import org.egov.dx.adapter.vendor.dto.VendorVerifyOtpSuccessResponse;
import org.egov.dx.service.AadhaarSessionManager;
import org.egov.dx.service.UserRegistryService;
import org.egov.dx.service.impl.AadhaarSessionManagerImpl;
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

    @Test
    void testValidateChecksum_Success() {
        AadhaarVerificationRequest request = AadhaarVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .aadhaarNumber("548796521458")
                .build();

        when(validator.validateVerhoeffChecksum(request)).thenReturn(true);

        ResponseEntity<AadhaarVerificationResponse> responseEntity = controller.validateChecksum(request);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        AadhaarVerificationResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals(AadhaarConstants.STATUS_VALID, body.getStatus());
        assertTrue(body.getIsVerified());
        assertEquals("XXXXXXXX1458", body.getMaskedAadhaar());
        assertEquals(AadhaarConstants.MSG_VALID_AADHAAR_CHECKSUM, body.getMessage());
        verify(validator, times(1)).validateVerhoeffChecksum(request);
    }

    @Test
    void testValidateChecksum_InvalidChecksum() {
        AadhaarVerificationRequest request = AadhaarVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .aadhaarNumber("222222222222")
                .build();

        when(validator.validateVerhoeffChecksum(request)).thenReturn(false);

        ResponseEntity<AadhaarVerificationResponse> responseEntity = controller.validateChecksum(request);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        AadhaarVerificationResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals(AadhaarConstants.STATUS_INVALID, body.getStatus());
        assertFalse(body.getIsVerified());
        assertEquals("XXXXXXXX2222", body.getMaskedAadhaar());
        assertEquals(AadhaarConstants.MSG_INVALID_AADHAAR_CHECKSUM, body.getMessage());
        verify(validator, times(1)).validateVerhoeffChecksum(request);
    }

    @Test
    void testValidateChecksum_ValidationError_ReturnsBadRequest() {
        AadhaarVerificationRequest request = AadhaarVerificationRequest.builder().build();

        doThrow(new CustomException(AadhaarConstants.ERR_CODE_INVALID_AADHAAR, "Invalid Aadhaar format"))
                .when(validator).validateVerhoeffChecksum(request);

        ResponseEntity<AadhaarVerificationResponse> responseEntity = controller.validateChecksum(request);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        AadhaarVerificationResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals(AadhaarConstants.ERR_CODE_INVALID_AADHAAR, body.getStatus());
        assertFalse(body.getIsVerified());
        assertEquals("Invalid Aadhaar format", body.getMessage());
    }

    @Test
    void testVerifyOtp_WithExistingCitizenInEgovUser_ReturnsUserUuid() {
        UserRegistryService userRegistryService = mock(UserRegistryService.class);
        AadhaarSessionManager sessionManager = new AadhaarSessionManagerImpl(3, null);
        AadhaarVerificationController customController = new AadhaarVerificationController(
                gatewayAdapter, validator, sessionManager, userRegistryService);

        AadhaarVerificationRequest request = AadhaarVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .referenceId("998877")
                .otp("123456")
                .aadhaarNumber("548796521458")
                .tenantId("pg.citya")
                .build();

        VendorVerifyOtpSuccessResponse mockVendorRes = VendorVerifyOtpSuccessResponse.builder()
                .code(200)
                .transactionId("TXN-VERIFY-1")
                .data(VendorVerifyOtpSuccessResponse.VendorVerifyOtpSuccessResponseDetails.builder()
                        .status("VALID")
                        .aadhaarNumber("XXXXXXXX1458")
                        .name("Citizen Name")
                        .build())
                .build();

        when(gatewayAdapter.verifyOtp(any(VendorVerifyOtpRequest.class))).thenReturn(mockVendorRes);
        when(userRegistryService.searchUserUuidByAadhaar(eq("548796521458"), eq("pg.citya"), any()))
                .thenReturn("0b90ae1c-7506-4114-b409-d48093e5db45");

        ResponseEntity<AadhaarVerificationResponse> responseEntity = customController.verifyOtp(request);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        AadhaarVerificationResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertTrue(body.getIsVerified());
        assertEquals("0b90ae1c-7506-4114-b409-d48093e5db45", body.getUserUuid());
        assertEquals("XXXXXXXX1458", body.getMaskedAadhaar());
        verify(userRegistryService, times(1)).searchUserUuidByAadhaar(eq("548796521458"), eq("pg.citya"), any());
    }

    @Test
    void testSendOtp_ThenVerifyOtp_SessionCarriesAadhaarAndResolvesUserUuid() {
        UserRegistryService userRegistryService = mock(UserRegistryService.class);
        AadhaarSessionManager sessionManager = new AadhaarSessionManagerImpl(3, null);
        AadhaarVerificationController customController = new AadhaarVerificationController(
                gatewayAdapter, validator, sessionManager, userRegistryService);

        // 1. Send OTP
        AadhaarVerificationRequest sendReq = AadhaarVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .aadhaarNumber("548796521458")
                .tenantId("pg.citya")
                .build();

        VendorSendOtpResponse mockSendRes = VendorSendOtpResponse.builder()
                .code(200)
                .transactionId("TXN-SEND-1")
                .data(VendorSendOtpResponse.VendorSendOtpResponseDetails.builder()
                        .referenceId(445566L)
                        .message("OTP sent successfully")
                        .build())
                .build();

        when(gatewayAdapter.sendOtp(any())).thenReturn(mockSendRes);
        ResponseEntity<AadhaarVerificationResponse> sendResEntity = customController.sendOtp(sendReq);
        assertEquals(HttpStatus.OK, sendResEntity.getStatusCode());

        // 2. Verify OTP with ONLY referenceId (no aadhaarNumber provided in verify request)
        AadhaarVerificationRequest verifyReq = AadhaarVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .referenceId("445566")
                .otp("123456")
                .build();

        VendorVerifyOtpSuccessResponse mockVerifyRes = VendorVerifyOtpSuccessResponse.builder()
                .code(200)
                .transactionId("TXN-VERIFY-2")
                .data(VendorVerifyOtpSuccessResponse.VendorVerifyOtpSuccessResponseDetails.builder()
                        .status("VALID")
                        .aadhaarNumber("XXXXXXXX1234") // Gateway returns default mock
                        .name("Citizen Name")
                        .build())
                .build();

        when(gatewayAdapter.verifyOtp(any())).thenReturn(mockVerifyRes);
        when(userRegistryService.searchUserUuidByAadhaar(eq("548796521458"), any(), any()))
                .thenReturn("0b90ae1c-7506-4114-b409-d48093e5db45");

        ResponseEntity<AadhaarVerificationResponse> verifyResEntity = customController.verifyOtp(verifyReq);

        assertNotNull(verifyResEntity);
        assertEquals(HttpStatus.OK, verifyResEntity.getStatusCode());
        AadhaarVerificationResponse body = verifyResEntity.getBody();
        assertNotNull(body);
        assertTrue(body.getIsVerified());
        // Masked Aadhaar was enriched from session
        assertEquals("XXXXXXXX1458", body.getMaskedAadhaar());
        // User UUID was successfully looked up using session Aadhaar
        assertEquals("0b90ae1c-7506-4114-b409-d48093e5db45", body.getUserUuid());
    }

    @Test
    void testVerifyOtp_CitizenNotFoundInEgovUser_AutoProvisionsAndReturnsNewUserUuid() {
        UserRegistryService userRegistryService = mock(UserRegistryService.class);
        AadhaarSessionManager sessionManager = new AadhaarSessionManagerImpl(3, null);
        AadhaarVerificationController customController = new AadhaarVerificationController(
                gatewayAdapter, validator, sessionManager, userRegistryService);

        AadhaarVerificationRequest request = AadhaarVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .referenceId("112233")
                .otp("123456")
                .aadhaarNumber("999988887777")
                .build();

        VendorVerifyOtpSuccessResponse mockVendorRes = VendorVerifyOtpSuccessResponse.builder()
                .code(200)
                .transactionId("TXN-NOT-FOUND")
                .data(VendorVerifyOtpSuccessResponse.VendorVerifyOtpSuccessResponseDetails.builder()
                        .status("VALID")
                        .aadhaarNumber("XXXXXXXX7777")
                        .name("New Citizen")
                        .build())
                .build();

        when(gatewayAdapter.verifyOtp(any(VendorVerifyOtpRequest.class))).thenReturn(mockVendorRes);
        when(userRegistryService.searchUserUuidByAadhaar(eq("999988887777"), any(), any()))
                .thenReturn(null);
        when(userRegistryService.resolveOrCreateUserUuid(eq("999988887777"), any(), any(), any()))
                .thenReturn("newly-provisioned-citizen-uuid-999");

        ResponseEntity<AadhaarVerificationResponse> responseEntity = customController.verifyOtp(request);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        AadhaarVerificationResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertTrue(body.getIsVerified());
        assertEquals("newly-provisioned-citizen-uuid-999", body.getUserUuid());
    }
}
