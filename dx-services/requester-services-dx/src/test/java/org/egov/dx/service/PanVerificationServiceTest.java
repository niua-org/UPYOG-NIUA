package org.egov.dx.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.dx.adapter.vendor.dto.VendorPanVerificationRequest;
import org.egov.dx.adapter.vendor.dto.VendorPanVerificationResponse;
import org.egov.dx.adapter.vendor.pan.PanGatewayAdapter;
import org.egov.dx.service.impl.PanVerificationServiceImpl;
import org.egov.dx.util.PanConstants;
import org.egov.dx.validator.PanVerificationValidator;
import org.egov.dx.web.models.PanVerificationRequest;
import org.egov.dx.web.models.PanVerificationResponse;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PanVerificationServiceTest {

    private PanGatewayAdapter gatewayAdapter;
    private PanVerificationValidator validator;
    private TenantVerificationConfigService tenantConfigService;
    private PanVerificationService service;

    @BeforeEach
    void setUp() {
        gatewayAdapter = mock(PanGatewayAdapter.class);
        validator = mock(PanVerificationValidator.class);
        tenantConfigService = mock(TenantVerificationConfigService.class);
        when(tenantConfigService.isVerificationEnabled(any(), any(), any())).thenReturn(true);
        service = new PanVerificationServiceImpl(gatewayAdapter, validator, tenantConfigService);
    }

    @Test
    void testVerifyPan_TenantDisabledInMdms_ThrowsCustomException() {
        PanVerificationRequest request = PanVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .pan("BLRPA0001A")
                .fullName("Rahul Sharma")
                .dob("15/08/1990")
                .tenantId("pg.disabledcity")
                .build();

        when(tenantConfigService.isVerificationEnabled(any(), eq("pg.disabledcity"), eq("PAN"))).thenReturn(false);

        CustomException ce = assertThrows(CustomException.class, () -> service.verifyPan(request));
        assertEquals(PanConstants.ERR_CODE_TENANT_DISABLED, ce.getCode());
        assertTrue(ce.getMessage().contains("PAN verification is not enabled for tenant"));
    }

    @Test
    void testVerifyPan_ActiveSuccess() {
        PanVerificationRequest request = PanVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .pan("BLRPA0001A")
                .fullName("Rahul Sharma")
                .dob("15/08/1990")
                .tenantId("pg.citya")
                .build();

        VendorPanVerificationResponse.ResponseParams vParams = VendorPanVerificationResponse.ResponseParams.builder()
                .pan("BLRPA0001A")
                .fullName("RAHUL SHARMA")
                .category("Individual")
                .panStatus(PanConstants.STATUS_EXISTING_AND_VALID)
                .nameMatch(PanConstants.MATCH_YES)
                .dobMatch(PanConstants.MATCH_YES)
                .seedingStatus("Y")
                .build();

        VendorPanVerificationResponse vRes = VendorPanVerificationResponse.builder()
                .header(VendorPanVerificationResponse.ResponseHeader.builder()
                        .status(PanConstants.STATUS_SUCCESS)
                        .statusCode("200")
                        .build())
                .responseParams(vParams)
                .build();

        when(gatewayAdapter.verifyPan(any(VendorPanVerificationRequest.class))).thenReturn(vRes);

        PanVerificationResponse response = service.verifyPan(request);
        assertNotNull(response);
        assertTrue(response.getIsVerified());
        assertEquals("BLRPA****A", response.getMaskedPan());
        assertNotNull(response.getPanDetails());
        assertEquals("BLRPA0001A", response.getPanDetails().getPan());
        assertEquals("Individual", response.getPanDetails().getCategory());
        assertEquals(PanConstants.STATUS_EXISTING_AND_VALID, response.getPanDetails().getPanStatus());
        assertTrue(response.getPanDetails().getNameMatch());
        assertTrue(response.getPanDetails().getDobMatch());

        verify(validator, times(1)).validatePanRequest(request);
    }

    @Test
    void testVerifyPan_MismatchedName_NotVerified() {
        PanVerificationRequest request = PanVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .pan("BLRPA0001A")
                .fullName("Different Name")
                .dob("15/08/1990")
                .tenantId("pg.citya")
                .build();

        VendorPanVerificationResponse.ResponseParams vParams = VendorPanVerificationResponse.ResponseParams.builder()
                .pan("BLRPA0001A")
                .fullName("RAHUL SHARMA")
                .category("Individual")
                .panStatus(PanConstants.STATUS_EXISTING_AND_VALID)
                .nameMatch(PanConstants.MATCH_NO)
                .dobMatch(PanConstants.MATCH_YES)
                .build();

        VendorPanVerificationResponse vRes = VendorPanVerificationResponse.builder()
                .header(VendorPanVerificationResponse.ResponseHeader.builder()
                        .status(PanConstants.STATUS_SUCCESS)
                        .statusCode("200")
                        .build())
                .responseParams(vParams)
                .build();

        when(gatewayAdapter.verifyPan(any(VendorPanVerificationRequest.class))).thenReturn(vRes);

        PanVerificationResponse response = service.verifyPan(request);
        assertNotNull(response);
        assertFalse(response.getIsVerified());
        assertFalse(response.getPanDetails().getNameMatch());
        assertTrue(response.getPanDetails().getDobMatch());
    }

    @Test
    void testVerifyPan_GatewayErrorDetails() {
        PanVerificationRequest request = PanVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .pan("BLRPA0001A")
                .fullName("Rahul Sharma")
                .dob("15/08/1990")
                .tenantId("pg.citya")
                .build();

        VendorPanVerificationResponse vRes = VendorPanVerificationResponse.builder()
                .header(VendorPanVerificationResponse.ResponseHeader.builder()
                        .status(PanConstants.STATUS_FAILURE)
                        .statusCode("404")
                        .build())
                .errorDetails(VendorPanVerificationResponse.VendorErrorDetails.builder()
                        .errorCode(PanConstants.ERR_CODE_RECORD_NOT_FOUND)
                        .errorMessage(PanConstants.MSG_RECORD_NOT_FOUND)
                        .build())
                .build();

        when(gatewayAdapter.verifyPan(any(VendorPanVerificationRequest.class))).thenReturn(vRes);

        PanVerificationResponse response = service.verifyPan(request);
        assertNotNull(response);
        assertFalse(response.getIsVerified());
        assertNotNull(response.getErrorDetails());
        assertEquals(PanConstants.ERR_CODE_RECORD_NOT_FOUND, response.getErrorDetails().getErrorCode());
    }

    @Test
    void testVerifyPan_NullGatewayResponse_ThrowsCustomException() {
        PanVerificationRequest request = PanVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .pan("BLRPA0001A")
                .fullName("Rahul Sharma")
                .dob("15/08/1990")
                .tenantId("pg.citya")
                .build();

        when(gatewayAdapter.verifyPan(any(VendorPanVerificationRequest.class))).thenReturn(null);

        CustomException ce = assertThrows(CustomException.class, () -> service.verifyPan(request));
        assertEquals(PanConstants.ERR_CODE_GATEWAY_ERROR, ce.getCode());
    }
}
