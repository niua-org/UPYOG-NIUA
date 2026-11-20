package org.egov.dx.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.dx.adapter.vendor.dto.VendorGstProfileResponse;
import org.egov.dx.adapter.vendor.gst.GstGatewayAdapter;
import org.egov.dx.service.impl.GstProfileServiceImpl;
import org.egov.dx.util.GstConstants;
import org.egov.dx.validator.GstVerificationValidator;
import org.egov.dx.web.models.GstProfileRequest;
import org.egov.dx.web.models.GstProfileResponse;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class GstProfileServiceTest {

    private GstGatewayAdapter gatewayAdapter;
    private GstVerificationValidator validator;
    private TenantVerificationConfigService tenantConfigService;
    private GstProfileService service;

    @BeforeEach
    void setUp() {
        gatewayAdapter = mock(GstGatewayAdapter.class);
        validator = mock(GstVerificationValidator.class);
        tenantConfigService = mock(TenantVerificationConfigService.class);
        when(tenantConfigService.isVerificationEnabled(any(), any(), any())).thenReturn(true);
        service = new GstProfileServiceImpl(gatewayAdapter, validator, tenantConfigService);
    }

    /**
     * Tests Search Gstin under the tenant disabled in mdms_throws custom exception scenario.
     *
     * Verifies that {@code CustomException} is thrown when tenant disabled in mdms_throws custom exception.
     */
    @Test
    void testSearchGstin_TenantDisabledInMdms_ThrowsCustomException() {
        GstProfileRequest request = GstProfileRequest.builder()
                .requestInfo(new RequestInfo())
                .gstin("03PTLAM0012A1Z5")
                .tenantId("pg.disabledcity")
                .build();

        when(tenantConfigService.isVerificationEnabled(any(), eq("pg.disabledcity"), eq("GST"))).thenReturn(false);

        CustomException ce = assertThrows(CustomException.class, () -> service.searchGstin(request));
        assertEquals(GstConstants.ERR_CODE_TENANT_DISABLED, ce.getCode());
        assertTrue(ce.getMessage().contains("GST profile search is not enabled for tenant"));
    }

    /**
     * Tests Search Gstin under the active success scenario.
     *
     * Verifies that the condition evaluates to true.
     */
    @Test
    void testSearchGstin_ActiveSuccess() {
        GstProfileRequest request = GstProfileRequest.builder()
                .requestInfo(new RequestInfo())
                .gstin("03PTLAM0012A1Z3")
                .tenantId("pg.citya")
                .build();

        VendorGstProfileResponse.TaxpayerData vData = VendorGstProfileResponse.TaxpayerData.builder()
                .gstin("03PTLAM0012A1Z3")
                .legalName("MOCK MUNICIPAL CORPORATION AMRITSAR")
                .tradeName("MOCK MUNICIPAL CORPORATION AMRITSAR")
                .constitutionOfBusiness("Local Authority")
                .taxpayerType("Tax Deductor")
                .status(GstConstants.STATUS_ACTIVE)
                .registrationDate("01/07/2017")
                .principalPlaceOfBusiness(VendorGstProfileResponse.PlaceOfBusiness.builder()
                        .address(VendorGstProfileResponse.Address.builder()
                                .doorNumber("Town Hall")
                                .buildingName("Heritage Street")
                                .city("Amritsar")
                                .state("Punjab")
                                .pincode("143001")
                                .build())
                        .build())
                .build();

        VendorGstProfileResponse vRes = VendorGstProfileResponse.builder()
                .statusCode("1")
                .statusDesc(GstConstants.STATUS_SUCCESS)
                .data(vData)
                .build();

        when(gatewayAdapter.searchGstin(anyString())).thenReturn(vRes);

        GstProfileResponse response = service.searchGstin(request);
        assertNotNull(response);
        assertTrue(response.getIsVerified());
        assertEquals("03PTLA****1Z3", response.getMaskedGstin());
        assertNotNull(response.getTaxpayerDetails());
        assertEquals("03PTLAM0012A1Z3", response.getTaxpayerDetails().getGstin());
        assertEquals("PTLAM0012A", response.getTaxpayerDetails().getPan());
        assertEquals("03", response.getTaxpayerDetails().getStateCode());
        assertEquals("Punjab", response.getTaxpayerDetails().getState());
        assertEquals("MOCK MUNICIPAL CORPORATION AMRITSAR", response.getTaxpayerDetails().getLegalName());
        assertEquals("Active", response.getTaxpayerDetails().getStatus());

        verify(validator, times(1)).validateGstRequest(request);
    }

    /**
     * Tests Search Gstin under the cancelled taxpayer scenario.
     *
     * Verifies that the check evaluates to false.
     */
    @Test
    void testSearchGstin_CancelledTaxpayer() {
        GstProfileRequest request = GstProfileRequest.builder()
                .requestInfo(new RequestInfo())
                .gstin("03CANCE0001L1Z1")
                .tenantId("pg.citya")
                .build();

        VendorGstProfileResponse.TaxpayerData vData = VendorGstProfileResponse.TaxpayerData.builder()
                .gstin("03CANCE0001L1Z1")
                .legalName("VINTAGE TRADERS")
                .status(GstConstants.STATUS_CANCELLED)
                .build();

        VendorGstProfileResponse vRes = VendorGstProfileResponse.builder()
                .statusCode("1")
                .statusDesc(GstConstants.STATUS_SUCCESS)
                .data(vData)
                .build();

        when(gatewayAdapter.searchGstin(anyString())).thenReturn(vRes);

        GstProfileResponse response = service.searchGstin(request);
        assertNotNull(response);
        assertFalse(response.getIsVerified());
        assertEquals("Cancelled", response.getTaxpayerDetails().getStatus());
    }

    /**
     * Tests Search Gstin under the gateway error response scenario.
     *
     * Verifies that {@code CustomException} is thrown when gateway error response.
     */
    @Test
    void testSearchGstin_GatewayErrorResponse() {
        GstProfileRequest request = GstProfileRequest.builder()
                .requestInfo(new RequestInfo())
                .gstin("03NOTFOUND001Z5")
                .tenantId("pg.citya")
                .build();

        VendorGstProfileResponse vRes = VendorGstProfileResponse.builder()
                .statusCode("0")
                .statusDesc(GstConstants.STATUS_FAILURE)
                .error(VendorGstProfileResponse.VendorError.builder()
                        .errorCode(GstConstants.ERR_CODE_RECORD_NOT_FOUND)
                        .message(GstConstants.MSG_RECORD_NOT_FOUND)
                        .build())
                .build();

        when(gatewayAdapter.searchGstin(anyString())).thenReturn(vRes);

        GstProfileResponse response = service.searchGstin(request);
        assertNotNull(response);
        assertFalse(response.getIsVerified());
        assertNotNull(response.getErrorDetails());
        assertEquals(GstConstants.ERR_CODE_RECORD_NOT_FOUND, response.getErrorDetails().getErrorCode());
    }

    /**
     * Tests Search Gstin under the null gateway response_throws custom exception scenario.
     *
     * Verifies that {@code CustomException} is thrown when null gateway response_throws custom exception.
     */
    @Test
    void testSearchGstin_NullGatewayResponse_ThrowsCustomException() {
        GstProfileRequest request = GstProfileRequest.builder()
                .requestInfo(new RequestInfo())
                .gstin("03PTLAM0012A1Z5")
                .tenantId("pg.citya")
                .build();

        when(gatewayAdapter.searchGstin(anyString())).thenReturn(null);

        CustomException ce = assertThrows(CustomException.class, () -> service.searchGstin(request));
        assertEquals(GstConstants.ERR_CODE_GATEWAY_ERROR, ce.getCode());
    }
}
