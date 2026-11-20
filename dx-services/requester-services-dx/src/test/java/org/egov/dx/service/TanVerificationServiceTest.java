package org.egov.dx.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.dx.adapter.vendor.dto.VendorTanVerificationRequest;
import org.egov.dx.adapter.vendor.dto.VendorTanVerificationResponse;
import org.egov.dx.adapter.vendor.tan.TanGatewayAdapter;
import org.egov.dx.service.impl.TanVerificationServiceImpl;
import org.egov.dx.util.TanConstants;
import org.egov.dx.validator.TanVerificationValidator;
import org.egov.dx.web.models.TanVerificationRequest;
import org.egov.dx.web.models.TanVerificationResponse;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TanVerificationServiceTest {

    private TanGatewayAdapter gatewayAdapter;
    private TanVerificationValidator validator;
    private TenantVerificationConfigService tenantConfigService;
    private TanVerificationService service;

    @BeforeEach
    void setUp() {
        gatewayAdapter = mock(TanGatewayAdapter.class);
        validator = mock(TanVerificationValidator.class);
        tenantConfigService = mock(TenantVerificationConfigService.class);
        when(tenantConfigService.isVerificationEnabled(any(), any(), any())).thenReturn(true);
        service = new TanVerificationServiceImpl(gatewayAdapter, validator, tenantConfigService);
    }

    /**
     * Tests Verify Tan under the tenant disabled in mdms_throws custom exception scenario.
     *
     * Verifies that {@code CustomException} is thrown when tenant disabled in mdms_throws custom exception.
     */
    @Test
    void testVerifyTan_TenantDisabledInMdms_ThrowsCustomException() {
        TanVerificationRequest request = TanVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .tan("BLRM12345C")
                .tenantId("pg.disabledcity")
                .build();

        when(tenantConfigService.isVerificationEnabled(any(), eq("pg.disabledcity"), eq("TAN"))).thenReturn(false);

        CustomException ce = assertThrows(CustomException.class, () -> service.verifyTan(request));
        assertEquals(TanConstants.ERR_CODE_TENANT_DISABLED, ce.getCode());
        assertTrue(ce.getMessage().contains("TAN verification is not enabled for tenant"));
    }

    /**
     * Tests Verify Tan under the active success scenario.
     *
     * Verifies that the condition evaluates to true.
     */
    @Test
    void testVerifyTan_ActiveSuccess() {
        TanVerificationRequest request = TanVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .tan("BLRM12345C")
                .tenantId("pg.citya")
                .build();

        VendorTanVerificationResponse.VendorTanDetails vDetails = VendorTanVerificationResponse.VendorTanDetails.builder()
                .tan("BLRM12345C")
                .nameOfDeductor("INFRASTRUCTURE DEVELOPMENT CORP")
                .categoryOfDeductor("Company")
                .status("Active")
                .emailId("accounts@infradev.org")
                .address(VendorTanVerificationResponse.VendorAddress.builder()
                        .city("Bengaluru")
                        .state("Karnataka")
                        .pinCode("560100")
                        .build())
                .jurisdictionDetails(VendorTanVerificationResponse.VendorJurisdiction.builder()
                        .aoCode("BLRT12341")
                        .aoType("CIT")
                        .build())
                .build();

        VendorTanVerificationResponse vRes = VendorTanVerificationResponse.builder()
                .header(VendorTanVerificationResponse.ResponseHeader.builder()
                        .status(TanConstants.STATUS_SUCCESS)
                        .statusCode("200")
                        .build())
                .tanDetails(vDetails)
                .build();

        when(gatewayAdapter.verifyTan(any(VendorTanVerificationRequest.class))).thenReturn(vRes);

        TanVerificationResponse response = service.verifyTan(request);
        assertNotNull(response);
        assertTrue(response.getIsVerified());
        assertEquals("BLRM****5C", response.getMaskedTan());
        assertNotNull(response.getTanDetails());
        assertNull(response.getTanDetails().getTan()); // unmasked TAN omitted for PII protection
        assertEquals("BLRM****5C", response.getTanDetails().getMaskedTan());
        assertEquals("Company", response.getTanDetails().getCategoryOfDeductor());
        assertEquals("Active", response.getTanDetails().getStatus());
        assertEquals("Bengaluru", response.getTanDetails().getAddress().getCity());

        verify(validator, times(1)).validateTanRequest(request);
    }

    /**
     * Tests Verify Tan under the with deductor name_matching scenario.
     *
     * Verifies that the condition evaluates to true.
     */
    @Test
    void testVerifyTan_WithDeductorName_Matching() {
        TanVerificationRequest request = TanVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .tan("BLRM12345C")
                .deductorName("INFRASTRUCTURE DEVELOPMENT CORP")
                .tenantId("pg.citya")
                .build();

        VendorTanVerificationResponse.VendorTanDetails vDetails = VendorTanVerificationResponse.VendorTanDetails.builder()
                .tan("BLRM12345C")
                .nameOfDeductor("INFRASTRUCTURE DEVELOPMENT CORP")
                .categoryOfDeductor("Company")
                .status("Active")
                .build();

        VendorTanVerificationResponse vRes = VendorTanVerificationResponse.builder()
                .header(VendorTanVerificationResponse.ResponseHeader.builder()
                        .status(TanConstants.STATUS_SUCCESS)
                        .statusCode("200")
                        .build())
                .tanDetails(vDetails)
                .build();

        when(gatewayAdapter.verifyTan(any())).thenReturn(vRes);

        TanVerificationResponse response = service.verifyTan(request);
        assertTrue(response.getIsVerified());
        assertTrue(response.getTanDetails().getNameMatch());
    }

    /**
     * Tests Verify Tan under the with deductor name_mismatched scenario.
     *
     * Verifies that the check evaluates to false.
     */
    @Test
    void testVerifyTan_WithDeductorName_Mismatched() {
        TanVerificationRequest request = TanVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .tan("BLRM12345C")
                .deductorName("WRONG CORP")
                .tenantId("pg.citya")
                .build();

        VendorTanVerificationResponse.VendorTanDetails vDetails = VendorTanVerificationResponse.VendorTanDetails.builder()
                .tan("BLRM12345C")
                .nameOfDeductor("INFRASTRUCTURE DEVELOPMENT CORP")
                .categoryOfDeductor("Company")
                .status("Active")
                .build();

        VendorTanVerificationResponse vRes = VendorTanVerificationResponse.builder()
                .header(VendorTanVerificationResponse.ResponseHeader.builder()
                        .status(TanConstants.STATUS_SUCCESS)
                        .statusCode("200")
                        .build())
                .tanDetails(vDetails)
                .build();

        when(gatewayAdapter.verifyTan(any())).thenReturn(vRes);

        TanVerificationResponse response = service.verifyTan(request);
        assertFalse(response.getIsVerified());
        assertFalse(response.getTanDetails().getNameMatch());
    }

    /**
     * Tests Verify Tan under the inactive record scenario.
     *
     * Verifies that the check evaluates to false.
     */
    @Test
    void testVerifyTan_InactiveRecord() {
        TanVerificationRequest request = TanVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .tan("CHDA11223D")
                .tenantId("pg.citya")
                .build();

        VendorTanVerificationResponse.VendorTanDetails vDetails = VendorTanVerificationResponse.VendorTanDetails.builder()
                .tan("CHDA11223D")
                .nameOfDeductor("HERITAGE PROPERTIES")
                .categoryOfDeductor("Autonomous Body")
                .status("Inactive")
                .build();

        VendorTanVerificationResponse vRes = VendorTanVerificationResponse.builder()
                .header(VendorTanVerificationResponse.ResponseHeader.builder()
                        .status(TanConstants.STATUS_SUCCESS)
                        .statusCode("200")
                        .build())
                .tanDetails(vDetails)
                .build();

        when(gatewayAdapter.verifyTan(any(VendorTanVerificationRequest.class))).thenReturn(vRes);

        TanVerificationResponse response = service.verifyTan(request);
        assertNotNull(response);
        assertFalse(response.getIsVerified());
        assertEquals("Inactive", response.getTanDetails().getStatus());
    }

    /**
     * Tests Verify Tan under the gateway error details scenario.
     *
     * Verifies that {@code CustomException} is thrown when gateway error details.
     */
    @Test
    void testVerifyTan_GatewayErrorDetails() {
        TanVerificationRequest request = TanVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .tan("BLRM12345C")
                .tenantId("pg.citya")
                .build();

        VendorTanVerificationResponse vRes = VendorTanVerificationResponse.builder()
                .header(VendorTanVerificationResponse.ResponseHeader.builder()
                        .status(TanConstants.STATUS_FAILURE)
                        .statusCode("404")
                        .build())
                .errorDetails(VendorTanVerificationResponse.VendorErrorDetails.builder()
                        .errorCode(TanConstants.ERR_CODE_RECORD_NOT_FOUND)
                        .errorMessage(TanConstants.MSG_RECORD_NOT_FOUND)
                        .build())
                .build();

        when(gatewayAdapter.verifyTan(any(VendorTanVerificationRequest.class))).thenReturn(vRes);

        TanVerificationResponse response = service.verifyTan(request);
        assertNotNull(response);
        assertFalse(response.getIsVerified());
        assertNotNull(response.getErrorDetails());
        assertEquals(TanConstants.ERR_CODE_RECORD_NOT_FOUND, response.getErrorDetails().getErrorCode());
    }

    /**
     * Tests Verify Tan under the null gateway response_throws custom exception scenario.
     *
     * Verifies that {@code CustomException} is thrown when null gateway response_throws custom exception.
     */
    @Test
    void testVerifyTan_NullGatewayResponse_ThrowsCustomException() {
        TanVerificationRequest request = TanVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .tan("BLRM12345C")
                .tenantId("pg.citya")
                .build();

        when(gatewayAdapter.verifyTan(any(VendorTanVerificationRequest.class))).thenReturn(null);

        CustomException ce = assertThrows(CustomException.class, () -> service.verifyTan(request));
        assertEquals(TanConstants.ERR_CODE_GATEWAY_ERROR, ce.getCode());
    }
}
