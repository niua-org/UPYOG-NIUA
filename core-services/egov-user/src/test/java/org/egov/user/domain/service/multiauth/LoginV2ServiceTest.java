package org.egov.user.domain.service.multiauth;

import org.egov.common.contract.request.RequestInfo;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.enums.Gender;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.multiauth.strategy.AuthenticatedUserResult;
import org.egov.user.domain.service.multiauth.strategy.MultiAuthStrategy;
import org.egov.user.domain.service.multiauth.validator.LoginV2Validator;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.multiauth.AuthType;
import org.egov.user.web.contract.multiauth.LoginV2Request;
import org.egov.user.web.contract.multiauth.LoginV2Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LoginV2Service} verifying multi-auth orchestration
 * and multi-user profile selection handling.
 */
@ExtendWith(MockitoExtension.class)
public class LoginV2ServiceTest {

    @Mock
    private MultiAuthStrategyFactory strategyFactory;

    @Mock
    private TokenGenerationService tokenGenerationService;

    @Mock
    private LoginV2Validator validator;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MultiAuthStrategy mobileStrategy;

    private LoginV2Service loginV2Service;

    @BeforeEach
    public void setUp() {
        loginV2Service = new LoginV2Service(strategyFactory, tokenGenerationService, validator, userRepository);
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
                .tenantId("pb")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should return 200 with multipleUsersFound=true and user accounts without minting tokens when multiple profiles exist")
    public void testLogin_WhenMultipleUsersFound_ReturnsAccountsForSelection() {
        LoginV2Request request = LoginV2Request.builder()
                .mobileNumber("9876543210")
                .otp("123456")
                .tenantId("pb.amritsar")
                .requestInfo(new RequestInfo())
                .build();

        User user1 = buildUser(101L, "uuid-1", "9876543210", "9876543210", "User One");
        User user2 = buildUser(102L, "uuid-2", "9876543210_sub", "9876543210", "User Two");
        List<User> matchedUsers = Arrays.asList(user1, user2);

        AuthenticatedUserResult multiUserResult = AuthenticatedUserResult.builder()
                .multipleUsersFound(true)
                .matchedUsers(matchedUsers)
                .authType(AuthType.MOBILE_NUMBER)
                .build();

        when(strategyFactory.getStrategy(request)).thenReturn(mobileStrategy);
        when(mobileStrategy.authenticateAndResolveUser(request)).thenReturn(multiUserResult);

        LoginV2Response response = loginV2Service.login(request);

        assertNotNull(response);
        assertTrue(response.getMultipleUsersFound());
        assertNotNull(response.getUserAccounts());
        assertEquals(2, response.getUserAccounts().size());
        assertEquals("uuid-1", response.getUserAccounts().get(0).getUuid());
        assertEquals("uuid-2", response.getUserAccounts().get(1).getUuid());
        assertNull(response.getAccessToken()); // Tokens must NOT be minted prior to profile selection
        verify(tokenGenerationService, never()).generateTokensAndResponse(any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("Should mint tokens and persist lastLoginMethod when single user is authenticated")
    public void testLogin_WhenSingleUser_MintsTokensSuccessfully() {
        LoginV2Request request = LoginV2Request.builder()
                .mobileNumber("9876543210")
                .otp("123456")
                .tenantId("pb.amritsar")
                .requestInfo(new RequestInfo())
                .build();

        User user = buildUser(101L, "uuid-1", "9876543210", "9876543210", "User One");

        AuthenticatedUserResult singleUserResult = AuthenticatedUserResult.builder()
                .user(user)
                .isNewUser(false)
                .authType(AuthType.MOBILE_NUMBER)
                .build();

        LoginV2Response expectedResponse = LoginV2Response.builder()
                .accessToken("mock-access-token")
                .refreshToken("mock-refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600)
                .isNewUser(false)
                .authenticatedVia(AuthType.MOBILE_NUMBER)
                .build();

        when(strategyFactory.getStrategy(request)).thenReturn(mobileStrategy);
        when(mobileStrategy.authenticateAndResolveUser(request)).thenReturn(singleUserResult);
        when(tokenGenerationService.generateTokensAndResponse(user, request.getScope(), AuthType.MOBILE_NUMBER, false))
                .thenReturn(expectedResponse);

        LoginV2Response response = loginV2Service.login(request);

        assertNotNull(response);
        assertEquals("mock-access-token", response.getAccessToken());
        assertEquals("mock-refresh-token", response.getRefreshToken());
        assertEquals(AuthType.MOBILE_NUMBER, response.getAuthenticatedVia());
        verify(userRepository, times(1)).updateLastLoginMethod(101L, "MOBILE_NUMBER");
    }
}
