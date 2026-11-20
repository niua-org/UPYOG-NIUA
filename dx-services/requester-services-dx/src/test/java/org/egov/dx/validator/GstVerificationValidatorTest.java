package org.egov.dx.validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
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

    private RequestInfo createAuthenticatedRequestInfo() {
        return RequestInfo.builder()
                .apiId("org.egov.dx")
                .ver("1.0")
                .action("search")
                .msgId("msg-1")
                .userInfo(User.builder().uuid("usr-1234").userName("tester").build())
                .build();
    }

    /**
     * Tests Validate Gst Request under the null request scenario.
     *
     * Verifies that {@code CustomException} is thrown when null request.
     */
    @Test
    void testValidateGstRequest_NullRequest() {
        CustomException ce = assertThrows(CustomException.class, () -> validator.validateGstRequest(null));
        assertEquals(GstConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
    }

    /**
     * Tests Validate Gst Request under the null request info scenario.
     *
     * Verifies that {@code CustomException} is thrown when null request info.
     */
    @Test
    void testValidateGstRequest_NullRequestInfo() {
        GstProfileRequest req = GstProfileRequest.builder()
                .gstin("03AAAPS0001B1ZW")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validateGstRequest(req));
        assertEquals(GstConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
    }

    /**
     * Tests Validate Gst Request under the unauthenticated user scenario.
     *
     * Verifies that {@code CustomException} is thrown when unauthenticated user.
     */
    @Test
    void testValidateGstRequest_UnauthenticatedUser() {
        GstProfileRequest req = GstProfileRequest.builder()
                .requestInfo(new RequestInfo())
                .gstin("03AAAPS0001B1ZW")
                .tenantId("pg.citya")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validateGstRequest(req));
        assertEquals(GstConstants.ERR_CODE_UNAUTHORIZED_USER, ce.getCode());
    }

    /**
     * Tests Validate Gst Request under the blank gstin scenario.
     *
     * Verifies that {@code CustomException} is thrown when blank gstin.
     */
    @Test
    void testValidateGstRequest_BlankGstin() {
        GstProfileRequest req = GstProfileRequest.builder()
                .requestInfo(createAuthenticatedRequestInfo())
                .gstin("   ")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validateGstRequest(req));
        assertEquals(GstConstants.ERR_CODE_INVALID_FORMAT, ce.getCode());
        assertEquals(GstConstants.MSG_BLANK_GSTIN, ce.getMessage());
    }

    /**
     * Tests Validate Gst Request under the invalid format scenario.
     *
     * Verifies that {@code CustomException} is thrown when invalid format.
     */
    @Test
    void testValidateGstRequest_InvalidFormat() {
        GstProfileRequest req = GstProfileRequest.builder()
                .requestInfo(createAuthenticatedRequestInfo())
                .gstin("INVALID_GSTIN_12")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validateGstRequest(req));
        assertEquals(GstConstants.ERR_CODE_INVALID_FORMAT, ce.getCode());
        assertEquals(GstConstants.MSG_INVALID_FORMAT, ce.getMessage());
    }

    /**
     * Tests Validate Gst Request under the invalid checksum scenario.
     *
     * Verifies that {@code CustomException} is thrown when invalid checksum.
     */
    @Test
    void testValidateGstRequest_InvalidChecksum() {
        // Correct regex format but invalid Mod-36 check digit ('5' instead of 'W')
        GstProfileRequest req = GstProfileRequest.builder()
                .requestInfo(createAuthenticatedRequestInfo())
                .gstin("03AAAPS0001B1Z5")
                .tenantId("pg.citya")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validateGstRequest(req));
        assertEquals(GstConstants.ERR_CODE_INVALID_CHECKSUM, ce.getCode());
        assertEquals(GstConstants.MSG_INVALID_CHECKSUM, ce.getMessage());
    }

    /**
     * Tests Validate Gst Request under the blank tenant id scenario.
     *
     * Verifies that {@code CustomException} is thrown when blank tenant id.
     */
    @Test
    void testValidateGstRequest_BlankTenantId() {
        GstProfileRequest req = GstProfileRequest.builder()
                .requestInfo(createAuthenticatedRequestInfo())
                .gstin("03AAAPS0001B1ZW")
                .tenantId("   ")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validateGstRequest(req));
        assertEquals(GstConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
        assertTrue(ce.getMessage().contains("Tenant ID is mandatory"));
    }

    /**
     * Tests Validate Gst Request under the valid success scenario.
     *
     * Verifies that execution completes without throwing any exception.
     */
    @Test
    void testValidateGstRequest_ValidSuccess() {
        GstProfileRequest req = GstProfileRequest.builder()
                .requestInfo(createAuthenticatedRequestInfo())
                .gstin("03AAAPS0001B1ZW")
                .tenantId("pg.citya")
                .build();
        assertDoesNotThrow(() -> validator.validateGstRequest(req));
    }
}
