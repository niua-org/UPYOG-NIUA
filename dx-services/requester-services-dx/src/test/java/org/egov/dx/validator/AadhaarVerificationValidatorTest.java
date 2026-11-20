package org.egov.dx.validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.dx.util.AadhaarConstants;
import org.egov.dx.web.models.AadhaarVerificationRequest;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AadhaarVerificationValidatorTest {

    private AadhaarVerificationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AadhaarVerificationValidator();
    }

    /**
     * Tests Validate Send Otp Request under the success scenario.
     *
     * Verifies that {@code CustomException} is thrown when success.
     */
    @Test
    void testValidateSendOtpRequest_Success() {
        AadhaarVerificationRequest request = AadhaarVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .aadhaarNumber("548796521452")
                .build();

        assertDoesNotThrow(() -> validator.validateSendOtpRequest(request));
    }

    /**
     * Tests Validate Send Otp Request under the null request scenario.
     *
     * Verifies that {@code CustomException} is thrown when null request.
     */
    @Test
    void testValidateSendOtpRequest_NullRequest() {
        CustomException ex = assertThrows(CustomException.class,
                () -> validator.validateSendOtpRequest(null));

        assertEquals(AadhaarConstants.ERR_CODE_INVALID_REQUEST, ex.getCode());
    }

    /**
     * Tests Validate Send Otp Request under the null request info scenario.
     *
     * Verifies that {@code CustomException} is thrown when null request info.
     */
    @Test
    void testValidateSendOtpRequest_NullRequestInfo() {
        AadhaarVerificationRequest request = AadhaarVerificationRequest.builder()
                .aadhaarNumber("548796521458")
                .build();

        CustomException ex = assertThrows(CustomException.class,
                () -> validator.validateSendOtpRequest(request));

        assertEquals(AadhaarConstants.ERR_CODE_INVALID_REQUEST_INFO, ex.getCode());
    }

    /**
     * Tests Validate Send Otp Request under the invalid aadhaar number scenario.
     *
     * Verifies that {@code CustomException} is thrown when invalid aadhaar number.
     */
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

    /**
     * Tests Validate Verify Otp Request under the success scenario.
     *
     * Verifies that {@code CustomException} is thrown when success.
     */
    @Test
    void testValidateVerifyOtpRequest_Success() {
        AadhaarVerificationRequest request = AadhaarVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .referenceId("123456")
                .otp("654321")
                .build();

        assertDoesNotThrow(() -> validator.validateVerifyOtpRequest(request));
    }

    /**
     * Tests Validate Verify Otp Request under the missing reference id scenario.
     *
     * Verifies that {@code CustomException} is thrown when missing reference id.
     */
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

    /**
     * Tests Validate Verify Otp Request under the invalid otp length scenario.
     *
     * Verifies that {@code CustomException} is thrown when invalid otp length.
     */
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
