package org.egov.dx.adapter.vendor.impl;

import lombok.extern.slf4j.Slf4j;
import org.egov.dx.adapter.vendor.AadhaarGatewayAdapter;
import org.egov.dx.adapter.vendor.dto.VendorSendOtpRequest;
import org.egov.dx.adapter.vendor.dto.VendorSendOtpResponse;
import org.egov.dx.adapter.vendor.dto.VendorVerifyOtpRequest;
import org.egov.dx.adapter.vendor.dto.VendorVerifyOtpSuccessResponse;
import org.egov.dx.util.AadhaarConstants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mock / simulated implementation of {@link AadhaarGatewayAdapter} for local development,
 * testing, and staging environments where live vendor credentials are unavailable.
 *
 * <p>Enforces the <b>Single Responsibility Principle (SRP)</b>: This class is solely responsible for
 * generating realistic simulated responses.</p>
 * <p>Enforces the <b>Liskov Substitution Principle (LSP)</b>: Fully honors the {@link AadhaarGatewayAdapter} contract.</p>
 *
 * <p>Automatically activated by Spring when {@code aadhaar.gateway.mock.enabled=true} (or if property is omitted).</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(name = "aadhaar.gateway.mock.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class MockAadhaarGatewayAdapter implements AadhaarGatewayAdapter {

    @Override
    public VendorSendOtpResponse sendOtp(VendorSendOtpRequest request) {
        String maskedAadhaar = (request != null) ? request.getMaskedAadhaar() : "null";
        log.info("[MOCK] Simulating Aadhaar OTP dispatch for masked Aadhaar: {}", maskedAadhaar);

        long timestamp = System.currentTimeMillis();
        String txnId = "TXN-MOCK-" + timestamp;
        long referenceId = 100000L + (timestamp % 900000L);

        return VendorSendOtpResponse.builder()
                .code(200)
                .timestamp(timestamp)
                .transactionId(txnId)
                .data(VendorSendOtpResponse.VendorSendOtpResponseDetails.builder()
                        .referenceId(referenceId)
                        .entity(AadhaarConstants.ENTITY_SEND_OTP_RESPONSE)
                        .message(AadhaarConstants.MSG_OTP_SENT_SUCCESS)
                        .build())
                .build();
    }

    @Override
    public VendorVerifyOtpSuccessResponse verifyOtp(VendorVerifyOtpRequest request) {
        Long referenceId = (request != null) ? request.getReferenceId() : null;
        log.info("[MOCK] Simulating Aadhaar OTP verification for referenceId: {}", referenceId);

        long timestamp = System.currentTimeMillis();
        String txnId = "TXN-MOCK-" + timestamp;
        String refIdStr = (referenceId != null) ? String.valueOf(referenceId) : String.valueOf(timestamp);

        return VendorVerifyOtpSuccessResponse.builder()
                .code(200)
                .timestamp(timestamp)
                .transactionId(txnId)
                .data(VendorVerifyOtpSuccessResponse.VendorVerifyOtpSuccessResponseDetails.builder()
                        .entity(AadhaarConstants.ENTITY_VERIFY_OTP_RESPONSE)
                        .referenceId(refIdStr)
                        .status(AadhaarConstants.STATUS_VALID)
                        .message(AadhaarConstants.MSG_VERIFY_SUCCESS)
                        .name("Citizen Name")
                        .dob("1995-05-15")
                        .gender("M")
                        .careOf("S/O Parent Name")
                        .aadhaarNumber("XXXXXXXX1234")
                        .fullAddress("123, Model Town, City, State, 143001")
                        .address(VendorVerifyOtpSuccessResponse.VendorVerifyOtpSuccessResponseDetails.AddressDetails.builder()
                                .house("123")
                                .street("Model Town Road")
                                .landmark("Near Central Park")
                                .subdistrict("Tehsil 1")
                                .vtc("Amritsar")
                                .district("Amritsar")
                                .state("Punjab")
                                .pincode("143001")
                                .country("India")
                                .build())
                        .build())
                .build();
    }
}
