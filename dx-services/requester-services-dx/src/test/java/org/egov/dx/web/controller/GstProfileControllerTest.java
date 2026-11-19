package org.egov.dx.web.controller;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.dx.service.GstProfileService;
import org.egov.dx.web.models.GstProfileRequest;
import org.egov.dx.web.models.GstProfileResponse;
import org.egov.dx.web.models.GstTaxpayerDetails;
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

    @Test
    void testSearchGstin_DelegatesToServiceAndReturnsOk() {
        GstProfileRequest request = GstProfileRequest.builder()
                .requestInfo(new RequestInfo())
                .gstin("03PTLAM0012A1Z5")
                .tenantId("pg.citya")
                .build();

        GstProfileResponse expectedResponse = GstProfileResponse.builder()
                .responseInfo(new ResponseInfo())
                .isVerified(true)
                .maskedGstin("03PTLA****1Z5")
                .taxpayerDetails(GstTaxpayerDetails.builder()
                        .gstin("03PTLAM0012A1Z5")
                        .legalName("MUNICIPAL CORPORATION AMRITSAR")
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
}
