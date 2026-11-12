package org.egov.dx.adapter.vendor.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.egov.dx.adapter.vendor.AadhaarGatewayAdapter;
import org.egov.dx.adapter.vendor.dto.VendorSendOtpRequest;
import org.egov.dx.adapter.vendor.dto.VendorSendOtpResponse;
import org.egov.dx.adapter.vendor.dto.VendorVerifyOtpRequest;
import org.egov.dx.adapter.vendor.dto.VendorVerifyOtpSuccessResponse;
import org.egov.dx.util.AadhaarConstants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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

    @Getter
    @AllArgsConstructor
    private static class MockCitizenPersona {
        private final String name;
        private final String gender;
        private final String careOf;
    }

    private static final List<MockCitizenPersona> MOCK_CITIZEN_PERSONAS = Arrays.asList(
            new MockCitizenPersona("Aarav Sharma", "M", "S/O Rajesh Sharma"),
            new MockCitizenPersona("Priya Patel", "F", "D/O Suresh Patel"),
            new MockCitizenPersona("Rohan Verma", "M", "S/O Ramesh Verma"),
            new MockCitizenPersona("Ananya Gupta", "F", "D/O Manoj Gupta"),
            new MockCitizenPersona("Vikram Singh", "M", "S/O Harpreet Singh"),
            new MockCitizenPersona("Sneha Reddy", "F", "D/O Venkat Reddy"),
            new MockCitizenPersona("Rahul Kumar", "M", "S/O Ashok Kumar"),
            new MockCitizenPersona("Pooja Joshi", "F", "D/O Dinesh Joshi"),
            new MockCitizenPersona("Amitabh Das", "M", "S/O Subhash Das"),
            new MockCitizenPersona("Neha Malhotra", "F", "D/O Sunil Malhotra"),
            new MockCitizenPersona("Rajesh Iyer", "M", "S/O Narayanan Iyer"),
            new MockCitizenPersona("Sunita Meena", "F", "W/O Ram Meena"),
            new MockCitizenPersona("Suresh Nair", "M", "S/O Madhavan Nair"),
            new MockCitizenPersona("Kavita Rao", "F", "D/O Krishna Rao"),
            new MockCitizenPersona("Manish Choudhary", "M", "S/O Jagdish Choudhary"),
            new MockCitizenPersona("Deepika Sen", "F", "D/O Pradip Sen"),
            new MockCitizenPersona("Aditya Mishra", "M", "S/O Alok Mishra"),
            new MockCitizenPersona("Divya Bhat", "F", "D/O Ganesh Bhat"),
            new MockCitizenPersona("Arjun Kapoor", "M", "S/O Anil Kapoor"),
            new MockCitizenPersona("Ritu Mukherjee", "F", "D/O Sourav Mukherjee")
    );

    private MockCitizenPersona getRandomCitizenPersona() {
        int index = ThreadLocalRandom.current().nextInt(MOCK_CITIZEN_PERSONAS.size());
        return MOCK_CITIZEN_PERSONAS.get(index);
    }

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
        MockCitizenPersona persona = getRandomCitizenPersona();

        return VendorVerifyOtpSuccessResponse.builder()
                .code(200)
                .timestamp(timestamp)
                .transactionId(txnId)
                .data(VendorVerifyOtpSuccessResponse.VendorVerifyOtpSuccessResponseDetails.builder()
                        .entity(AadhaarConstants.ENTITY_VERIFY_OTP_RESPONSE)
                        .referenceId(refIdStr)
                        .status(AadhaarConstants.STATUS_VALID)
                        .message(AadhaarConstants.MSG_VERIFY_SUCCESS)
                        .name(persona.getName())
                        .dob("1995-05-15")
                        .gender(persona.getGender())
                        .careOf(persona.getCareOf())
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
