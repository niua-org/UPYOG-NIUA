package org.egov.dx.web.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.dx.adapter.vendor.AadhaarGatewayAdapter;
import org.egov.dx.adapter.vendor.dto.VendorSendOtpRequest;
import org.egov.dx.adapter.vendor.dto.VendorSendOtpResponse;
import org.egov.dx.adapter.vendor.dto.VendorVerifyOtpRequest;
import org.egov.dx.adapter.vendor.dto.VendorVerifyOtpSuccessResponse;
import org.egov.dx.util.AadhaarConstants;
import org.egov.dx.validator.AadhaarVerificationValidator;
import org.egov.dx.web.models.AadhaarVerificationRequest;
import org.egov.dx.web.models.AadhaarVerificationResponse;
import org.egov.dx.web.models.ResponseInfoFactory;
import org.egov.dx.web.models.UserDemographics;
import org.egov.tracer.model.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing Aadhaar verification endpoints for requester-services-dx.
 *
 * <p>Adheres 100% to SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Solely handles HTTP transport orchestration. Validation is delegated to {@link AadhaarVerificationValidator}.</li>
 *   <li><b>Dependency Inversion Principle (DIP):</b> Depends on the {@link AadhaarGatewayAdapter} interface abstraction via constructor injection, never on concrete classes.</li>
 *   <li><b>Open/Closed Principle (OCP):</b> New gateway adapters can be swapped or added via configuration without changing this controller.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/aadhaar/v1")
@Slf4j
public class AadhaarVerificationController {

    private final AadhaarGatewayAdapter gatewayAdapter;
    private final AadhaarVerificationValidator validator;

    /**
     * Constructor injection satisfying the Dependency Inversion Principle (DIP).
     *
     * @param gatewayAdapter abstraction for the configured vendor gateway adapter
     * @param validator component handling request validation
     */
    public AadhaarVerificationController(AadhaarGatewayAdapter gatewayAdapter,
                                         AadhaarVerificationValidator validator) {
        this.gatewayAdapter = gatewayAdapter;
        this.validator = validator;
    }

    /**
     * Dispatches an OTP to the citizen's Aadhaar-linked mobile number.
     *
     * @param request {@link AadhaarVerificationRequest} containing RequestInfo and 12-digit aadhaarNumber
     * @return {@link ResponseEntity} wrapping {@link AadhaarVerificationResponse} with session referenceId and status
     */
    @PostMapping("/_sendOtp")
    public ResponseEntity<AadhaarVerificationResponse> sendOtp(
            @RequestBody @Valid AadhaarVerificationRequest request) {

        // 1. Delegate Validation (SRP)
        try {
            validator.validateSendOtpRequest(request);
        } catch (CustomException ce) {
            return new ResponseEntity<>(createErrorResponse(request, ce.getCode(), ce.getMessage()), HttpStatus.BAD_REQUEST);
        }

        String aadhaarNumber = request.getAadhaarNumber();
        String maskedAadhaar = maskAadhaar(aadhaarNumber);
        log.info("Processing Aadhaar Send OTP for masked Aadhaar: {}, apiId: {}, isResync: {}, userUuid: {}",
                maskedAadhaar, request.getRequestInfo().getApiId(), request.getIsResync(), request.getUserUuid());

        // 2. Map to Vendor DTO
        VendorSendOtpRequest vendorReq = VendorSendOtpRequest.builder()
                .aadhaarNumber(aadhaarNumber)
                .consent(AadhaarConstants.DEFAULT_CONSENT)
                .reason(AadhaarConstants.DEFAULT_REASON)
                .build();

        // 3. Invoke Gateway Adapter Abstraction (DIP)
        VendorSendOtpResponse vendorRes;
        try {
            vendorRes = gatewayAdapter.sendOtp(vendorReq);
        } catch (CustomException ce) {
            log.error("Aadhaar _sendOtp failed at gateway: {} - {}", ce.getCode(), ce.getMessage());
            return new ResponseEntity<>(createErrorResponse(request, ce.getCode(), ce.getMessage()), HttpStatus.BAD_REQUEST);
        }

        // 4. Validate Gateway Response
        if (vendorRes == null || vendorRes.getData() == null) {
            log.error("Gateway returned empty response for masked Aadhaar: {}", maskedAadhaar);
            return new ResponseEntity<>(createErrorResponse(request, AadhaarConstants.ERR_CODE_GATEWAY_ERROR, AadhaarConstants.MSG_GATEWAY_DISPATCH_FAILED), HttpStatus.BAD_GATEWAY);
        }

        String referenceId = vendorRes.getData().getReferenceId() != null
                ? String.valueOf(vendorRes.getData().getReferenceId())
                : null;
        String txnId = vendorRes.getTransactionId();
        String message = StringUtils.isNotBlank(vendorRes.getData().getMessage())
                ? vendorRes.getData().getMessage()
                : AadhaarConstants.MSG_OTP_SENT_SUCCESS;

        log.info("Aadhaar OTP dispatched successfully. maskedAadhaar: {}, referenceId: {}, txnId: {}",
                maskedAadhaar, referenceId, txnId);

        // 5. Map to UPYOG Controller Response
        ResponseInfo responseInfo = ResponseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true);

        AadhaarVerificationResponse response = AadhaarVerificationResponse.builder()
                .responseInfo(responseInfo)
                .txnId(txnId)
                .referenceId(referenceId)
                .status(AadhaarConstants.STATUS_OTP_SENT)
                .message(message)
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Verifies the citizen-submitted OTP against the active session reference ID.
     *
     * @param request {@link AadhaarVerificationRequest} containing RequestInfo, referenceId, and 6-digit OTP
     * @return {@link ResponseEntity} wrapping {@link AadhaarVerificationResponse} with verification result and demographic details
     */
    @PostMapping("/_verifyOtp")
    public ResponseEntity<AadhaarVerificationResponse> verifyOtp(
            @RequestBody @Valid AadhaarVerificationRequest request) {

        // 1. Delegate Validation (SRP)
        try {
            validator.validateVerifyOtpRequest(request);
        } catch (CustomException ce) {
            return new ResponseEntity<>(createErrorResponse(request, ce.getCode(), ce.getMessage()), HttpStatus.BAD_REQUEST);
        }

        String referenceId = StringUtils.isNotBlank(request.getReferenceId())
                ? request.getReferenceId()
                : request.getTxnId();

        log.info("Processing Aadhaar Verify OTP for referenceId: {}, apiId: {}, isResync: {}, userUuid: {}",
                referenceId, request.getRequestInfo().getApiId(), request.getIsResync(), request.getUserUuid());

        // 2. Map to Vendor DTO
        Long vendorRefId = null;
        try {
            vendorRefId = Long.valueOf(referenceId);
        } catch (NumberFormatException e) {
            log.debug("Reference ID is non-numeric string: {}", referenceId);
        }

        VendorVerifyOtpRequest vendorReq = VendorVerifyOtpRequest.builder()
                .referenceId(vendorRefId)
                .otp(request.getOtp())
                .build();

        // 3. Invoke Gateway Adapter Abstraction (DIP)
        VendorVerifyOtpSuccessResponse vendorRes;
        try {
            vendorRes = gatewayAdapter.verifyOtp(vendorReq);
        } catch (CustomException ce) {
            log.warn("Aadhaar _verifyOtp failed at gateway: {} - {}", ce.getCode(), ce.getMessage());
            return new ResponseEntity<>(createErrorResponse(request, ce.getCode(), ce.getMessage()), HttpStatus.BAD_REQUEST);
        }

        // 4. Validate Gateway Response
        if (vendorRes == null || vendorRes.getData() == null) {
            log.error("Gateway returned empty response during OTP verification for referenceId: {}", referenceId);
            return new ResponseEntity<>(createErrorResponse(request, AadhaarConstants.ERR_CODE_GATEWAY_ERROR, AadhaarConstants.MSG_GATEWAY_NO_RESPONSE), HttpStatus.BAD_GATEWAY);
        }

        boolean isValid = Integer.valueOf(200).equals(vendorRes.getCode())
                && AadhaarConstants.STATUS_VALID.equalsIgnoreCase(vendorRes.getData().getStatus());

        if (!isValid) {
            String failureMessage = StringUtils.isNotBlank(vendorRes.getData().getMessage())
                    ? vendorRes.getData().getMessage()
                    : AadhaarConstants.MSG_INVALID_OR_EXPIRED_OTP;
            log.warn("Aadhaar OTP verification failed at gateway for referenceId: {}. Reason: {}", referenceId, failureMessage);

            ResponseInfo failureResponseInfo = ResponseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), false);
            AadhaarVerificationResponse errorRes = AadhaarVerificationResponse.builder()
                    .responseInfo(failureResponseInfo)
                    .txnId(vendorRes.getTransactionId() != null ? vendorRes.getTransactionId() : referenceId)
                    .referenceId(referenceId)
                    .status(AadhaarConstants.STATUS_INVALID_OTP)
                    .isVerified(false)
                    .message(failureMessage)
                    .build();

            return new ResponseEntity<>(errorRes, HttpStatus.BAD_REQUEST);
        }

        // 5. Map Verified e-KYC Demographic & Address Details to UPYOG Model
        VendorVerifyOtpSuccessResponse.VendorVerifyOtpSuccessResponseDetails details = vendorRes.getData();
        VendorVerifyOtpSuccessResponse.VendorVerifyOtpSuccessResponseDetails.AddressDetails addr = details.getAddress();

        UserDemographics demographics = UserDemographics.builder()
                .name(details.getName())
                .dob(StringUtils.isNotBlank(details.getDob()) ? details.getDob() : details.getDateOfBirth())
                .gender(details.getGender())
                .careOf(details.getCareOf())
                .fullAddress(details.getFullAddress())
                .photo(details.getPhoto())
                .doorNo(addr != null ? addr.getHouse() : null)
                .street(addr != null ? addr.getStreet() : null)
                .landmark(addr != null ? addr.getLandmark() : null)
                .locality(addr != null ? addr.getSubdistrict() : null)
                .city(addr != null ? addr.getVtc() : null)
                .district(addr != null ? addr.getDistrict() : null)
                .state(addr != null ? addr.getState() : null)
                .pincode(addr != null ? addr.getPincode() : null)
                .country(addr != null ? addr.getCountry() : null)
                .build();

        ResponseInfo successResponseInfo = ResponseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true);

        AadhaarVerificationResponse response = AadhaarVerificationResponse.builder()
                .responseInfo(successResponseInfo)
                .txnId(vendorRes.getTransactionId() != null ? vendorRes.getTransactionId() : referenceId)
                .referenceId(referenceId)
                .status(AadhaarConstants.STATUS_VERIFIED)
                .isVerified(true)
                .maskedAadhaar(details.getAadhaarNumber())
                .demographics(demographics)
                .message(AadhaarConstants.MSG_VERIFY_SUCCESS)
                .build();

        log.info("Aadhaar OTP verified successfully for referenceId: {}, maskedAadhaar: {}",
                referenceId, details.getAadhaarNumber());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Generates a standard error response wrapping eGov ResponseInfo and business error message.
     */
    private AadhaarVerificationResponse createErrorResponse(AadhaarVerificationRequest request, String status, String message) {
        ResponseInfo responseInfo = ResponseInfoFactory.createResponseInfoFromRequestInfo(
                request != null ? request.getRequestInfo() : null, false);

        return AadhaarVerificationResponse.builder()
                .responseInfo(responseInfo)
                .status(status)
                .message(message)
                .isVerified(false)
                .build();
    }

    /**
     * Safely masks an Aadhaar number, revealing only the last 4 digits (e.g. "XXXXXXXX1234").
     *
     * @param aadhaarNumber the raw 12-digit Aadhaar string
     * @return masked Aadhaar string or "XXXX" if input is null or too short
     */
    private String maskAadhaar(String aadhaarNumber) {
        if (StringUtils.isBlank(aadhaarNumber) || aadhaarNumber.length() < 4) {
            return AadhaarConstants.DEFAULT_MASKED_AADHAAR;
        }
        return aadhaarNumber.replaceAll(AadhaarConstants.REGEX_AADHAAR_MASK, AadhaarConstants.MASK_CHAR);
    }
}