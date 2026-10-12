package org.egov.dx.validator;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.dx.util.AadhaarConstants;
import org.egov.dx.web.models.AadhaarVerificationRequest;
import org.egov.tracer.model.CustomException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Dedicated validator responsible for business and format validation of Aadhaar requests.
 *
 * <p>Enforces the <b>Single Responsibility Principle (SRP)</b> by isolating validation rules
 * and input sanity checks away from HTTP controller orchestration concerns.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class AadhaarVerificationValidator {

    private static final Pattern AADHAAR_PATTERN = Pattern.compile(AadhaarConstants.REGEX_AADHAAR);
    private static final Pattern OTP_PATTERN = Pattern.compile(AadhaarConstants.REGEX_OTP);

    /**
     * Validates incoming request for the {@code /_sendOtp} operation.
     *
     * @param request the verification request containing RequestInfo and 12-digit Aadhaar number
     * @throws CustomException if request, RequestInfo, or Aadhaar number is null/invalid
     */
    public void validateSendOtpRequest(AadhaarVerificationRequest request) {
        if (request == null) {
            log.error("Aadhaar _sendOtp validation failed: {}", AadhaarConstants.MSG_REQUEST_BODY_NULL);
            throw new CustomException(AadhaarConstants.ERR_CODE_INVALID_REQUEST, AadhaarConstants.MSG_REQUEST_BODY_NULL);
        }

        if (request.getRequestInfo() == null) {
            log.error("Aadhaar _sendOtp validation failed: {}", AadhaarConstants.MSG_REQUEST_INFO_MANDATORY);
            throw new CustomException(AadhaarConstants.ERR_CODE_INVALID_REQUEST_INFO, AadhaarConstants.MSG_REQUEST_INFO_MANDATORY);
        }

        String aadhaarNumber = request.getAadhaarNumber();
        if (StringUtils.isBlank(aadhaarNumber) || !AADHAAR_PATTERN.matcher(aadhaarNumber).matches()) {
            log.warn("Aadhaar _sendOtp validation failed: {}", AadhaarConstants.MSG_INVALID_AADHAAR);
            throw new CustomException(AadhaarConstants.ERR_CODE_INVALID_AADHAAR, AadhaarConstants.MSG_INVALID_AADHAAR);
        }
    }

    /**
     * Validates incoming request for the {@code /_verifyOtp} operation.
     *
     * @param request the verification request containing RequestInfo, referenceId, and 6-digit OTP
     * @throws CustomException if request, RequestInfo, referenceId, or OTP is missing/invalid
     */
    public void validateVerifyOtpRequest(AadhaarVerificationRequest request) {
        if (request == null) {
            log.error("Aadhaar _verifyOtp validation failed: {}", AadhaarConstants.MSG_REQUEST_BODY_NULL);
            throw new CustomException(AadhaarConstants.ERR_CODE_INVALID_REQUEST, AadhaarConstants.MSG_REQUEST_BODY_NULL);
        }

        if (request.getRequestInfo() == null) {
            log.error("Aadhaar _verifyOtp validation failed: {}", AadhaarConstants.MSG_REQUEST_INFO_MANDATORY);
            throw new CustomException(AadhaarConstants.ERR_CODE_INVALID_REQUEST_INFO, AadhaarConstants.MSG_REQUEST_INFO_MANDATORY);
        }

        String referenceId = StringUtils.isNotBlank(request.getReferenceId())
                ? request.getReferenceId()
                : request.getTxnId();

        if (StringUtils.isBlank(referenceId)) {
            log.warn("Aadhaar _verifyOtp validation failed: {}", AadhaarConstants.MSG_MISSING_REFERENCE_ID);
            throw new CustomException(AadhaarConstants.ERR_CODE_MISSING_REFERENCE_ID, AadhaarConstants.MSG_MISSING_REFERENCE_ID);
        }

        String otp = request.getOtp();
        if (StringUtils.isBlank(otp) || !OTP_PATTERN.matcher(otp).matches()) {
            log.warn("Aadhaar _verifyOtp validation failed: Invalid OTP for referenceId: {}", referenceId);
            throw new CustomException(AadhaarConstants.ERR_CODE_INVALID_OTP_FORMAT, AadhaarConstants.MSG_INVALID_OTP);
        }
    }
}
