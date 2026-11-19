package org.egov.dx.validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.dx.util.TanConstants;
import org.egov.dx.web.models.TanVerificationRequest;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TanVerificationValidatorTest {

    private TanVerificationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TanVerificationValidator();
    }

    @Test
    void testValidateTanRequest_NullRequest() {
        CustomException ce = assertThrows(CustomException.class, () -> validator.validateTanRequest(null));
        assertEquals(TanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
    }

    @Test
    void testValidateTanRequest_NullRequestInfo() {
        TanVerificationRequest req = TanVerificationRequest.builder()
                .tan("BLRM12345C")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validateTanRequest(req));
        assertEquals(TanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
    }

    @Test
    void testValidateTanRequest_BlankTan() {
        TanVerificationRequest req = TanVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .tan("   ")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validateTanRequest(req));
        assertEquals(TanConstants.ERR_CODE_INVALID_FORMAT, ce.getCode());
        assertEquals(TanConstants.MSG_BLANK_TAN, ce.getMessage());
    }

    @Test
    void testValidateTanRequest_InvalidFormat() {
        TanVerificationRequest req = TanVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .tan("INVALID_TAN_123")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validateTanRequest(req));
        assertEquals(TanConstants.ERR_CODE_INVALID_FORMAT, ce.getCode());
        assertEquals(TanConstants.MSG_INVALID_FORMAT, ce.getMessage());
    }

    @Test
    void testValidateTanRequest_BlankTenantId() {
        TanVerificationRequest req = TanVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .tan("BLRM12345C")
                .tenantId("   ")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validateTanRequest(req));
        assertEquals(TanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
        assertTrue(ce.getMessage().contains("Tenant ID is mandatory"));
    }

    @Test
    void testValidateTanRequest_ValidSuccess() {
        TanVerificationRequest req = TanVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .tan("BLRM12345C")
                .tenantId("pg.citya")
                .build();
        assertDoesNotThrow(() -> validator.validateTanRequest(req));
    }
}
