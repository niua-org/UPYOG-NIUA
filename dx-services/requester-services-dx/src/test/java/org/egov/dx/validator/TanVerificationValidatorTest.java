package org.egov.dx.validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.dx.util.TanConstants;
import org.egov.dx.web.models.TanVerificationRequest;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TanVerificationValidatorTest {

    private TanVerificationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TanVerificationValidator();
    }

    private RequestInfo createAuthenticatedRequestInfo() {
        return RequestInfo.builder()
                .apiId("org.egov.dx")
                .ver("1.0")
                .action("verify")
                .msgId("msg-1")
                .userInfo(User.builder().uuid("usr-1234").userName("tester").build())
                .build();
    }

    /**
     * Tests Validate Tan Request under the null request scenario.
     *
     * Verifies that {@code CustomException} is thrown when null request.
     */
    @Test
    void testValidateTanRequest_NullRequest() {
        CustomException ce = assertThrows(CustomException.class, () -> validator.validateTanRequest(null));
        assertEquals(TanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
    }

    /**
     * Tests Validate Tan Request under the null request info scenario.
     *
     * Verifies that {@code CustomException} is thrown when null request info.
     */
    @Test
    void testValidateTanRequest_NullRequestInfo() {
        TanVerificationRequest req = TanVerificationRequest.builder()
                .tan("BLRM12345C")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validateTanRequest(req));
        assertEquals(TanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
    }

    /**
     * Tests Validate Tan Request under the unauthenticated user scenario.
     *
     * Verifies that {@code CustomException} is thrown when unauthenticated user.
     */
    @Test
    void testValidateTanRequest_UnauthenticatedUser() {
        TanVerificationRequest req = TanVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .tan("BLRM12345C")
                .tenantId("pg.citya")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validateTanRequest(req));
        assertEquals(TanConstants.ERR_CODE_UNAUTHORIZED_USER, ce.getCode());
    }

    /**
     * Tests Validate Tan Request under the blank tan scenario.
     *
     * Verifies that {@code CustomException} is thrown when blank tan.
     */
    @Test
    void testValidateTanRequest_BlankTan() {
        TanVerificationRequest req = TanVerificationRequest.builder()
                .requestInfo(createAuthenticatedRequestInfo())
                .tan("   ")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validateTanRequest(req));
        assertEquals(TanConstants.ERR_CODE_INVALID_FORMAT, ce.getCode());
        assertEquals(TanConstants.MSG_BLANK_TAN, ce.getMessage());
    }

    /**
     * Tests Validate Tan Request under the invalid format scenario.
     *
     * Verifies that {@code CustomException} is thrown when invalid format.
     */
    @Test
    void testValidateTanRequest_InvalidFormat() {
        TanVerificationRequest req = TanVerificationRequest.builder()
                .requestInfo(createAuthenticatedRequestInfo())
                .tan("INVALID_TAN_123")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validateTanRequest(req));
        assertEquals(TanConstants.ERR_CODE_INVALID_FORMAT, ce.getCode());
        assertEquals(TanConstants.MSG_INVALID_FORMAT, ce.getMessage());
    }

    /**
     * Tests Validate Tan Request under the deductor name too long scenario.
     *
     * Verifies that {@code CustomException} is thrown when deductor name too long.
     */
    @Test
    void testValidateTanRequest_DeductorNameTooLong() {
        String longName = "A".repeat(201);
        TanVerificationRequest req = TanVerificationRequest.builder()
                .requestInfo(createAuthenticatedRequestInfo())
                .tan("BLRM12345C")
                .deductorName(longName)
                .tenantId("pg.citya")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validateTanRequest(req));
        assertEquals(TanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
        assertTrue(ce.getMessage().contains("Deductor name cannot exceed"));
    }

    /**
     * Tests Validate Tan Request under the blank tenant id scenario.
     *
     * Verifies that {@code CustomException} is thrown when blank tenant id.
     */
    @Test
    void testValidateTanRequest_BlankTenantId() {
        TanVerificationRequest req = TanVerificationRequest.builder()
                .requestInfo(createAuthenticatedRequestInfo())
                .tan("BLRM12345C")
                .tenantId("   ")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validateTanRequest(req));
        assertEquals(TanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
        assertTrue(ce.getMessage().contains("Tenant ID is mandatory"));
    }

    /**
     * Tests Validate Tan Request under the valid success scenario.
     *
     * Verifies that execution completes without throwing any exception.
     */
    @Test
    void testValidateTanRequest_ValidSuccess() {
        TanVerificationRequest req = TanVerificationRequest.builder()
                .requestInfo(createAuthenticatedRequestInfo())
                .tan("BLRM12345C")
                .deductorName("Valid Deductor")
                .tenantId("pg.citya")
                .build();
        assertDoesNotThrow(() -> validator.validateTanRequest(req));
    }
}
