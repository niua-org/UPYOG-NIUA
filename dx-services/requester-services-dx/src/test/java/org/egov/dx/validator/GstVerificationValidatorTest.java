package org.egov.dx.validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.dx.util.GstConstants;
import org.egov.dx.web.models.GstProfileRequest;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GstVerificationValidatorTest {

    private GstVerificationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new GstVerificationValidator();
    }

    @Test
    void testValidateGstRequest_NullRequest() {
        CustomException ce = assertThrows(CustomException.class, () -> validator.validateGstRequest(null));
        assertEquals(GstConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
    }

    @Test
    void testValidateGstRequest_NullRequestInfo() {
        GstProfileRequest req = GstProfileRequest.builder()
                .gstin("03AAAPS0001B1Z5")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validateGstRequest(req));
        assertEquals(GstConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
    }

    @Test
    void testValidateGstRequest_BlankGstin() {
        GstProfileRequest req = GstProfileRequest.builder()
                .requestInfo(new RequestInfo())
                .gstin("   ")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validateGstRequest(req));
        assertEquals(GstConstants.ERR_CODE_INVALID_FORMAT, ce.getCode());
        assertEquals(GstConstants.MSG_BLANK_GSTIN, ce.getMessage());
    }

    @Test
    void testValidateGstRequest_InvalidFormat() {
        GstProfileRequest req = GstProfileRequest.builder()
                .requestInfo(new RequestInfo())
                .gstin("INVALID_GSTIN_12")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validateGstRequest(req));
        assertEquals(GstConstants.ERR_CODE_INVALID_FORMAT, ce.getCode());
        assertEquals(GstConstants.MSG_INVALID_FORMAT, ce.getMessage());
    }

    @Test
    void testValidateGstRequest_BlankTenantId() {
        GstProfileRequest req = GstProfileRequest.builder()
                .requestInfo(new RequestInfo())
                .gstin("03AAAPS0001B1Z5")
                .tenantId("   ")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validateGstRequest(req));
        assertEquals(GstConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
        assertTrue(ce.getMessage().contains("Tenant ID is mandatory"));
    }

    @Test
    void testValidateGstRequest_ValidSuccess() {
        GstProfileRequest req = GstProfileRequest.builder()
                .requestInfo(new RequestInfo())
                .gstin("03AAAPS0001B1Z5")
                .tenantId("pg.citya")
                .build();
        assertDoesNotThrow(() -> validator.validateGstRequest(req));
    }
}
