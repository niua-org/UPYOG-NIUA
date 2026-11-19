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

    @Test
    void testSearchGstin_ActiveSuccess() {
        GstProfileRequest request = GstProfileRequest.builder()
                .requestInfo(new RequestInfo())
                .gstin("03PTLAM0012A1Z5")
                .tenantId("pg.citya")
                .build();

        VendorGstProfileResponse.TaxpayerData vData = VendorGstProfileResponse.TaxpayerData.builder()
                .gstin("03PTLAM0012A1Z5")
                .legalName("MUNICIPAL CORPORATION AMRITSAR")
                .tradeName("MUNICIPAL CORPORATION AMRITSAR")
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
        assertEquals("03PTLA****1Z5", response.getMaskedGstin());
        assertNotNull(response.getTaxpayerDetails());
        assertEquals("03PTLAM0012A1Z5", response.getTaxpayerDetails().getGstin());
        assertEquals("PTLAM0012A", response.getTaxpayerDetails().getPan());
        assertEquals("03", response.getTaxpayerDetails().getStateCode());
        assertEquals("Punjab", response.getTaxpayerDetails().getState());
        assertEquals("MUNICIPAL CORPORATION AMRITSAR", response.getTaxpayerDetails().getLegalName());
        assertEquals("Active", response.getTaxpayerDetails().getStatus());

        verify(validator, times(1)).validateGstRequest(request);
    }

    @Test
    void testSearchGstin_CancelledTaxpayer() {
        GstProfileRequest request = GstProfileRequest.builder()
                .requestInfo(new RequestInfo())
                .gstin("03CANCEL0001Z9")
                .tenantId("pg.citya")
                .build();

        VendorGstProfileResponse.TaxpayerData vData = VendorGstProfileResponse.TaxpayerData.builder()
                .gstin("03CANCEL0001Z9")
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
