package org.egov.dx.adapter;

import org.egov.dx.adapter.vendor.dto.VendorGstProfileResponse;
import org.egov.dx.adapter.vendor.gst.impl.MockGstGatewayAdapter;
import org.egov.dx.util.Configurations;
import org.egov.dx.util.GstConstants;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MockGstGatewayAdapterTest {

    private Configurations configurations;
    private MockGstGatewayAdapter adapter;

    @BeforeEach
    void setUp() {
        configurations = mock(Configurations.class);
        when(configurations.getGstVerificationMockRecordsPath()).thenReturn("mock/gst-mock-records.json");
        adapter = new MockGstGatewayAdapter(configurations);
        adapter.init();
    }

    /**
     * Tests Init under the missing configured mock path_throws exception scenario.
     *
     * Verifies that {@code CustomException} is thrown when missing configured mock path_throws exception.
     */
    @Test
    void testInit_MissingConfiguredMockPath_ThrowsException() {
        Configurations badConfig = mock(Configurations.class);
        when(badConfig.getGstVerificationMockRecordsPath()).thenReturn(null);
        MockGstGatewayAdapter unconfiguredAdapter = new MockGstGatewayAdapter(badConfig);

        CustomException ce = assertThrows(CustomException.class, unconfiguredAdapter::init);
        assertEquals(GstConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
    }

    /**
     * Tests Search Gstin under the existing municipal record scenario.
     *
     * Verifies the expected response and model assertions.
     */
    @Test
    void testSearchGstin_ExistingMunicipalRecord() {
        VendorGstProfileResponse response = adapter.searchGstin("03PTLAM0012A1Z3");
        assertNotNull(response);
        assertEquals("1", response.getStatusCode());
        assertEquals(GstConstants.STATUS_SUCCESS, response.getStatusDesc());
        assertNotNull(response.getData());
        assertEquals("03PTLAM0012A1Z3", response.getData().getGstin());
        assertEquals("MOCK MUNICIPAL CORPORATION AMRITSAR", response.getData().getLegalName());
        assertEquals("Local Authority", response.getData().getConstitutionOfBusiness());
        assertEquals(GstConstants.STATUS_ACTIVE, response.getData().getStatus());
        assertNotNull(response.getData().getPrincipalPlaceOfBusiness());
        assertNotNull(response.getData().getPrincipalPlaceOfBusiness().getAddress());
        assertEquals("Amritsar", response.getData().getPrincipalPlaceOfBusiness().getAddress().getCity());
    }

    /**
     * Tests Search Gstin under the existing cancelled record scenario.
     *
     * Verifies the expected response and model assertions.
     */
    @Test
    void testSearchGstin_ExistingCancelledRecord() {
        VendorGstProfileResponse response = adapter.searchGstin("03CANCE0001L1Z1");
        assertNotNull(response);
        assertEquals("1", response.getStatusCode());
        assertNotNull(response.getData());
        assertEquals(GstConstants.STATUS_CANCELLED, response.getData().getStatus());
        assertEquals("31/03/2022", response.getData().getCancellationDate());
    }

    /**
     * Tests Search Gstin under the simulated not found scenario.
     *
     * Verifies the expected response and model assertions.
     */
    @Test
    void testSearchGstin_SimulatedNotFound() {
        VendorGstProfileResponse response = adapter.searchGstin("03AAAAA0000A1Z5");
        assertNotNull(response);
        assertEquals("0", response.getStatusCode());
        assertEquals(GstConstants.STATUS_FAILURE, response.getStatusDesc());
        assertNotNull(response.getError());
        assertEquals(GstConstants.ERR_CODE_RECORD_NOT_FOUND, response.getError().getErrorCode());
    }

    /**
     * Tests Search Gstin under the unlisted gstin returns not found by default scenario.
     *
     * Verifies the expected response and model assertions.
     */
    @Test
    void testSearchGstin_UnlistedGstinReturnsNotFoundByDefault() {
        // Arbitrary valid GSTIN not explicitly in mock records: 27AABCT1234A1Z5 (Maharashtra)
        VendorGstProfileResponse response = adapter.searchGstin("27AABCT1234A1Z5");
        assertNotNull(response);
        assertEquals("0", response.getStatusCode());
        assertEquals(GstConstants.STATUS_FAILURE, response.getStatusDesc());
        assertNotNull(response.getError());
        assertEquals(GstConstants.ERR_CODE_RECORD_NOT_FOUND, response.getError().getErrorCode());
    }

    /**
     * Tests Search Gstin under the deterministic fallback_when synthesize unknown enabled scenario.
     *
     * Verifies the expected response and model assertions.
     */
    @Test
    void testSearchGstin_DeterministicFallback_WhenSynthesizeUnknownEnabled() {
        when(configurations.isGstVerificationMockSynthesizeUnknownEnabled()).thenReturn(true);

        VendorGstProfileResponse response = adapter.searchGstin("27AABCT1234A1Z5");
        assertNotNull(response);
        assertEquals("1", response.getStatusCode());
        assertEquals(GstConstants.STATUS_SUCCESS, response.getStatusDesc());
        assertNotNull(response.getData());
        assertEquals("27AABCT1234A1Z5", response.getData().getGstin());
        assertEquals(GstConstants.STATUS_ACTIVE, response.getData().getStatus());
        assertNotNull(response.getData().getPrincipalPlaceOfBusiness());
        assertEquals("Maharashtra", response.getData().getPrincipalPlaceOfBusiness().getAddress().getState());
    }

    /**
     * Tests Search Gstin under the invalid format scenario.
     *
     * Verifies the expected response and model assertions.
     */
    @Test
    void testSearchGstin_InvalidFormat() {
        VendorGstProfileResponse response = adapter.searchGstin("INVALID_123");
        assertNotNull(response);
        assertEquals("0", response.getStatusCode());
        assertEquals(GstConstants.STATUS_FAILURE, response.getStatusDesc());
        assertNotNull(response.getError());
        assertEquals(GstConstants.ERR_CODE_INVALID_FORMAT, response.getError().getErrorCode());
    }
}
