package org.egov.dx.adapter.vendor.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egov.dx.adapter.vendor.AadhaarGatewayAdapter;
import org.egov.dx.adapter.vendor.dto.VendorSendOtpRequest;
import org.egov.dx.adapter.vendor.dto.VendorSendOtpResponse;
import org.egov.dx.adapter.vendor.dto.VendorVerifyOtpRequest;
import org.egov.dx.adapter.vendor.dto.VendorVerifyOtpSuccessResponse;
import org.egov.dx.util.AadhaarConstants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mock / simulated implementation of {@link AadhaarGatewayAdapter} for local development,
 * testing, and staging environments where live vendor credentials are unavailable.
 *
 * <p>Enforces the <b>Single Responsibility Principle (SRP)</b>: This class is solely responsible for
 * generating realistic simulated responses.</p>
 * <p>Enforces the <b>Liskov Substitution Principle (LSP)</b>: Fully honors the {@link AadhaarGatewayAdapter} contract.</p>
 *
 * <p>Loads 500+ realistic citizen records from {@code mock/aadhaar-mock-citizens.json}
 * and serves them sequentially (round-robin) so that each successive test or OTP invocation
 * receives a distinct new citizen persona until all records are consumed.</p>
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

    /**
     * Simulated citizen persona representing verified e-KYC response attributes.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MockCitizenPersona {
        /** Unique sequential record index */
        private Integer id;
        /** Full citizen name */
        private String name;
        /** Gender classification ("M" or "F") */
        private String gender;
        /** Date of birth formatted as YYYY-MM-DD */
        private String dob;
        /** Care-of relation string (e.g. "S/O Rajesh Sharma") */
        private String careOf;
        /** Father or guardian name */
        private String fatherName;
        /** Simulated contact mobile number */
        private String mobileNumber;
        /** House or flat number */
        private String house;
        /** Street or road address line */
        private String street;
        /** Locality landmark */
        private String landmark;
        /** City locality or sector */
        private String locality;
        /** Subdistrict or tehsil */
        private String subdistrict;
        /** Village/Town/City identifier */
        private String vtc;
        /** District name */
        private String district;
        /** State name */
        private String state;
        /** 6-digit postal PIN code */
        private String pincode;
        /** Full single-line address */
        private String fullAddress;
    }

    private static final String MOCK_RECORDS_PATH = "mock/aadhaar-mock-citizens.json";
    private static final String TXN_PREFIX_MOCK = "TXN-MOCK-";
    private static final String DEFAULT_MASKED_AADHAAR = "XXXXXXXX1234";
    private static final String DEFAULT_NAME = "Aarav Sharma";
    private static final String DEFAULT_GENDER = "M";
    private static final String DEFAULT_DOB = "1995-05-15";
    private static final String DEFAULT_CARE_OF = "S/O Rajesh Sharma";
    private static final String DEFAULT_FULL_ADDRESS = "123, Model Town, City, State, 143001";
    private static final String DEFAULT_HOUSE = "123";
    private static final String DEFAULT_STREET = "Model Town Road";
    private static final String DEFAULT_LANDMARK = "Near Central Park";
    private static final String DEFAULT_SUBDISTRICT = "Tehsil 1";
    private static final String DEFAULT_VTC = "Amritsar";
    private static final String DEFAULT_DISTRICT = "Amritsar";
    private static final String DEFAULT_STATE = "Punjab";
    private static final String DEFAULT_PINCODE = "143001";
    private static final String DEFAULT_COUNTRY = "India";

    private final List<MockCitizenPersona> mockPersonas = new ArrayList<>();
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    /**
     * Eagerly loads mock persona records from classpath JSON resource upon bean initialization.
     */
    @PostConstruct
    public void init() {
        loadPersonas();
    }

    /**
     * Thread-safely reads the 500+ mock records from classpath JSON into memory.
     */
    private synchronized void loadPersonas() {
        if (!mockPersonas.isEmpty()) {
            return;
        }
        try {
            ClassPathResource resource = new ClassPathResource(MOCK_RECORDS_PATH);
            if (resource.exists()) {
                ObjectMapper mapper = new ObjectMapper();
                try (InputStream is = resource.getInputStream()) {
                    List<MockCitizenPersona> loaded = mapper.readValue(is, new TypeReference<List<MockCitizenPersona>>() {});
                    if (loaded != null && !loaded.isEmpty()) {
                        mockPersonas.addAll(loaded);
                        log.info("[MOCK-AADHAAR-ADAPTER] Successfully loaded {} mock Aadhaar citizen records from {}",
                                mockPersonas.size(), MOCK_RECORDS_PATH);
                    }
                }
            } else {
                log.warn("[MOCK-AADHAAR-ADAPTER] Mock Aadhaar resource not found at: {}", MOCK_RECORDS_PATH);
            }
        } catch (Exception e) {
            log.error("[MOCK-AADHAAR-ADAPTER] Failed to load mock Aadhaar citizens: {}", e.getMessage(), e);
        }
    }

    /**
     * Dispenses the next mock citizen persona in round-robin sequential order.
     * Guaranteed to cycle through all 500+ records before repeating.
     *
     * @return next simulated {@link MockCitizenPersona}
     */
    public MockCitizenPersona getNextCitizenPersona() {
        if (mockPersonas.isEmpty()) {
            loadPersonas();
        }

        if (mockPersonas.isEmpty()) {
            return MockCitizenPersona.builder()
                    .name(DEFAULT_NAME)
                    .gender(DEFAULT_GENDER)
                    .dob(DEFAULT_DOB)
                    .careOf(DEFAULT_CARE_OF)
                    .fullAddress(DEFAULT_FULL_ADDRESS)
                    .house(DEFAULT_HOUSE)
                    .street(DEFAULT_STREET)
                    .landmark(DEFAULT_LANDMARK)
                    .subdistrict(DEFAULT_SUBDISTRICT)
                    .vtc(DEFAULT_VTC)
                    .district(DEFAULT_DISTRICT)
                    .state(DEFAULT_STATE)
                    .pincode(DEFAULT_PINCODE)
                    .build();
        }

        int index = Math.floorMod(currentIndex.getAndIncrement(), mockPersonas.size());
        MockCitizenPersona persona = mockPersonas.get(index);
        log.info("[MOCK-AADHAAR-ADAPTER] Dispensing mock citizen persona [{}/{}]: {} ({})",
                index + 1, mockPersonas.size(), persona.getName(), persona.getGender());
        return persona;
    }

    /**
     * Simulates Aadhaar OTP generation and dispatch for testing environments.
     *
     * @param request vendor send OTP request enclosing masked Aadhaar identifier
     * @return mock vendor send OTP response with generated reference ID and transaction ID
     */
    @Override
    public VendorSendOtpResponse sendOtp(VendorSendOtpRequest request) {
        String maskedAadhaar = (request != null) ? request.getMaskedAadhaar() : "null";
        log.info("[MOCK] Simulating Aadhaar OTP dispatch for masked Aadhaar: {}", maskedAadhaar);

        long timestamp = System.currentTimeMillis();
        String txnId = TXN_PREFIX_MOCK + timestamp;
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

    /**
     * Simulates UIDAI Aadhaar OTP verification and returns mock e-KYC citizen persona demographics.
     *
     * @param request vendor verify OTP request containing reference ID and OTP
     * @return mock vendor verification response containing complete citizen demographics
     */
    @Override
    public VendorVerifyOtpSuccessResponse verifyOtp(VendorVerifyOtpRequest request) {
        Long referenceId = (request != null) ? request.getReferenceId() : null;
        log.info("[MOCK] Simulating Aadhaar OTP verification for referenceId: {}", referenceId);

        long timestamp = System.currentTimeMillis();
        String txnId = TXN_PREFIX_MOCK + timestamp;
        String refIdStr = (referenceId != null) ? String.valueOf(referenceId) : String.valueOf(timestamp);
        MockCitizenPersona persona = getNextCitizenPersona();

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
                        .dob(persona.getDob() != null ? persona.getDob() : DEFAULT_DOB)
                        .gender(persona.getGender())
                        .careOf(persona.getCareOf())
                        .aadhaarNumber(DEFAULT_MASKED_AADHAAR)
                        .fullAddress(persona.getFullAddress() != null ? persona.getFullAddress() : DEFAULT_FULL_ADDRESS)
                        .address(VendorVerifyOtpSuccessResponse.VendorVerifyOtpSuccessResponseDetails.AddressDetails.builder()
                                .house(persona.getHouse() != null ? persona.getHouse() : DEFAULT_HOUSE)
                                .street(persona.getStreet() != null ? persona.getStreet() : DEFAULT_STREET)
                                .landmark(persona.getLandmark() != null ? persona.getLandmark() : DEFAULT_LANDMARK)
                                .subdistrict(persona.getSubdistrict() != null ? persona.getSubdistrict() : DEFAULT_SUBDISTRICT)
                                .vtc(persona.getVtc() != null ? persona.getVtc() : DEFAULT_VTC)
                                .district(persona.getDistrict() != null ? persona.getDistrict() : DEFAULT_DISTRICT)
                                .state(persona.getState() != null ? persona.getState() : DEFAULT_STATE)
                                .pincode(persona.getPincode() != null ? persona.getPincode() : DEFAULT_PINCODE)
                                .country(DEFAULT_COUNTRY)
                                .build())
                        .build())
                .build();
    }
}
