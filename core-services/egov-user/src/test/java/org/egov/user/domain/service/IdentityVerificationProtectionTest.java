package org.egov.user.domain.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.user.domain.model.Role;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.utils.EncryptionDecryptionUtil;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.UserRequest;
import org.egov.user.web.contract.UserRequestV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class IdentityVerificationProtectionTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncryptionDecryptionUtil encryptionDecryptionUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    public void setUp() {
        userService = new UserService(
                userRepository,
                null,
                null,
                passwordEncoder,
                encryptionDecryptionUtil,
                90,
                false,
                false,
                "pg",
                10,
                10,
                null
        );

        when(encryptionDecryptionUtil.encryptObject(any(User.class), any(), eq(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(encryptionDecryptionUtil.decryptObject(any(User.class), any(), eq(User.class), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("updateWithoutOtpValidation should preserve existing isIdentityVerified=false even if caller sends true")
    public void testUpdateWithoutOtpValidation_PreservesUnverifiedStatus() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(org.egov.common.contract.request.User.builder()
                        .id(1L)
                        .uuid("emp-caller")
                        .type("EMPLOYEE")
                        .build())
                .build();

        User existingDbUser = User.builder()
                .id(100L)
                .uuid("user-100")
                .username("emp100")
                .tenantId("pg")
                .type(UserType.EMPLOYEE)
                .roles(Collections.singleton(Role.builder().name("EMPLOYEE").code("EMPLOYEE").build()))
                .isIdentityVerified(false)
                .identityVerifiedDate(null)
                .identityVerifiedSource(null)
                .accountLocked(false)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(existingDbUser));

        User incomingUpdate = User.builder()
                .id(100L)
                .uuid("user-100")
                .username("emp100")
                .tenantId("pg")
                .type(UserType.EMPLOYEE)
                .roles(Collections.singleton(Role.builder().name("EMPLOYEE").code("EMPLOYEE").build()))
                .isIdentityVerified(true)
                .identityVerifiedDate(new Date())
                .identityVerifiedSource("MALICIOUS_SOURCE")
                .build();

        userService.updateWithoutOtpValidation(incomingUpdate, requestInfo);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).update(captor.capture(), eq(existingDbUser), eq(1L), eq("emp-caller"));

        User persistedUser = captor.getValue();
        assertFalse(persistedUser.getIsIdentityVerified(), "isIdentityVerified must remain false from existing database record");
        assertNull(persistedUser.getIdentityVerifiedDate(), "identityVerifiedDate must remain null");
        assertNull(persistedUser.getIdentityVerifiedSource(), "identityVerifiedSource must remain null");
    }

    @Test
    @DisplayName("updateWithoutOtpValidation should preserve existing isIdentityVerified=true even if caller sends false")
    public void testUpdateWithoutOtpValidation_PreservesVerifiedStatus() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(org.egov.common.contract.request.User.builder()
                        .id(1L)
                        .uuid("emp-caller")
                        .type("EMPLOYEE")
                        .build())
                .build();

        Date verifiedDate = new Date(1700000000000L);
        User existingDbUser = User.builder()
                .id(200L)
                .uuid("user-200")
                .username("emp200")
                .tenantId("pg")
                .type(UserType.EMPLOYEE)
                .roles(Collections.singleton(Role.builder().name("EMPLOYEE").code("EMPLOYEE").build()))
                .isIdentityVerified(true)
                .identityVerifiedDate(verifiedDate)
                .identityVerifiedSource(UserConstants.IDENTITY_SOURCE_AADHAAR)
                .accountLocked(false)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(existingDbUser));

        User incomingUpdate = User.builder()
                .id(200L)
                .uuid("user-200")
                .username("emp200")
                .tenantId("pg")
                .type(UserType.EMPLOYEE)
                .roles(Collections.singleton(Role.builder().name("EMPLOYEE").code("EMPLOYEE").build()))
                .isIdentityVerified(false)
                .identityVerifiedDate(null)
                .identityVerifiedSource(null)
                .build();

        userService.updateWithoutOtpValidation(incomingUpdate, requestInfo);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).update(captor.capture(), eq(existingDbUser), eq(1L), eq("emp-caller"));

        User persistedUser = captor.getValue();
        assertTrue(persistedUser.getIsIdentityVerified(), "isIdentityVerified must remain true from existing record");
        assertEquals(verifiedDate, persistedUser.getIdentityVerifiedDate());
        assertEquals(UserConstants.IDENTITY_SOURCE_AADHAAR, persistedUser.getIdentityVerifiedSource());
    }

    @Test
    @DisplayName("updateUserV2 should preserve existing isIdentityVerified fields from existing database record")
    public void testUpdateUserV2_PreservesIdentityVerificationFields() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(org.egov.common.contract.request.User.builder()
                        .id(300L)
                        .uuid("user-300")
                        .type("CITIZEN")
                        .build())
                .build();

        User existingDbUser = User.builder()
                .id(300L)
                .uuid("user-300")
                .username("9999999999")
                .mobileNumber("9999999999")
                .tenantId("pg")
                .type(UserType.CITIZEN)
                .roles(Collections.singleton(Role.getCitizenRole()))
                .isIdentityVerified(false)
                .identityVerifiedDate(null)
                .identityVerifiedSource(null)
                .accountLocked(false)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(existingDbUser));

        User incomingUpdate = User.builder()
                .id(300L)
                .uuid("user-300")
                .username("9999999999")
                .mobileNumber("9999999999")
                .tenantId("pg")
                .type(UserType.CITIZEN)
                .roles(Collections.singleton(Role.getCitizenRole()))
                .isIdentityVerified(true)
                .identityVerifiedDate(new Date())
                .identityVerifiedSource(UserConstants.IDENTITY_SOURCE_AADHAAR)
                .build();

        userService.updateUserV2(incomingUpdate, requestInfo);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).updateV2(captor.capture(), eq(existingDbUser), eq(300L), eq("user-300"));

        User persistedUser = captor.getValue();
        assertFalse(persistedUser.getIsIdentityVerified(), "updateUserV2 must not update isIdentityVerified from payload");
        assertNull(persistedUser.getIdentityVerifiedDate());
        assertNull(persistedUser.getIdentityVerifiedSource());
    }

    @Test
    @DisplayName("UserRequest.toDomain and UserRequestV2.toDomain should not accept isIdentityVerified from payload")
    public void testContractToDomain_DoesNotPassUntrustedIdentityVerification() {
        UserRequest requestV1 = UserRequest.builder()
                .isIdentityVerified(true)
                .identityVerifiedDate(new Date())
                .identityVerifiedSource(UserConstants.IDENTITY_SOURCE_AADHAAR)
                .build();

        User domainCreateV1 = requestV1.toDomain(1L, "uuid", true);
        assertFalse(domainCreateV1.getIsIdentityVerified(), "toDomain(true) must set isIdentityVerified=false");
        assertNull(domainCreateV1.getIdentityVerifiedDate(), "toDomain(true) must nullify identityVerifiedDate");
        assertNull(domainCreateV1.getIdentityVerifiedSource(), "toDomain(true) must nullify identityVerifiedSource");

        User domainUpdateV1 = requestV1.toDomain(1L, "uuid", false);
        assertNull(domainUpdateV1.getIsIdentityVerified(), "toDomain(false) must not accept isIdentityVerified");
        assertNull(domainUpdateV1.getIdentityVerifiedDate(), "toDomain(false) must not accept identityVerifiedDate");
        assertNull(domainUpdateV1.getIdentityVerifiedSource(), "toDomain(false) must not accept identityVerifiedSource");

        UserRequestV2 requestV2 = UserRequestV2.builder()
                .isIdentityVerified(true)
                .identityVerifiedDate(new Date())
                .identityVerifiedSource(UserConstants.IDENTITY_SOURCE_AADHAAR)
                .build();

        User domainCreateV2 = requestV2.toDomain(1L, "uuid", true);
        assertFalse(domainCreateV2.getIsIdentityVerified(), "toDomain(true) in V2 must set isIdentityVerified=false");
        assertNull(domainCreateV2.getIdentityVerifiedDate(), "toDomain(true) in V2 must nullify identityVerifiedDate");
        assertNull(domainCreateV2.getIdentityVerifiedSource(), "toDomain(true) in V2 must nullify identityVerifiedSource");

        User domainUpdateV2 = requestV2.toDomain(1L, "uuid", false);
        assertNull(domainUpdateV2.getIsIdentityVerified(), "toDomain(false) in V2 must not accept isIdentityVerified");
        assertNull(domainUpdateV2.getIdentityVerifiedDate(), "toDomain(false) in V2 must not accept identityVerifiedDate");
        assertNull(domainUpdateV2.getIdentityVerifiedSource(), "toDomain(false) in V2 must not accept identityVerifiedSource");
    }
}
