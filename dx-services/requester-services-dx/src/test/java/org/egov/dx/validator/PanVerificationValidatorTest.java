package org.egov.dx.validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.dx.util.PanConstants;
import org.egov.dx.web.models.PanVerificationRequest;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PanVerificationValidatorTest {

    private PanVerificationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PanVerificationValidator();
    }

    @Test
    void testValidatePanRequest_NullRequest() {
        CustomException ce = assertThrows(CustomException.class, () -> validator.validatePanRequest(null));
        assertEquals(PanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
    }

    @Test
    void testValidatePanRequest_NullRequestInfo() {
        PanVerificationRequest req = PanVerificationRequest.builder()
                .pan("ABCDE1234F")
                .fullName("John Doe")
                .dob("1990-01-01")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validatePanRequest(req));
        assertEquals(PanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
    }

    @Test
    void testValidatePanRequest_BlankPan() {
        PanVerificationRequest req = PanVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .pan("   ")
                .fullName("John Doe")
                .dob("1990-01-01")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validatePanRequest(req));
        assertEquals(PanConstants.ERR_CODE_INVALID_FORMAT, ce.getCode());
        assertEquals(PanConstants.MSG_BLANK_PAN, ce.getMessage());
    }

    @Test
    void testValidatePanRequest_InvalidFormat() {
        PanVerificationRequest req = PanVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .pan("INVALID_PAN_123")
                .fullName("John Doe")
                .dob("1990-01-01")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validatePanRequest(req));
        assertEquals(PanConstants.ERR_CODE_INVALID_FORMAT, ce.getCode());
        assertEquals(PanConstants.MSG_INVALID_FORMAT, ce.getMessage());
    }

    @Test
    void testValidatePanRequest_BlankFullName() {
        PanVerificationRequest req = PanVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .pan("ABCDE1234F")
                .fullName("   ")
                .dob("1990-01-01")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validatePanRequest(req));
        assertEquals(PanConstants.ERR_CODE_MISSING_NAME, ce.getCode());
        assertEquals(PanConstants.MSG_MISSING_NAME, ce.getMessage());
    }

    @Test
    void testValidatePanRequest_BlankDob() {
        PanVerificationRequest req = PanVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .pan("ABCDE1234F")
                .fullName("John Doe")
                .dob("   ")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validatePanRequest(req));
        assertEquals(PanConstants.ERR_CODE_MISSING_DOB, ce.getCode());
        assertEquals(PanConstants.MSG_MISSING_DOB, ce.getMessage());
    }

    @Test
    void testValidatePanRequest_BlankTenantId() {
        PanVerificationRequest req = PanVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .pan("ABCDE1234F")
                .fullName("John Doe")
                .dob("1990-01-01")
                .tenantId("   ")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validatePanRequest(req));
        assertEquals(PanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
        assertTrue(ce.getMessage().contains("Tenant ID is mandatory"));
    }

    @Test
    void testValidatePanRequest_ValidSuccess() {
        PanVerificationRequest req = PanVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .pan("ABCDE1234F")
                .fullName("John Doe")
                .dob("1990-01-01")
                .tenantId("pg.citya")
                .build();
        assertDoesNotThrow(() -> validator.validatePanRequest(req));
    }
}
