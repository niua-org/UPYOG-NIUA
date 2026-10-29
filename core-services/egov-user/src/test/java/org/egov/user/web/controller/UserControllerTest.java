package org.egov.user.web.controller;

import org.egov.common.contract.request.RequestInfo;
import org.egov.user.domain.exception.DuplicateUserNameException;
import org.egov.user.domain.exception.UserNotFoundException;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.TokenService;
import org.egov.user.domain.service.UserService;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.SecureUser;
import org.egov.user.domain.model.UserDetail;
import org.egov.user.web.contract.AddressRequest;
import org.egov.user.web.contract.AddressResponse;
import org.egov.user.web.contract.CreateUserRequest;
import org.egov.user.web.contract.CreateUserRequestV2;
import org.egov.user.web.contract.RoleRequest;
import org.egov.user.domain.model.UpdateResponse;
import org.egov.user.web.contract.UserDetailResponse;
import org.egov.user.web.contract.UserDetailResponseV2;
import org.egov.user.web.contract.UserRequest;
import org.egov.user.web.contract.UserRequestV2;
import org.egov.user.web.contract.UserSearchRequest;
import org.egov.user.web.contract.UserSearchRequestV2;
import org.egov.user.web.contract.UserSearchResponse;
import org.egov.user.web.contract.auth.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private TokenService tokenService;

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController(userService, tokenService);
        ReflectionTestUtils.setField(controller, "IsValidationMandatory", true);
        ReflectionTestUtils.setField(controller, "isRegWithLoginEnabled", false);
        ReflectionTestUtils.setField(controller, "defaultSearchSize", 100);
        ReflectionTestUtils.setField(controller, "mobileValidationWorkaroundEnabled", "true");
    }

    @Test
    void createCitizen_loginDisabled_returnsUserDetailResponse() {
        RequestInfo requestInfo = RequestInfo.builder().build();
        User persisted = User.builder()
                .username("9999999999")
                .name("Citizen")
                .uuid("uuid-1")
                .active(true)
                .type(UserType.CITIZEN)
                .tenantId("pb")
                .build();
        when(userService.createCitizen(any(User.class), eq(requestInfo))).thenReturn(persisted);

        Object response = controller.createCitizen(createRequest(requestInfo));

        assertThat(response).isInstanceOf(UserDetailResponse.class);
        UserDetailResponse detail = (UserDetailResponse) response;
        assertThat(detail.getUser()).hasSize(1);
        assertThat(detail.getUser().get(0).getUserName()).isEqualTo("9999999999");
        assertThat(detail.getResponseInfo().getStatus()).isEqualTo("200");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).createCitizen(userCaptor.capture(), eq(requestInfo));
        assertThat(userCaptor.getValue().isOtpValidationMandatory()).isTrue();
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("9999999999");
    }

    @Test
    void createCitizen_loginEnabled_returnsOkResponseEntity() {
        ReflectionTestUtils.setField(controller, "isRegWithLoginEnabled", true);
        RequestInfo requestInfo = RequestInfo.builder().build();
        Object tokenBody = Collections.singletonMap("access_token", "abc");
        when(userService.registerWithLogin(any(User.class), eq(requestInfo))).thenReturn(tokenBody);

        Object response = controller.createCitizen(createRequest(requestInfo));

        assertThat(response).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> entity = (ResponseEntity<?>) response;
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody()).isEqualTo(tokenBody);
    }

    @Test
    void createUserWithoutValidation_gatewayCall_delegatesToCreateUser() {
        RequestInfo requestInfo = RequestInfo.builder().build();
        User persisted = User.builder().username("alice").uuid("uuid-2").active(true).type(UserType.EMPLOYEE).build();
        when(userService.createUser(any(User.class), eq(requestInfo))).thenReturn(persisted);

        UserDetailResponse response = controller.createUserWithoutValidation(createRequest(requestInfo), gatewayHeaders());

        assertThat(response.getUser()).extracting(UserRequest::getUserName).containsExactly("alice");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).createUser(userCaptor.capture(), eq(requestInfo));
        assertThat(userCaptor.getValue().isOtpValidationMandatory()).isFalse();
        assertThat(userCaptor.getValue().isMobileValidationMandatory()).isTrue();
    }

    @Test
    void createUserWithoutValidation_interServiceWorkaround_disablesMobileValidation() {
        RequestInfo requestInfo = RequestInfo.builder().build();
        when(userService.createUser(any(User.class), eq(requestInfo)))
                .thenReturn(User.builder().username("alice").active(true).build());

        controller.createUserWithoutValidation(createRequest(requestInfo), new HttpHeaders());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).createUser(userCaptor.capture(), eq(requestInfo));
        assertThat(userCaptor.getValue().isMobileValidationMandatory()).isFalse();
        assertThat(userCaptor.getValue().isOtpValidationMandatory()).isFalse();
    }

    @Test
    void createUserWithoutValidation_duplicateUser_propagatesDuplicateUserNameException() {
        when(userService.createUser(any(User.class), any()))
                .thenThrow(new DuplicateUserNameException(UserSearchCriteria.builder().userName("alice").build()));

        assertThatThrownBy(() -> controller.createUserWithoutValidation(createRequest(RequestInfo.builder().build()),
                gatewayHeaders()))
                .isInstanceOf(DuplicateUserNameException.class);
    }

    @Test
    void get_nullActive_setsActiveTrueAndReturnsUsers() {
        UserSearchRequest request = searchRequest(null, 10);
        User found = User.builder().id(7L).username("alice").uuid("uuid-7").active(true).type(UserType.CITIZEN)
                .tenantId("pb").build();
        when(userService.searchUsers(any(), eq(false), eq(request.getRequestInfo())))
                .thenReturn(Collections.singletonList(found));

        UserSearchResponse response = controller.get(request, gatewayHeaders());

        assertThat(request.getActive()).isTrue();
        assertThat(response.getUserSearchResponseContent()).hasSize(1);
        assertThat(response.getUserSearchResponseContent().get(0).getUserName()).isEqualTo("alice");
        assertThat(response.getResponseInfo().getStatus()).isEqualTo("200");
        ArgumentCaptor<UserSearchCriteria> criteria = ArgumentCaptor.forClass(UserSearchCriteria.class);
        verify(userService).searchUsers(criteria.capture(), eq(false), eq(request.getRequestInfo()));
        assertThat(criteria.getValue().getActive()).isTrue();
        assertThat(criteria.getValue().getLimit()).isEqualTo(10);
    }

    @Test
    void get_zeroPageSizeFromGateway_appliesDefaultLimit() {
        UserSearchRequest request = searchRequest(true, 0);
        when(userService.searchUsers(any(), anyBoolean(), any())).thenReturn(Collections.emptyList());

        UserSearchResponse response = controller.get(request, gatewayHeaders());

        assertThat(response.getUserSearchResponseContent()).isEmpty();
        ArgumentCaptor<UserSearchCriteria> criteria = ArgumentCaptor.forClass(UserSearchCriteria.class);
        verify(userService).searchUsers(criteria.capture(), eq(false), any());
        assertThat(criteria.getValue().getLimit()).isEqualTo(100);
    }

    @Test
    void getV1_nullActive_keepsActiveUnset() {
        UserSearchRequest request = searchRequest(null, 5);
        when(userService.searchUsers(any(), anyBoolean(), any())).thenReturn(Collections.emptyList());

        controller.getV1(request, gatewayHeaders());

        assertThat(request.getActive()).isNull();
        ArgumentCaptor<UserSearchCriteria> criteria = ArgumentCaptor.forClass(UserSearchCriteria.class);
        verify(userService).searchUsers(criteria.capture(), eq(false), any());
        assertThat(criteria.getValue().getActive()).isNull();
    }

    @Test
    void get_serviceThrows_propagatesUserNotFoundException() {
        UserSearchRequest request = searchRequest(true, 10);
        when(userService.searchUsers(any(), anyBoolean(), any()))
                .thenThrow(new UserNotFoundException(UserSearchCriteria.builder().userName("alice").build()));

        assertThatThrownBy(() -> controller.get(request, gatewayHeaders()))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getUser_mapsSecureUserToCommonContract() {
        org.egov.user.web.contract.auth.User authUser = org.egov.user.web.contract.auth.User.builder()
                .id(3L)
                .uuid("uuid-3")
                .userName("alice")
                .name("Alice")
                .tenantId("pb")
                .roles(Set.of(new Role("Citizen", "CITIZEN", "pb")))
                .build();
        SecureUser secureUser = new SecureUser(authUser);
        when(tokenService.getUser("token-abc")).thenReturn(new UserDetail(secureUser, null));

        org.egov.common.contract.request.User commonUser = controller.getUser("token-abc");

        assertThat(commonUser.getUuid()).isEqualTo("uuid-3");
        assertThat(commonUser.getRoles()).hasSize(1);
        assertThat(commonUser.getRoles().get(0).getCode()).isEqualTo("CITIZEN");
    }

    @Test
    void patch_delegatesToPartialUpdate() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(org.egov.common.contract.request.User.builder().id(1L).uuid("uuid-1").build())
                .build();
        User persisted = User.builder().uuid("uuid-1").username("alice").active(true).type(UserType.EMPLOYEE).build();
        when(userService.partialUpdate(any(User.class), eq(requestInfo))).thenReturn(persisted);

        UserRequest profileRequest = userRequest("alice");
        profileRequest.setUuid("uuid-1");
        profileRequest.setId(1L);
        UpdateResponse response = controller.patch(new CreateUserRequest(requestInfo, profileRequest));

        assertThat(response.getUser()).hasSize(1);
        verify(userService).partialUpdate(any(User.class), eq(requestInfo));
    }

    @Test
    void updateUserWithoutValidation_delegatesToService() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(org.egov.common.contract.request.User.builder().id(1L).uuid("uuid-1").build())
                .build();
        User updated = User.builder().uuid("uuid-1").username("alice").active(true).type(UserType.EMPLOYEE).build();
        when(userService.updateWithoutOtpValidation(any(User.class), eq(requestInfo))).thenReturn(updated);

        UpdateResponse response = controller.updateUserWithoutValidation(
                new CreateUserRequest(requestInfo, userRequest("alice")), gatewayHeaders());

        assertThat(response.getUser()).hasSize(1);
        verify(userService).updateWithoutOtpValidation(any(User.class), eq(requestInfo));
    }

    @Test
    void createAddress_returnsCreatedResponse() {
        Address saved = Address.builder().city("City").pinCode("123456").address("Line 1").build();
        when(userService.createAddress(eq("uuid-1"), any(Address.class))).thenReturn(saved);

        ResponseEntity<AddressResponse> response = controller.createAddress(
                new AddressRequest(null, Address.builder().city("City").pinCode("123456").address("Line 1").build(),
                        "uuid-1", null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getAddress()).containsExactly(saved);
    }

    @Test
    void getAddress_returnsAddressList() {
        Address stored = Address.builder().city("City").build();
        when(userService.getAddress("uuid-1", "pb")).thenReturn(Collections.singletonList(stored));

        AddressResponse response = controller.getAddress("uuid-1", "pb");

        assertThat(response.getAddress()).containsExactly(stored);
    }

    @Test
    void createUserWithAddressV2_delegatesToService() {
        RequestInfo requestInfo = RequestInfo.builder().build();
        User persisted = User.builder().uuid("uuid-v2").username("alice").active(true).type(UserType.EMPLOYEE).build();
        when(userService.createUserWithAddressV2(any(User.class), eq(requestInfo))).thenReturn(persisted);

        Object response = controller.createUserWithAddress(
                new CreateUserRequestV2(requestInfo, userRequestV2("alice")), gatewayHeaders());

        assertThat(response).isInstanceOf(UserDetailResponseV2.class);
        verify(userService).createUserWithAddressV2(any(User.class), eq(requestInfo));
    }

    @Test
    void getUsersV2_capsPageSizeForExternalCalls() {
        UserSearchRequestV2 request = new UserSearchRequestV2();
        request.setRequestInfo(RequestInfo.builder().build());
        request.setUserName("alice");
        request.setTenantId("pb");
        request.setPageSize(0);
        when(userService.searchUsersV2(any(), eq(false), any())).thenReturn(Collections.emptyList());

        UserSearchResponse response = controller.getUsersV2(request, gatewayHeaders());

        assertThat(response.getUserSearchResponseContent()).isEmpty();
        ArgumentCaptor<UserSearchCriteria> criteria = ArgumentCaptor.forClass(UserSearchCriteria.class);
        verify(userService).searchUsersV2(criteria.capture(), eq(false), any());
        assertThat(criteria.getValue().getLimit()).isEqualTo(100);
    }

    private CreateUserRequest createRequest(RequestInfo requestInfo) {
        return new CreateUserRequest(requestInfo, userRequest("9999999999"));
    }

    private UserRequest userRequest(String userName) {
        return UserRequest.builder()
                .userName(userName)
                .name("Citizen")
                .mobileNumber("9999999999")
                .tenantId("pb")
                .type(UserType.CITIZEN)
                .active(true)
                .roles(Set.of(RoleRequest.builder().code("CITIZEN").build()))
                .build();
    }

    private UserRequestV2 userRequestV2(String userName) {
        return UserRequestV2.builder()
                .userName(userName)
                .name("Citizen")
                .mobileNumber("9999999999")
                .tenantId("pb")
                .type(UserType.EMPLOYEE)
                .active(true)
                .roles(Set.of(RoleRequest.builder().code("EMPLOYEE").build()))
                .build();
    }

    private UserSearchRequest searchRequest(Boolean active, int pageSize) {
        UserSearchRequest request = new UserSearchRequest();
        request.setRequestInfo(RequestInfo.builder().build());
        request.setUserName("alice");
        request.setTenantId("pb");
        request.setUserType("CITIZEN");
        request.setPageSize(pageSize);
        request.setActive(active);
        return request;
    }

    private HttpHeaders gatewayHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("x-pass-through-gateway", "true");
        return headers;
    }
}
