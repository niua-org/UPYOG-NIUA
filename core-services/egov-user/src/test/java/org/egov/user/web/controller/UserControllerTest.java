package org.egov.user.web.controller;

import org.egov.common.contract.request.RequestInfo;
import org.egov.user.domain.exception.DuplicateUserNameException;
import org.egov.user.domain.exception.UserNotFoundException;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.TokenService;
import org.egov.user.domain.service.UserService;
import org.egov.user.web.contract.CreateUserRequest;
import org.egov.user.web.contract.RoleRequest;
import org.egov.user.web.contract.UserDetailResponse;
import org.egov.user.web.contract.UserRequest;
import org.egov.user.web.contract.UserSearchRequest;
import org.egov.user.web.contract.UserSearchResponse;
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

    private CreateUserRequest createRequest(RequestInfo requestInfo) {
        UserRequest userRequest = UserRequest.builder()
                .userName("9999999999")
                .name("Citizen")
                .mobileNumber("9999999999")
                .tenantId("pb")
                .type(UserType.CITIZEN)
                .active(true)
                .roles(Set.of(RoleRequest.builder().code("CITIZEN").build()))
                .build();
        return new CreateUserRequest(requestInfo, userRequest);
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
