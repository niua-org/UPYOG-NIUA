package org.egov.user.domain.service.multiauth.strategy;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.config.UserConfiguration;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.enums.Gender;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.UserService;
import org.egov.user.domain.service.utils.EncryptionDecryptionUtil;
import org.egov.user.persistence.repository.OtpRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.multiauth.AuthType;
import org.egov.user.web.contract.multiauth.LoginV2Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link FamilyIdAuthStrategy} verifying family authentication flows
 * when logging in with either Family ID alone or with Member Mobile Number.
 */
@ExtendWith(MockitoExtension.class)
public class FamilyIdAuthStrategyTest {

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private EncryptionDecryptionUtil encryptionDecryptionUtil;

    private UserConfiguration userConfiguration;
    private FamilyIdAuthStrategy familyIdAuthStrategy;

    @BeforeEach
    public void setUp() {
        userConfiguration = new UserConfiguration();
        userConfiguration.setCitizenLoginPasswordOtpFixedEnabled(true);
        userConfiguration.setCitizenLoginPasswordOtpFixedValue("123456");
        userConfiguration.setRolesStateLevelEnabled(true);

        familyIdAuthStrategy = new FamilyIdAuthStrategy(
                userService, userRepository, otpRepository, encryptionDecryptionUtil, userConfiguration
        );
    }

    private User buildUser(Long id, String uuid, String username, String mobile, String name) {
        return User.builder()
                .id(id)
                .uuid(uuid)
                .username(username)
                .mobileNumber(mobile)
                .name(name)
                .gender(Gender.MALE)
                .dob(new Date())
                .type(UserType.CITIZEN)
                .tenantId("pg")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should login successfully using only familyId and OTP without mobileNumber")
    public void testLogin_WithFamilyIdOnly_Success() {
        LoginV2Request request = LoginV2Request.builder()
                .authType(AuthType.FAMILY_ID)
                .familyId("HR-PPP-998877")
                .otp("123456")
                .tenantId("pg.citya")
                .requestInfo(new RequestInfo())
                .build();

        User existingUser = buildUser(301L, "uuid-fam-head", "HR-PPP-998877", null, "Family Head");
        when(userRepository.findAll(any())).thenReturn(Collections.singletonList(existingUser));
        when(encryptionDecryptionUtil.decryptObject(any(), any(), any(), any())).thenReturn(existingUser);

        AuthenticatedUserResult result = familyIdAuthStrategy.authenticateAndResolveUser(request);

        assertNotNull(result);
        assertFalse(result.isNewUser());
        assertEquals(AuthType.FAMILY_ID, result.getAuthType());
        assertEquals("uuid-fam-head", result.getUser().getUuid());
        assertEquals("FAMILY_ID", result.getUser().getLastLoginMethod());
    }

    @Test
    @DisplayName("Should auto-provision user when familyId user is not found")
    public void testLogin_WithFamilyIdOnly_AutoProvision() {
        LoginV2Request request = LoginV2Request.builder()
                .authType(AuthType.FAMILY_ID)
                .familyId("HR-PPP-NEW")
                .otp("123456")
                .tenantId("pg.citya")
                .name("Kavita Sharma")
                .requestInfo(new RequestInfo())
                .build();

        when(userRepository.findAll(any())).thenReturn(Collections.emptyList());

        User newUser = buildUser(401L, "uuid-new-head", "HR-PPP-NEW", null, "Kavita Sharma");
        when(userService.createUser(any(), any())).thenReturn(newUser);

        AuthenticatedUserResult result = familyIdAuthStrategy.authenticateAndResolveUser(request);

        assertNotNull(result);
        assertTrue(result.isNewUser());
        assertEquals(AuthType.FAMILY_ID, result.getAuthType());
        assertEquals("uuid-new-head", result.getUser().getUuid());
        verify(userService, times(1)).createUser(any(), any());
    }

    @Test
    @DisplayName("Should login successfully using familyId and mobileNumber when both provided")
    public void testLogin_WithFamilyIdAndMobileNumber_Success() {
        LoginV2Request request = LoginV2Request.builder()
                .authType(AuthType.FAMILY_ID)
                .familyId("HR-PPP-998877")
                .mobileNumber("9876543210")
                .otp("123456")
                .tenantId("pg.citya")
                .requestInfo(new RequestInfo())
                .build();

        User existingUser = buildUser(501L, "uuid-member", "9876543210", "9876543210", "Member One");
        when(userRepository.findAll(any())).thenReturn(Collections.singletonList(existingUser));
        when(encryptionDecryptionUtil.decryptObject(any(), any(), any(), any())).thenReturn(existingUser);

        AuthenticatedUserResult result = familyIdAuthStrategy.authenticateAndResolveUser(request);

        assertNotNull(result);
        assertFalse(result.isNewUser());
        assertEquals(AuthType.FAMILY_ID, result.getAuthType());
        assertEquals("uuid-member", result.getUser().getUuid());
    }

    @Test
    @DisplayName("Should throw CustomException when familyId is blank")
    public void testLogin_WhenFamilyIdIsBlank_ThrowsException() {
        LoginV2Request request = LoginV2Request.builder()
                .authType(AuthType.FAMILY_ID)
                .otp("123456")
                .tenantId("pg.citya")
                .build();

        assertThrows(CustomException.class, () -> familyIdAuthStrategy.authenticateAndResolveUser(request));
    }

    @Test
    @DisplayName("Should throw CustomException when OTP is blank")
    public void testLogin_WhenOtpIsBlank_ThrowsException() {
        LoginV2Request request = LoginV2Request.builder()
                .authType(AuthType.FAMILY_ID)
                .familyId("HR-PPP-123")
                .tenantId("pg.citya")
                .build();

        assertThrows(CustomException.class, () -> familyIdAuthStrategy.authenticateAndResolveUser(request));
    }
}
