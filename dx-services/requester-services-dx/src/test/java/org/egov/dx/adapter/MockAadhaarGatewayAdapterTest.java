package org.egov.dx.adapter;

import org.egov.dx.adapter.vendor.dto.VendorSendOtpRequest;
import org.egov.dx.adapter.vendor.dto.VendorSendOtpResponse;
import org.egov.dx.adapter.vendor.dto.VendorVerifyOtpRequest;
import org.egov.dx.adapter.vendor.dto.VendorVerifyOtpSuccessResponse;
import org.egov.dx.adapter.vendor.impl.MockAadhaarGatewayAdapter;
import org.egov.dx.util.AadhaarConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class MockAadhaarGatewayAdapterTest {

    private MockAadhaarGatewayAdapter mockAdapter;

    @BeforeEach
    void setUp() {
        mockAdapter = new MockAadhaarGatewayAdapter();
    }

    @Test
    void testSendOtp_GeneratesValidSimulation() {
        VendorSendOtpRequest request = VendorSendOtpRequest.builder()
                .aadhaarNumber("548796521458")
                .consent("Y")
                .build();

        VendorSendOtpResponse response = mockAdapter.sendOtp(request);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertNotNull(response.getTransactionId());
        assertNotNull(response.getData());
        assertNotNull(response.getData().getReferenceId());
        assertEquals(AadhaarConstants.MSG_OTP_SENT_SUCCESS, response.getData().getMessage());
    }

    @Test
    void testVerifyOtp_GeneratesValidDemographics() {
        VendorVerifyOtpRequest request = VendorVerifyOtpRequest.builder()
                .referenceId(123456L)
                .otp("123456")
                .build();

        VendorVerifyOtpSuccessResponse response = mockAdapter.verifyOtp(request);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals("VALID", response.getData().getStatus());
        assertEquals("XXXXXXXX1234", response.getData().getAadhaarNumber());
        assertNotNull(response.getData().getName());
        assertFalse(response.getData().getName().isEmpty());
        assertNotNull(response.getData().getAddress());
        assertNotNull(response.getData().getAddress().getVtc());
    }

    @Test
    @DisplayName("Should dispense distinct new records sequentially until all 520 records are used")
    void testVerifyOtp_DispensesAllRecordsSequentiallyUntilAllUsed() {
        Set<String> dispensedNames = new HashSet<>();
        int totalExpected = 520;

        for (int i = 0; i < totalExpected; i++) {
            VendorVerifyOtpRequest request = VendorVerifyOtpRequest.builder()
                    .referenceId(100000L + i)
                    .otp("123456")
                    .build();
            VendorVerifyOtpSuccessResponse response = mockAdapter.verifyOtp(request);
            assertNotNull(response.getData().getName());
            dispensedNames.add(response.getData().getName());
        }

        // All 520 records in the first pass must be unique
        assertEquals(totalExpected, dispensedNames.size(), "Expected 520 unique names dispensed sequentially");

        // Next invocation wraps around to the beginning
        VendorVerifyOtpRequest wrapRequest = VendorVerifyOtpRequest.builder()
                .referenceId(999999L)
                .otp("123456")
                .build();
        VendorVerifyOtpSuccessResponse wrapResponse = mockAdapter.verifyOtp(wrapRequest);
        assertTrue(dispensedNames.contains(wrapResponse.getData().getName()), "Should wrap around after all used");
    }
}
