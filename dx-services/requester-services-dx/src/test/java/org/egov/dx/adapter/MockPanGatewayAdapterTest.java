package org.egov.dx.adapter;

import org.egov.dx.adapter.vendor.dto.VendorPanVerificationRequest;
import org.egov.dx.adapter.vendor.dto.VendorPanVerificationResponse;
import org.egov.dx.adapter.vendor.pan.impl.MockPanGatewayAdapter;
import org.egov.dx.util.Configurations;
import org.egov.dx.util.PanConstants;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MockPanGatewayAdapterTest {

    private Configurations configurations;
    private MockPanGatewayAdapter adapter;

    @BeforeEach
    void setUp() {
        configurations = mock(Configurations.class);
        when(configurations.getPanVerificationMockRecordsPath()).thenReturn("mock/pan-mock-records.json");
        adapter = new MockPanGatewayAdapter(configurations);
        adapter.init();
    }

    @Test
    void testInit_MissingConfiguredMockPath_ThrowsException() {
        Configurations badConfig = mock(Configurations.class);
        when(badConfig.getPanVerificationMockRecordsPath()).thenReturn(null);
        MockPanGatewayAdapter unconfiguredAdapter = new MockPanGatewayAdapter(badConfig);

        CustomException ce = assertThrows(CustomException.class, unconfiguredAdapter::init);
        assertEquals(PanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
    }

    @Test
    void testVerifyPan_ExistingCitizenRecord_ExactMatch() {
        VendorPanVerificationRequest request = VendorPanVerificationRequest.builder()
                .header(VendorPanVerificationRequest.RequestHeader.builder()
                        .requestId("REQ-001")
                        .serviceId("10001")
                        .build())
                .searchParams(VendorPanVerificationRequest.SearchParams.builder()
                        .pan("AAAPS0001B")
                        .fullName("Aarav Sharma")
                        .dob("24/01/1972")
                        .build())
                .build();

        VendorPanVerificationResponse response = adapter.verifyPan(request);
        assertNotNull(response);
        assertNotNull(response.getHeader());
        assertEquals("200", response.getHeader().getStatusCode());
        assertEquals(PanConstants.STATUS_SUCCESS, response.getHeader().getStatus());
        assertNotNull(response.getResponseParams());
        assertEquals("AAAPS0001B", response.getResponseParams().getPan());
        assertEquals("AARAV SHARMA", response.getResponseParams().getFullName());
        assertEquals("Individual", response.getResponseParams().getCategory());
        assertEquals(PanConstants.STATUS_EXISTING_AND_VALID, response.getResponseParams().getPanStatus());
        assertEquals(PanConstants.MATCH_YES, response.getResponseParams().getNameMatch());
        assertEquals(PanConstants.MATCH_YES, response.getResponseParams().getDobMatch());
    }

    @Test
    void testVerifyPan_ExistingRecord_MismatchedName() {
        VendorPanVerificationRequest request = VendorPanVerificationRequest.builder()
                .searchParams(VendorPanVerificationRequest.SearchParams.builder()
                        .pan("AAAPS0001B")
                        .fullName("Different Name")
                        .dob("24/01/1972")
                        .build())
                .build();

        VendorPanVerificationResponse response = adapter.verifyPan(request);
        assertNotNull(response);
        assertEquals("200", response.getHeader().getStatusCode());
        assertNotNull(response.getResponseParams());
        assertEquals(PanConstants.MATCH_NO, response.getResponseParams().getNameMatch());
        assertEquals(PanConstants.MATCH_YES, response.getResponseParams().getDobMatch());
    }

    @Test
    void testVerifyPan_SimulatedNotFound() {
        VendorPanVerificationRequest request = VendorPanVerificationRequest.builder()
                .searchParams(VendorPanVerificationRequest.SearchParams.builder()
                        .pan("NOTFOUND01")
                        .fullName("Nobody")
                        .dob("01/01/2000")
                        .build())
                .build();

        VendorPanVerificationResponse response = adapter.verifyPan(request);
        assertNotNull(response);
        assertEquals("404", response.getHeader().getStatusCode());
        assertEquals(PanConstants.STATUS_FAILURE, response.getHeader().getStatus());
        assertNotNull(response.getErrorDetails());
        assertEquals(PanConstants.ERR_CODE_RECORD_NOT_FOUND, response.getErrorDetails().getErrorCode());
    }

    @Test
    void testVerifyPan_DeterministicFallback() {
        // Arbitrary valid PAN not in mock records: DELCA9988A (4th char 'C' -> Company)
        VendorPanVerificationRequest request = VendorPanVerificationRequest.builder()
                .searchParams(VendorPanVerificationRequest.SearchParams.builder()
                        .pan("DELCA9988A")
                        .fullName("Dynamic User")
                        .dob("1992-05-20")
                        .build())
                .build();

        VendorPanVerificationResponse response = adapter.verifyPan(request);
        assertNotNull(response);
        assertEquals("200", response.getHeader().getStatusCode());
        assertEquals(PanConstants.STATUS_SUCCESS, response.getHeader().getStatus());
        assertNotNull(response.getResponseParams());
        assertEquals("DELCA9988A", response.getResponseParams().getPan());
        assertEquals("Company", response.getResponseParams().getCategory());
        assertEquals(PanConstants.STATUS_EXISTING_AND_VALID, response.getResponseParams().getPanStatus());
        assertEquals(PanConstants.MATCH_YES, response.getResponseParams().getNameMatch());
        assertEquals(PanConstants.MATCH_YES, response.getResponseParams().getDobMatch());
    }

    @Test
    void testVerifyPan_InvalidFormat() {
        VendorPanVerificationRequest request = VendorPanVerificationRequest.builder()
                .searchParams(VendorPanVerificationRequest.SearchParams.builder()
                        .pan("INVALID_12")
                        .build())
                .build();

        VendorPanVerificationResponse response = adapter.verifyPan(request);
        assertNotNull(response);
        assertEquals("400", response.getHeader().getStatusCode());
        assertEquals(PanConstants.STATUS_FAILURE, response.getHeader().getStatus());
        assertEquals(PanConstants.ERR_CODE_INVALID_FORMAT, response.getErrorDetails().getErrorCode());
    }
}
