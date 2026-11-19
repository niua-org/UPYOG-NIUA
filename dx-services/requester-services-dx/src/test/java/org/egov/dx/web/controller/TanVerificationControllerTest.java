package org.egov.dx.web.controller;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.dx.service.TanVerificationService;
import org.egov.dx.web.models.TanDetails;
import org.egov.dx.web.models.TanVerificationRequest;
import org.egov.dx.web.models.TanVerificationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TanVerificationControllerTest {

    private TanVerificationService tanVerificationService;
    private TanVerificationController controller;

    @BeforeEach
    void setUp() {
        tanVerificationService = mock(TanVerificationService.class);
        controller = new TanVerificationController(tanVerificationService);
    }

    @Test
    void testVerifyTan_DelegatesToServiceAndReturnsOk() {
        TanVerificationRequest request = TanVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .tan("BLRM12345C")
                .tenantId("pg.citya")
                .build();

        TanVerificationResponse expectedResponse = TanVerificationResponse.builder()
                .responseInfo(new ResponseInfo())
                .isVerified(true)
                .maskedTan("BLRM****5C")
                .tanDetails(TanDetails.builder()
                        .tan("BLRM12345C")
                        .nameOfDeductor("INFRASTRUCTURE DEVELOPMENT CORP")
                        .categoryOfDeductor("Company")
                        .status("Active")
                        .build())
                .build();

        when(tanVerificationService.verifyTan(request)).thenReturn(expectedResponse);

        ResponseEntity<TanVerificationResponse> responseEntity = controller.verifyTan(request);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(expectedResponse, responseEntity.getBody());
        verify(tanVerificationService, times(1)).verifyTan(request);
    }
}
