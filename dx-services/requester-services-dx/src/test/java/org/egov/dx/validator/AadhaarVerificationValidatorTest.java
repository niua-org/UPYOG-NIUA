package org.egov.dx.validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.dx.util.AadhaarConstants;
import org.egov.dx.web.models.AadhaarVerificationRequest;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AadhaarVerificationValidatorTest {

    private AadhaarVerificationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AadhaarVerificationValidator();
    }

    @Test
    void testValidateSendOtpRequest_Success() {
        AadhaarVerificationRequest request = AadhaarVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .aadhaarNumber("548796521458")
                .build();

        assertDoesNotThrow(() -> validator.validateSendOtpRequest(request));
    }

    @Test
    void testValidateSendOtpRequest_NullRequest() {
        CustomException ex = assertThrows(CustomException.class,
                () -> validator.validateSendOtpRequest(null));

        assertEquals(AadhaarConstants.ERR_CODE_INVALID_REQUEST, ex.getCode());
    }

    @Test
    void testValidateSendOtpRequest_NullRequestInfo() {
        AadhaarVerificationRequest request = AadhaarVerificationRequest.builder()
                .aadhaarNumber("548796521458")
                .build();

        CustomException ex = assertThrows(CustomException.class,
                () -> validator.validateSendOtpRequest(request));

        assertEquals(AadhaarConstants.ERR_CODE_INVALID_REQUEST_INFO, ex.getCode());
    }

    @Test
    void testValidateSendOtpRequest_InvalidAadhaarNumber() {
        // Starts with 0 (invalid per UIDAI)
        AadhaarVerificationRequest request = AadhaarVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .aadhaarNumber("012345678901")
                .build();

        CustomException ex = assertThrows(CustomException.class,
                () -> validator.validateSendOtpRequest(request));

        assertEquals(AadhaarConstants.ERR_CODE_INVALID_AADHAAR, ex.getCode());
    }

    @Test
    void testValidateVerifyOtpRequest_Success() {
        AadhaarVerificationRequest request = AadhaarVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .referenceId("123456")
                .otp("654321")
                .build();

        assertDoesNotThrow(() -> validator.validateVerifyOtpRequest(request));
    }

    @Test
    void testValidateVerifyOtpRequest_MissingReferenceId() {
        AadhaarVerificationRequest request = AadhaarVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .otp("654321")
                .build();

        CustomException ex = assertThrows(CustomException.class,
                () -> validator.validateVerifyOtpRequest(request));

        assertEquals(AadhaarConstants.ERR_CODE_MISSING_REFERENCE_ID, ex.getCode());
    }

    @Test
    void testValidateVerifyOtpRequest_InvalidOtpLength() {
        AadhaarVerificationRequest request = AadhaarVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .referenceId("123456")
                .otp("1234") // only 4 digits
                .build();

        CustomException ex = assertThrows(CustomException.class,
                () -> validator.validateVerifyOtpRequest(request));

        assertEquals(AadhaarConstants.ERR_CODE_INVALID_OTP_FORMAT, ex.getCode());
    }
}
