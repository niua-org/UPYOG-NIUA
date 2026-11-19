package org.egov.dx.adapter;

import org.egov.dx.adapter.vendor.dto.VendorTanVerificationRequest;
import org.egov.dx.adapter.vendor.dto.VendorTanVerificationResponse;
import org.egov.dx.adapter.vendor.tan.impl.MockTanGatewayAdapter;
import org.egov.dx.util.Configurations;
import org.egov.dx.util.TanConstants;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MockTanGatewayAdapterTest {

    private Configurations configurations;
    private MockTanGatewayAdapter adapter;

    @BeforeEach
    void setUp() {
        configurations = mock(Configurations.class);
        when(configurations.getTanVerificationMockRecordsPath()).thenReturn("mock/tan-mock-records.json");
        adapter = new MockTanGatewayAdapter(configurations);
        adapter.init();
    }

    @Test
    void testInit_MissingConfiguredMockPath_ThrowsException() {
        Configurations badConfig = mock(Configurations.class);
        when(badConfig.getTanVerificationMockRecordsPath()).thenReturn(null);
        MockTanGatewayAdapter unconfiguredAdapter = new MockTanGatewayAdapter(badConfig);

        CustomException ce = assertThrows(CustomException.class, unconfiguredAdapter::init);
        assertEquals(TanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
    }

    @Test
    void testVerifyTan_ExistingStaticMunicipalRecord() {
        VendorTanVerificationRequest request = VendorTanVerificationRequest.builder()
                .header(VendorTanVerificationRequest.RequestHeader.builder()
                        .requestId("REQ-001")
                        .serviceId("10002")
                        .build())
                .searchCriteria(VendorTanVerificationRequest.SearchCriteria.builder()
                        .tan("PTLA00123A")
                        .build())
                .build();

        VendorTanVerificationResponse response = adapter.verifyTan(request);
        assertNotNull(response);
        assertNotNull(response.getHeader());
        assertEquals("200", response.getHeader().getStatusCode());
        assertEquals(TanConstants.STATUS_SUCCESS, response.getHeader().getStatus());
        assertNotNull(response.getTanDetails());
        assertEquals("PTLA00123A", response.getTanDetails().getTan());
        assertEquals("MUNICIPAL CORPORATION AMRITSAR", response.getTanDetails().getNameOfDeductor());
        assertEquals("Local Authority", response.getTanDetails().getCategoryOfDeductor());
        assertEquals(TanConstants.STATUS_ACTIVE, response.getTanDetails().getStatus());
        assertNotNull(response.getTanDetails().getAddress());
        assertEquals("Amritsar", response.getTanDetails().getAddress().getCity());
    }

    @Test
    void testVerifyTan_ExistingStaticInactiveRecord() {
        VendorTanVerificationRequest request = VendorTanVerificationRequest.builder()
                .searchCriteria(VendorTanVerificationRequest.SearchCriteria.builder()
                        .tan("CHDA11223D")
                        .build())
                .build();

        VendorTanVerificationResponse response = adapter.verifyTan(request);
        assertNotNull(response);
        assertEquals("200", response.getHeader().getStatusCode());
        assertNotNull(response.getTanDetails());
        assertEquals(TanConstants.STATUS_INACTIVE, response.getTanDetails().getStatus());
    }

    @Test
    void testVerifyTan_SimulatedNotFound() {
        VendorTanVerificationRequest request = VendorTanVerificationRequest.builder()
                .searchCriteria(VendorTanVerificationRequest.SearchCriteria.builder()
                        .tan("NOTFOUND01")
                        .build())
                .build();

        VendorTanVerificationResponse response = adapter.verifyTan(request);
        assertNotNull(response);
        assertEquals("404", response.getHeader().getStatusCode());
        assertEquals(TanConstants.STATUS_FAILURE, response.getHeader().getStatus());
        assertNotNull(response.getErrorDetails());
        assertEquals(TanConstants.ERR_CODE_RECORD_NOT_FOUND, response.getErrorDetails().getErrorCode());
    }

    @Test
    void testVerifyTan_DeterministicFallback() {
        // Arbitrary valid TAN not in static JSON: DELC99887A
        VendorTanVerificationRequest request = VendorTanVerificationRequest.builder()
                .searchCriteria(VendorTanVerificationRequest.SearchCriteria.builder()
                        .tan("DELC99887A")
                        .build())
                .build();

        VendorTanVerificationResponse response = adapter.verifyTan(request);
        assertNotNull(response);
        assertEquals("200", response.getHeader().getStatusCode());
        assertEquals(TanConstants.STATUS_SUCCESS, response.getHeader().getStatus());
        assertNotNull(response.getTanDetails());
        assertEquals("DELC99887A", response.getTanDetails().getTan());
        assertEquals("Company", response.getTanDetails().getCategoryOfDeductor());
        assertEquals(TanConstants.STATUS_ACTIVE, response.getTanDetails().getStatus());
    }

    @Test
    void testVerifyTan_InvalidFormat() {
        VendorTanVerificationRequest request = VendorTanVerificationRequest.builder()
                .searchCriteria(VendorTanVerificationRequest.SearchCriteria.builder()
                        .tan("INVALID_12")
                        .build())
                .build();

        VendorTanVerificationResponse response = adapter.verifyTan(request);
        assertNotNull(response);
        assertEquals("400", response.getHeader().getStatusCode());
        assertEquals(TanConstants.STATUS_FAILURE, response.getHeader().getStatus());
        assertEquals(TanConstants.ERR_CODE_INVALID_FORMAT, response.getErrorDetails().getErrorCode());
    }
}
