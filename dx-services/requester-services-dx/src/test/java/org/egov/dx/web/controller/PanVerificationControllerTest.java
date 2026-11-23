package org.egov.dx.web.controller;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.dx.service.PanVerificationService;
import org.egov.dx.web.models.PanDetails;
import org.egov.dx.web.models.PanVerificationRequest;
import org.egov.dx.web.models.PanVerificationResponse;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PanVerificationControllerTest {

    private PanVerificationService panVerificationService;
    private PanVerificationController controller;

    @BeforeEach
    void setUp() {
        panVerificationService = mock(PanVerificationService.class);
        controller = new PanVerificationController(panVerificationService);
    }

    /**
     * Tests Verify Pan under the delegates to service and returns ok scenario.
     *
     * Verifies interactions with the underlying mock dependencies.
     */
    @Test
    void testVerifyPan_DelegatesToServiceAndReturnsOk() {
        PanVerificationRequest request = PanVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .pan("BLRPA0001A")
                .fullName("Rahul Sharma")
                .dob("15/08/1990")
                .tenantId("pg.citya")
                .build();

        PanVerificationResponse expectedResponse = PanVerificationResponse.builder()
                .responseInfo(new ResponseInfo())
                .isVerified(true)
                .maskedPan("BLRPA****A")
                .panDetails(PanDetails.builder()
                        .pan("BLRPA0001A")
                        .fullName("Rahul Sharma")
                        .category("Individual")
                        .panStatus("Existing and Valid")
                        .nameMatch(true)
                        .dobMatch(true)
                        .build())
                .build();

        when(panVerificationService.verifyPan(request)).thenReturn(expectedResponse);

        ResponseEntity<PanVerificationResponse> responseEntity = controller.verifyPan(request);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(expectedResponse, responseEntity.getBody());
        verify(panVerificationService, times(1)).verifyPan(request);
    }

    /**
     * Tests Verify Pan under the when service throws exception scenario.
     *
     * Verifies the expected response and model assertions.
     */
    @Test
    void testVerifyPan_WhenServiceThrowsException() {
        PanVerificationRequest request = PanVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .pan("BLRPA0001A")
                .fullName("Rahul Sharma")
                .dob("15/08/1990")
                .tenantId("pg.disabled")
                .build();

        when(panVerificationService.verifyPan(request))
                .thenThrow(new CustomException("VERIFICATION_DISABLED", "Disabled in MDMS"));

        assertThrows(CustomException.class, () -> controller.verifyPan(request));
    }
}
