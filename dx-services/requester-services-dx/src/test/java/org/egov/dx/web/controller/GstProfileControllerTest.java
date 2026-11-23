package org.egov.dx.web.controller;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.dx.service.GstProfileService;
import org.egov.dx.web.models.GstProfileRequest;
import org.egov.dx.web.models.GstProfileResponse;
import org.egov.dx.web.models.GstTaxpayerDetails;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GstProfileControllerTest {

    private GstProfileService gstProfileService;
    private GstProfileController controller;

    @BeforeEach
    void setUp() {
        gstProfileService = mock(GstProfileService.class);
        controller = new GstProfileController(gstProfileService);
    }

    /**
     * Tests Search Gstin under the delegates to service and returns ok scenario.
     *
     * Verifies interactions with the underlying mock dependencies.
     */
    @Test
    void testSearchGstin_DelegatesToServiceAndReturnsOk() {
        GstProfileRequest request = GstProfileRequest.builder()
                .requestInfo(new RequestInfo())
                .gstin("03PTLAM0012A1Z3")
                .tenantId("pg.citya")
                .build();

        GstProfileResponse expectedResponse = GstProfileResponse.builder()
                .responseInfo(new ResponseInfo())
                .isVerified(true)
                .maskedGstin("03PTLA****1Z3")
                .taxpayerDetails(GstTaxpayerDetails.builder()
                        .gstin("03PTLAM0012A1Z3")
                        .legalName("MOCK MUNICIPAL CORPORATION AMRITSAR")
                        .status("Active")
                        .build())
                .build();

        when(gstProfileService.searchGstin(request)).thenReturn(expectedResponse);

        ResponseEntity<GstProfileResponse> responseEntity = controller.searchGstin(request);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(expectedResponse, responseEntity.getBody());
        verify(gstProfileService, times(1)).searchGstin(request);
    }

    /**
     * Tests Search Gstin under the when service throws exception scenario.
     *
     * Verifies the expected response and model assertions.
     */
    @Test
    void testSearchGstin_WhenServiceThrowsException() {
        GstProfileRequest request = GstProfileRequest.builder()
                .requestInfo(new RequestInfo())
                .gstin("03PTLAM0012A1Z3")
                .tenantId("pg.disabled")
                .build();

        when(gstProfileService.searchGstin(request))
                .thenThrow(new CustomException("VERIFICATION_DISABLED", "Disabled in MDMS"));

        assertThrows(CustomException.class, () -> controller.searchGstin(request));
    }
}
