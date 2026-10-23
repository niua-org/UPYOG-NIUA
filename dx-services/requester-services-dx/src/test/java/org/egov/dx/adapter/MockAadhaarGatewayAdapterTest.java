package org.egov.dx.adapter;

import org.egov.dx.adapter.vendor.dto.VendorSendOtpRequest;
import org.egov.dx.adapter.vendor.dto.VendorSendOtpResponse;
import org.egov.dx.adapter.vendor.dto.VendorVerifyOtpRequest;
import org.egov.dx.adapter.vendor.dto.VendorVerifyOtpSuccessResponse;
import org.egov.dx.adapter.vendor.impl.MockAadhaarGatewayAdapter;
import org.egov.dx.util.AadhaarConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        assertNotNull(response.getData().getAddress());
        assertEquals("Amritsar", response.getData().getAddress().getVtc());
    }
}
