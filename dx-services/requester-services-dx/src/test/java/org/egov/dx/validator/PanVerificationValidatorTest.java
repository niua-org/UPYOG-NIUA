package org.egov.dx.validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.dx.service.TenantVerificationConfigService;
import org.egov.dx.util.PanConstants;
import org.egov.dx.web.models.PanVerificationRequest;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PanVerificationValidatorTest {

    private TenantVerificationConfigService tenantConfigService;
    private PanVerificationValidator validator;

    @BeforeEach
    void setUp() {
        tenantConfigService = mock(TenantVerificationConfigService.class);
        when(tenantConfigService.getValidGenders(any(), any()))
                .thenReturn(Set.of("M", "F", "O", "T", "MALE", "FEMALE", "TRANSGENDER", "OTHER"));
        validator = new PanVerificationValidator(tenantConfigService);
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
     * Tests Validate Pan Request under the null request scenario.
     *
     * Verifies that {@code CustomException} is thrown when null request.
     */
    @Test
    void testValidatePanRequest_NullRequest() {
        CustomException ce = assertThrows(CustomException.class, () -> validator.validatePanRequest(null));
        assertEquals(PanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
    }

    /**
     * Tests Validate Pan Request under the null request info scenario.
     *
     * Verifies that {@code CustomException} is thrown when null request info.
     */
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

    /**
     * Tests Validate Pan Request under the unauthenticated user scenario.
     *
     * Verifies that {@code CustomException} is thrown when unauthenticated user.
     */
    @Test
    void testValidatePanRequest_UnauthenticatedUser() {
        PanVerificationRequest req = PanVerificationRequest.builder()
                .requestInfo(new RequestInfo())
                .pan("ABCDE1234F")
                .fullName("John Doe")
                .dob("1990-01-01")
                .tenantId("pg.citya")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validatePanRequest(req));
        assertEquals(PanConstants.ERR_CODE_UNAUTHORIZED_USER, ce.getCode());
    }

    /**
     * Tests Validate Pan Request under the blank pan scenario.
     *
     * Verifies that {@code CustomException} is thrown when blank pan.
     */
    @Test
    void testValidatePanRequest_BlankPan() {
        PanVerificationRequest req = PanVerificationRequest.builder()
                .requestInfo(createAuthenticatedRequestInfo())
                .pan("   ")
                .fullName("John Doe")
                .dob("1990-01-01")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validatePanRequest(req));
        assertEquals(PanConstants.ERR_CODE_INVALID_FORMAT, ce.getCode());
        assertEquals(PanConstants.MSG_BLANK_PAN, ce.getMessage());
    }

    /**
     * Tests Validate Pan Request under the invalid format scenario.
     *
     * Verifies that {@code CustomException} is thrown when invalid format.
     */
    @Test
    void testValidatePanRequest_InvalidFormat() {
        PanVerificationRequest req = PanVerificationRequest.builder()
                .requestInfo(createAuthenticatedRequestInfo())
                .pan("INVALID_PAN_123")
                .fullName("John Doe")
                .dob("1990-01-01")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validatePanRequest(req));
        assertEquals(PanConstants.ERR_CODE_INVALID_FORMAT, ce.getCode());
        assertEquals(PanConstants.MSG_INVALID_FORMAT, ce.getMessage());
    }

    /**
     * Tests Validate Pan Request under the blank full name scenario.
     *
     * Verifies that {@code CustomException} is thrown when blank full name.
     */
    @Test
    void testValidatePanRequest_BlankFullName() {
        PanVerificationRequest req = PanVerificationRequest.builder()
                .requestInfo(createAuthenticatedRequestInfo())
                .pan("ABCDE1234F")
                .fullName("   ")
                .dob("1990-01-01")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validatePanRequest(req));
        assertEquals(PanConstants.ERR_CODE_MISSING_NAME, ce.getCode());
        assertEquals(PanConstants.MSG_MISSING_NAME, ce.getMessage());
    }

    /**
     * Tests Validate Pan Request under the full name too short scenario.
     *
     * Verifies that {@code CustomException} is thrown when full name too short.
     */
    @Test
    void testValidatePanRequest_FullNameTooShort() {
        PanVerificationRequest req = PanVerificationRequest.builder()
                .requestInfo(createAuthenticatedRequestInfo())
                .pan("ABCDE1234F")
                .fullName("A")
                .dob("1990-01-01")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validatePanRequest(req));
        assertEquals(PanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
        assertTrue(ce.getMessage().contains("Full name must be between"));
    }

    /**
     * Tests Validate Pan Request under the blank dob scenario.
     *
     * Verifies that {@code CustomException} is thrown when blank dob.
     */
    @Test
    void testValidatePanRequest_BlankDob() {
        PanVerificationRequest req = PanVerificationRequest.builder()
                .requestInfo(createAuthenticatedRequestInfo())
                .pan("ABCDE1234F")
                .fullName("John Doe")
                .dob("   ")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validatePanRequest(req));
        assertEquals(PanConstants.ERR_CODE_MISSING_DOB, ce.getCode());
        assertEquals(PanConstants.MSG_MISSING_DOB, ce.getMessage());
    }

    /**
     * Tests Validate Pan Request under the invalid dob format scenario.
     *
     * Verifies that {@code CustomException} is thrown when invalid dob format.
     */
    @Test
    void testValidatePanRequest_InvalidDobFormat() {
        PanVerificationRequest req = PanVerificationRequest.builder()
                .requestInfo(createAuthenticatedRequestInfo())
                .pan("ABCDE1234F")
                .fullName("John Doe")
                .dob("01-Jan-1990")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validatePanRequest(req));
        assertEquals(PanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
    }

    /**
     * Tests Validate Pan Request under the future dob scenario.
     *
     * Verifies that {@code CustomException} is thrown when future dob.
     */
    @Test
    void testValidatePanRequest_FutureDob() {
        PanVerificationRequest req = PanVerificationRequest.builder()
                .requestInfo(createAuthenticatedRequestInfo())
                .pan("ABCDE1234F")
                .fullName("John Doe")
                .dob("2099-01-01")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validatePanRequest(req));
        assertEquals(PanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
        assertTrue(ce.getMessage().contains("future"));
    }

    /**
     * Tests Validate Pan Request under the invalid gender scenario.
     *
     * Verifies that {@code CustomException} is thrown when invalid gender.
     */
    @Test
    void testValidatePanRequest_InvalidGender() {
        PanVerificationRequest req = PanVerificationRequest.builder()
                .requestInfo(createAuthenticatedRequestInfo())
                .pan("ABCDE1234F")
                .fullName("John Doe")
                .dob("1990-01-01")
                .gender("INVALID_GENDER")
                .tenantId("pg.citya")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validatePanRequest(req));
        assertEquals(PanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
        assertTrue(ce.getMessage().contains("Invalid gender"));
    }

    /**
     * Tests Validate Pan Request under the blank tenant id scenario.
     *
     * Verifies that {@code CustomException} is thrown when blank tenant id.
     */
    @Test
    void testValidatePanRequest_BlankTenantId() {
        PanVerificationRequest req = PanVerificationRequest.builder()
                .requestInfo(createAuthenticatedRequestInfo())
                .pan("ABCDE1234F")
                .fullName("John Doe")
                .dob("1990-01-01")
                .tenantId("   ")
                .build();
        CustomException ce = assertThrows(CustomException.class, () -> validator.validatePanRequest(req));
        assertEquals(PanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
        assertTrue(ce.getMessage().contains("Tenant ID is mandatory"));
    }

    /**
     * Tests Validate Pan Request under the valid success scenario.
     *
     * Verifies that execution completes without throwing any exception.
     */
    @Test
    void testValidatePanRequest_ValidSuccess() {
        PanVerificationRequest req = PanVerificationRequest.builder()
                .requestInfo(createAuthenticatedRequestInfo())
                .pan("ABCDE1234F")
                .fullName("John Doe")
                .dob("1990-01-01")
                .gender("M")
                .tenantId("pg.citya")
                .build();
        assertDoesNotThrow(() -> validator.validatePanRequest(req));
    }
}
