package org.egov.user.domain.service.multiauth.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.config.UserConfiguration;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.enums.Gender;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.UserService;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.multiauth.AuthType;
import org.egov.user.web.contract.multiauth.LoginV2Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AadhaarAuthStrategy} verifying identity verification and inactive account rejection.
 */
@ExtendWith(MockitoExtension.class)
public class AadhaarAuthStrategyTest {

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private org.egov.user.persistence.repository.FamilyRestRepository familyRestRepository;

    private ObjectMapper objectMapper;
    private UserConfiguration userConfiguration;
    private AadhaarAuthStrategy aadhaarAuthStrategy;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        userConfiguration = new UserConfiguration();
        userConfiguration.setCitizenLoginPasswordOtpFixedEnabled(true);
        userConfiguration.setCitizenLoginPasswordOtpFixedValue("123456");
        userConfiguration.setRolesStateLevelEnabled(true);

        aadhaarAuthStrategy = new AadhaarAuthStrategy(
                userService, userRepository, restTemplate, objectMapper, null, userConfiguration, familyRestRepository
        );
    }

    private User buildUser(Long id, String uuid, String username, String mobile, String name, boolean active) {
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
                .active(active)
                .build();
    }

    @Test
    @DisplayName("Should throw ACCOUNT_INACTIVE when matched Aadhaar citizen profile is inactive")
    public void testLogin_WhenMatchedAadhaarUserIsInactive_ThrowsAccountInactive() {
        LoginV2Request request = LoginV2Request.builder()
                .authType(AuthType.AADHAAR_NUMBER)
                .aadhaarNumber("123456789012")
                .otp("123456")
                .tenantId("pg.citya")
                .requestInfo(new RequestInfo())
                .build();

        User inactiveCitizen = buildUser(201L, "uuid-inactive", "9999999999", "9999999999", "Aadhaar User", false);
        inactiveCitizen.setAadhaarNumber("123456789012");

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(inactiveCitizen));

        CustomException ex = assertThrows(CustomException.class, () -> aadhaarAuthStrategy.authenticateAndResolveUser(request));
        assertEquals(UserConstants.ERROR_ACCOUNT_INACTIVE, ex.getCode());
        assertEquals(UserConstants.MSG_ACCOUNT_INACTIVE, ex.getMessage());
    }

    @Test
    @DisplayName("Should login successfully when matched Aadhaar citizen profile is active")
    public void testLogin_WhenMatchedAadhaarUserIsActive_Succeeds() {
        LoginV2Request request = LoginV2Request.builder()
                .authType(AuthType.AADHAAR_NUMBER)
                .aadhaarNumber("123456789012")
                .otp("123456")
                .tenantId("pg.citya")
                .requestInfo(new RequestInfo())
                .build();

        User activeCitizen = buildUser(201L, "uuid-active", "9999999999", "9999999999", "Aadhaar User", true);
        activeCitizen.setAadhaarNumber("123456789012");

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(activeCitizen));
        when(familyRestRepository.getActiveFamilyIdByUserUuid(eq("uuid-active"), any(), any())).thenReturn("FAM-AADHAAR-999");

        AuthenticatedUserResult result = aadhaarAuthStrategy.authenticateAndResolveUser(request);
        assertNotNull(result);
        assertFalse(result.isNewUser());
        assertEquals("uuid-active", result.getUser().getUuid());
        assertEquals(Boolean.TRUE, result.getUser().getIsIdentityVerified());
        assertEquals("FAM-AADHAAR-999", result.getUser().getFamilyId());
    }

    @Test
    @DisplayName("Should throw FIXED_OTP_NOT_CONFIGURED when fixed OTP is enabled but value is missing")
    public void testLogin_WhenFixedOtpEnabledButValueMissing_ThrowsException() {
        userConfiguration.setCitizenLoginPasswordOtpFixedValue(null);
        LoginV2Request request = LoginV2Request.builder()
                .authType(AuthType.AADHAAR_NUMBER)
                .aadhaarNumber("123456789012")
                .otp("123456")
                .tenantId("pg.citya")
                .requestInfo(new RequestInfo())
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> aadhaarAuthStrategy.authenticateAndResolveUser(request));
        assertEquals(UserConstants.ERROR_FIXED_OTP_NOT_CONFIGURED, ex.getCode());
    }

    @Test
    @DisplayName("Should throw INVALID_REQUEST when requester service host is missing for live OTP")
    public void testLogin_WhenRequesterHostMissing_ThrowsException() {
        userConfiguration.setCitizenLoginPasswordOtpFixedEnabled(false);
        userConfiguration.setRequesterServiceHost(null);
        LoginV2Request request = LoginV2Request.builder()
                .authType(AuthType.AADHAAR_NUMBER)
                .aadhaarNumber("123456789012")
                .otp("654321")
                .tenantId("pg.citya")
                .requestInfo(new RequestInfo())
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> aadhaarAuthStrategy.authenticateAndResolveUser(request));
        assertEquals(UserConstants.ERROR_INVALID_REQUEST, ex.getCode());
        assertEquals("requester.service.host is not configured", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw INVALID_REQUEST when requester verify endpoint is missing for live OTP")
    public void testLogin_WhenRequesterEndpointMissing_ThrowsException() {
        userConfiguration.setCitizenLoginPasswordOtpFixedEnabled(false);
        userConfiguration.setRequesterServiceHost("http://localhost:8280/");
        userConfiguration.setRequesterAadhaarVerifyEndpoint(null);
        LoginV2Request request = LoginV2Request.builder()
                .authType(AuthType.AADHAAR_NUMBER)
                .aadhaarNumber("123456789012")
                .otp("654321")
                .tenantId("pg.citya")
                .requestInfo(new RequestInfo())
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> aadhaarAuthStrategy.authenticateAndResolveUser(request));
        assertEquals(UserConstants.ERROR_INVALID_REQUEST, ex.getCode());
        assertEquals("requester.aadhaar.verify.endpoint is not configured", ex.getMessage());
    }
}
