package org.egov.user.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.config.UserConfiguration;
import org.egov.user.domain.model.*;

import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserDetail;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.DigiLockerService;
import org.egov.user.domain.service.TokenService;
import org.egov.user.domain.service.UserService;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.web.contract.UserDetailResponseV2;
import org.egov.user.web.contract.*;
import org.egov.user.web.contract.auth.CustomUserDetails;
import org.json.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Date;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.egov.tracer.http.HttpUtils.isInterServiceCall;
import static org.springframework.util.CollectionUtils.isEmpty;

@Tag(name = "User Management", description = "Core citizen and employee lifecycle management endpoints")
@RestController
@Slf4j
public class UserController {

    private UserService userService;
    private TokenService tokenService;
    private DigiLockerService digiLockerService;
    private UserConfiguration userConfiguration;

    @Autowired
    public UserController(UserService userService, TokenService tokenService,
                          @Autowired(required = false) DigiLockerService digiLockerService,
                          @Autowired(required = false) UserConfiguration userConfiguration) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.digiLockerService = digiLockerService;
        this.userConfiguration = userConfiguration;
    }

    public UserController(UserService userService, TokenService tokenService) {
        this(userService, tokenService, null, null);
    }

    private boolean isValidationMandatory() {
        return userConfiguration != null && Boolean.TRUE.equals(userConfiguration.getOtpValidationRegisterMandatory());
    }

    private boolean isRegWithLoginEnabled() {
        return userConfiguration != null && Boolean.TRUE.equals(userConfiguration.getCitizenRegistrationWithLoginEnabled());
    }

    private int getDefaultSearchSize() {
        return userConfiguration != null && userConfiguration.getDefaultSearchSize() != null
                ? userConfiguration.getDefaultSearchSize() : 10;
    }

    private String getMobileValidationWorkaroundEnabled() {
        return userConfiguration != null && userConfiguration.getMobileValidationWorkaroundEnabled() != null
                ? userConfiguration.getMobileValidationWorkaroundEnabled() : "false";
    }

    /**
     * end-point to create the citizen with otp.Here otp is mandatory to create
     * citizen.
     *
     * @param createUserRequest
     * @return
     */
    @PostMapping("/citizen/_create")
    public Object createCitizen(@RequestBody @Valid CreateUserRequest createUserRequest) {
        log.info("Received Citizen Registration Request  " + createUserRequest);
        User user = createUserRequest.toDomain(true);
        user.setOtpValidationMandatory(isValidationMandatory());
        if (isRegWithLoginEnabled()) {
            Object object = userService.registerWithLogin(user, createUserRequest.getRequestInfo());
            return new ResponseEntity<>(object, HttpStatus.OK);
        }
        User createdUser = userService.createCitizen(user, createUserRequest.getRequestInfo());
        return createResponse(createdUser);
    }

    /**
     * end-point to create the user without otp validation.
     *
     * @param createUserRequest
     * @param headers
     * @return
     */
    @PostMapping("/users/_createnovalidate")
    public UserDetailResponse createUserWithoutValidation(@RequestBody @Valid CreateUserRequest createUserRequest,
                                                          @RequestHeader HttpHeaders headers) {

        User user = createUserRequest.toDomain(true);
        user.setMobileValidationMandatory(isMobileValidationRequired(headers));
        user.setOtpValidationMandatory(false);
        final User newUser = userService.createUser(user, createUserRequest.getRequestInfo());
        return createResponse(newUser);
    }

    /**
     * end-point to search the users by providing userSearchRequest. In Request
     * if there is no active filed value, it will fetch only active users
     *
     * @param request
     * @return
     */
    @PostMapping("/_search")
    public UserSearchResponse get(@RequestBody @Valid UserSearchRequest request, @RequestHeader HttpHeaders headers) {

        log.info("Received User search Request  " + request);
        if (request.getActive() == null) {
            request.setActive(true);
        }
        return searchUsers(request, headers);
    }

    /**
     * end-point to search the users by providing userSearchRequest. In Request
     * if there is no active filed value, it will fetch all(active & inactive)
     * users.
     *
     * @param request
     * @return
     */
    @PostMapping("/v1/_search")
    public UserSearchResponse getV1(@RequestBody UserSearchRequest request, @RequestHeader HttpHeaders headers) {
        return searchUsers(request, headers);
    }

    /**
     * end-point to fetch the user details by access-token
     *
     * @param accessToken
     * @return
     */
    @PostMapping("/_details")
    public org.egov.common.contract.request.User getUser(@RequestParam(value = "access_token") String accessToken) {
        log.info("/_details endpoint called with token: {}...", accessToken.substring(0, Math.min(8, accessToken.length())));

        final UserDetail userDetail = tokenService.getUser(accessToken);
        log.info("TokenService returned userDetail: {}", userDetail != null ? "not null" : "NULL");

        // CRITICAL FIX: Return common contract User instead of CustomUserDetails for proper gateway deserialization
        org.egov.user.web.contract.auth.User authUser = userDetail.getSecureUser().getUser();
        log.info("AuthUser extracted from userDetail - id: {}, uuid: {}, userName: {}",
            authUser.getId(), authUser.getUuid(), authUser.getUserName());
        log.info("AuthUser roles: {} (size: {})",
            authUser.getRoles(), authUser.getRoles() != null ? authUser.getRoles().size() : "NULL");

        if (authUser.getRoles() != null && !authUser.getRoles().isEmpty()) {
            authUser.getRoles().forEach(role ->
                log.info("  Role - code: {}, name: {}, tenantId: {}",
                    role.getCode(), role.getName(), role.getTenantId())
            );
        }

        org.egov.common.contract.request.User commonUser = org.egov.common.contract.request.User.builder()
            .id(authUser.getId())
            .uuid(authUser.getUuid())
            .userName(authUser.getUserName())
            .name(authUser.getName())
            .mobileNumber(authUser.getMobileNumber())
            .emailId(authUser.getEmailId())
            .type(authUser.getType())
            .tenantId(authUser.getTenantId())
            .roles(authUser.getRoles() != null ?
                authUser.getRoles().stream()
                    .map(role -> org.egov.common.contract.request.Role.builder()
                        .code(role.getCode())
                        .name(role.getName())
                        .tenantId(role.getTenantId())
                        .build())
                    .collect(Collectors.toList()) : Collections.emptyList())
            .build();

        // Debug logging for authorization troubleshooting
        log.info("/_details endpoint returning commonUser {} with {} roles",
            commonUser.getUuid(),
            commonUser.getRoles().size());

        if (commonUser.getRoles().isEmpty()) {
            log.warn("WARNING: User {} has no roles - this will cause RBAC authorization failures!",
                commonUser.getUuid());
            log.warn("This means authUser.getRoles() was either null or empty");
        }

        return commonUser;
        //  no encrypt/decrypt
    }

    /**
     * end-point to update the user details without otp validations.
     *
     * @param createUserRequest
     * @param headers
     * @return
     */
    @PostMapping("/users/_updatenovalidate")
    public UpdateResponse updateUserWithoutValidation(@RequestBody final @Valid CreateUserRequest createUserRequest,
                                                      @RequestHeader HttpHeaders headers) {
        if (createUserRequest != null && userService.isCallerCitizen(createUserRequest.getRequestInfo())) {
            String callerUuid = (createUserRequest.getRequestInfo() != null && createUserRequest.getRequestInfo().getUserInfo() != null)
                    ? createUserRequest.getRequestInfo().getUserInfo().getUuid() : "unknown";
            log.error("[USER-CONTROLLER] Unauthorized access: CITIZEN users cannot call /users/_updatenovalidate: uuid={}", callerUuid);
            throw new CustomException("UNAUTHORIZED_ACCESS", "CITIZEN users are not allowed to use /users/_updatenovalidate");
        }
        if (createUserRequest != null && createUserRequest.getUser() != null
                && UserType.CITIZEN.equals(createUserRequest.getUser().getType())) {
            log.error("[USER-CONTROLLER] Invalid update: target user type cannot be CITIZEN in /users/_updatenovalidate");
            throw new CustomException("INVALID_USER_TYPE", "Cannot update CITIZEN user accounts via /users/_updatenovalidate");
        }
        User user = createUserRequest.toDomain(false);
        user.setMobileValidationMandatory(isMobileValidationRequired(headers));
        final User updatedUser = userService.updateWithoutOtpValidation(user, createUserRequest.getRequestInfo());
        return createResponseforUpdate(updatedUser);
    }

    /**
     * Legacy end-point to update user profile (delegated to ProfileController under /profile/_update).
     */
    // Legacy endpoint migrated to ProfileController.java to enforce source-based field locking and proper auditing.


    /**
     * Authenticates and registers/updates a citizen profile via DigiLocker access token.
     *
     * @param createUserRequest request payload containing DigiLocker access token and citizen attributes
     * @param headers           HTTP headers from incoming request
     * @return response containing citizen user details and generated access tokens
     */
    @PostMapping("/digilocker/oauth/token")
    public Object authDigiLocker(@RequestBody @Valid CreateUserRequest createUserRequest, @RequestHeader HttpHeaders headers) {
        return digiLockerService.authDigiLocker(createUserRequest, headers);
    }





    private UserDetailResponse createResponse(User newUser) {
        UserRequest userRequest = new UserRequest(newUser);
        ResponseInfo responseInfo = ResponseInfo.builder().status(String.valueOf(HttpStatus.OK.value())).build();
        return new UserDetailResponse(responseInfo, Collections.singletonList(userRequest));
    }

    private UpdateResponse createResponseforUpdate(User newUser) {
        UpdateRequest updateRequest = new UpdateRequest(newUser);
        ResponseInfo responseInfo = ResponseInfo.builder().status(String.valueOf(HttpStatus.OK.value())).build();
        return new UpdateResponse(responseInfo, Collections.singletonList(updateRequest));
    }

    private UserSearchResponse searchUsers(@RequestBody UserSearchRequest request, HttpHeaders headers) {

        UserSearchCriteria searchCriteria = request.toDomain();
        log.info("searchCriteria_in controller"+searchCriteria);
        log.info("headers"+headers);

        if (!isInterServiceCall(headers)) {
            if ((isEmpty(searchCriteria.getId()) && isEmpty(searchCriteria.getUuid())) && (searchCriteria.getLimit() > getDefaultSearchSize()
                    || searchCriteria.getLimit() == 0))
                searchCriteria.setLimit(getDefaultSearchSize());
        }

        List<User> userModels = userService.searchUsers(searchCriteria, isInterServiceCall(headers), request.getRequestInfo());
        List<UserSearchResponseContent> userContracts = userModels.stream().map(UserSearchResponseContent::new)
                .collect(Collectors.toList());
        ResponseInfo responseInfo = ResponseInfo.builder().status(String.valueOf(HttpStatus.OK.value())).build();
        return new UserSearchResponse(responseInfo, userContracts);
    }

    private boolean isMobileValidationRequired(HttpHeaders headers) {
        boolean x_pass_through_gateway = !isInterServiceCall(headers);
        String workaroundEnabled = getMobileValidationWorkaroundEnabled();
        if (workaroundEnabled != null && Boolean.valueOf(workaroundEnabled)
                && !x_pass_through_gateway) {
            return false;
        }
        return true;
    }
/// Author - Abhijeet


    /**
     * Endpoint to create a new address for a user identified by their UUID.
     *
     * @param addressRequest The address details to be saved.
     * @return AddressResponse containing the created address details and response info.
     */
    @PostMapping("/_createAddress")
    public ResponseEntity<AddressResponse> createAddress(@RequestBody AddressRequest addressRequest) {

        Address userAddress = userService.createAddress(addressRequest.getUserUuid(), addressRequest.getAddress());
        List<Address> userAddressList = new ArrayList<>();
        userAddressList.add(userAddress);
        ResponseInfo responseInfo = ResponseInfo.builder().status(HttpStatus.CREATED.toString()).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(new AddressResponse(responseInfo, userAddressList));
    }

    /**
     * Endpoint to retrieve address details for a user based on their UUID and tenant ID.
     *
     * @param uuid     The unique identifier of the user.
     * @param tenantId The tenant ID associated with the user.
     * @return AddressResponse containing the user's address details and response info.
     */
    @PostMapping("/_getAddress")
    public AddressResponse getAddress(@RequestParam(value = "uuid") String uuid, @RequestParam(value = "tenantId") String tenantId) {

        List<Address> userAddresses = userService.getAddress(uuid, tenantId);
        ResponseInfo responseInfo = ResponseInfo.builder().status(String.valueOf(HttpStatus.OK.value())).build();
        return new AddressResponse(responseInfo, userAddresses);
    }

    /**
     * Endpoint to update an existing address for a user identified by their UUID.
     *
     * @param addressRequest The address details to be updated.
     * @return AddressResponse containing the updated address details and response info.
     */
    @PostMapping("/_updateAddress")
    public AddressResponse updateAddress(@RequestBody AddressRequest addressRequest) {

        Address userAddress = userService.updateAddress(addressRequest.getAddress());
        List<Address> userAddressList = new ArrayList<>();
        userAddressList.add(userAddress);
        ResponseInfo responseInfo = ResponseInfo.builder().status(String.valueOf(HttpStatus.OK.value())).build();
        return new AddressResponse(responseInfo, userAddressList);
    }

    /**
     * API endpoint to create a new user with address details (V2).
     * <p>
     * Process:
     * - Converts the request DTO into a domain object.
     * - Determines if mobile validation is required based on headers.
     * - Calls the service layer to create a user.
     * - Returns a response containing the newly created user.
     *
     * @param createUserRequest the request payload containing user details
     * @param headers           HTTP headers for additional request context
     * @return the response object containing the created user details
     */
    @Operation(summary = "Create User with Address (V2)", description = "Creates a new user account along with address details in a single operation.")
    @PostMapping("/users/v2/_create")
    public Object createUserWithAddress(@RequestBody @Valid CreateUserRequestV2 createUserRequest,
                                        @RequestHeader HttpHeaders headers) {
        log.info("Received User Registration Request " + createUserRequest);

        User user = createUserRequest.toDomain(true);
        user.setMobileValidationMandatory(isMobileValidationRequired(headers));
        user.setOtpValidationMandatory(false);

        final User newUser = userService.createUserWithAddressV2(user, createUserRequest.getRequestInfo());

        return createResponseV2(newUser);
    }

    /**
     * API endpoint to update user details along with address information (V2).
     * <p>
     * Process:
     * - Converts the request DTO into a domain object.
     * - Determines if mobile validation is required based on headers.
     * - Calls the service layer to update user details.
     * - Returns a response containing the updated user details.
     * <p>
     * Note: OTP validation is NOT required for this update process.
     *
     * @param createUserRequest the request payload containing updated user details
     * @param headers           HTTP headers for additional request context
     * @return the response object containing updated user details
     */
    @Operation(summary = "Update User with Address (V2)", description = "Updates user account details along with address information (V2).")
    @PostMapping("/users/v2/_update")
    public UpdateResponse updateUserV2(@RequestBody @Valid CreateUserRequestV2 createUserRequest,
                                                @RequestHeader HttpHeaders headers) {

        User user = createUserRequest.toDomain(false);
        user.setMobileValidationMandatory(isMobileValidationRequired(headers));

        final User updatedUser = userService.updateUserV2(user, createUserRequest.getRequestInfo());

        return createResponseforUpdate(updatedUser);
    }

    /**
     * API endpoint to search users along with their address details (V2).
     * <p>
     * Process:
     * - Converts the search request DTO into a domain object.
     * - If the request is not an inter-service call:
     * - Limits the search results if no specific user ID or UUID is provided.
     * - Calls the service layer to fetch matching users based on search criteria.
     * - Constructs the response with user details.
     * <p>
     * Note: If the `active` field is not specified in the request, both active
     * and inactive users will be fetched.
     *
     * @param request the request payload containing search criteria
     * @param headers HTTP headers for additional request context
     * @return the response object containing matching user details
     */
    @Operation(summary = "Search Users with Address (V2)", description = "Searches users by criteria and enriches results with address information.")
    @PostMapping("/users/v2/_search")
    public UserSearchResponse getUsersV2(@RequestBody UserSearchRequestV2 request, @RequestHeader HttpHeaders headers) {

        UserSearchCriteria searchCriteria = request.toDomain();

        if (!isInterServiceCall(headers)) {
            if ((isEmpty(searchCriteria.getId()) && isEmpty(searchCriteria.getUuid())) &&
                    (searchCriteria.getLimit() > getDefaultSearchSize() || searchCriteria.getLimit() == 0)) {
                searchCriteria.setLimit(getDefaultSearchSize());
            }
        }

        List<User> userModels = userService.searchUsersV2(searchCriteria, isInterServiceCall(headers), request.getRequestInfo());
        List<UserSearchResponseContent> userContracts = userModels.stream()
                .map(UserSearchResponseContent::new)
                .collect(Collectors.toList());
        ResponseInfo responseInfo = ResponseInfo.builder().status(String.valueOf(HttpStatus.OK.value())).build();

        return new UserSearchResponse(responseInfo, userContracts);
    }

    private UserDetailResponseV2 createResponseV2(User newUser) {
        UserRequestV2 userRequestV2 = new UserRequestV2(newUser);
        ResponseInfo responseInfo = ResponseInfo.builder().status(String.valueOf(HttpStatus.OK.value())).build();
        return new UserDetailResponseV2(responseInfo, Collections.singletonList(userRequestV2));
    }
}
