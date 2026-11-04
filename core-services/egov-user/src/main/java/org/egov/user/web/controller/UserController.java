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
import org.egov.user.util.MaskUtils;
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
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.egov.tracer.http.HttpUtils.isInterServiceCall;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * REST Controller exposing core citizen, employee, and address management endpoints in UPYOG.
 *
 * <p>Serves as the primary transport boundary for:</p>
 * <ul>
 *   <li>Citizen registration with OTP verification or auto-login ({@code /citizen/_create}).</li>
 *   <li>Internal employee and system user creation without OTP ({@code /users/_createnovalidate}).</li>
 *   <li>Multi-criteria user searches with role-based and active-status filtering ({@code /_search}, {@code /v1/_search}, {@code /users/v2/_search}).</li>
 *   <li>OAuth2 access token introspection and user detail resolution for Zuul API Gateway ({@code /_details}).</li>
 *   <li>Administrative user updates ({@code /users/_updatenovalidate}).</li>
 *   <li>Legacy and V2 citizen address lifecycle management ({@code /_createAddress}, {@code /_getAddress}, {@code /_updateAddress}).</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Tag(name = "User Management", description = "Core citizen and employee lifecycle management endpoints")
@RestController
@Slf4j
public class UserController {

    private UserService userService;
    private TokenService tokenService;
    private DigiLockerService digiLockerService;
    private UserConfiguration userConfiguration;

    /**
     * Constructs a new {@link UserController} injecting all necessary service and configuration components.
     *
     * @param userService core business logic service for user accounts
     * @param tokenService token introspection and session management service
     * @param digiLockerService third-party DigiLocker auth adapter (optional)
     * @param userConfiguration platform user service configuration properties
     */
    @Autowired
    public UserController(UserService userService, TokenService tokenService,
                          @Autowired(required = false) DigiLockerService digiLockerService,
                          @Autowired(required = false) UserConfiguration userConfiguration) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.digiLockerService = digiLockerService;
        this.userConfiguration = userConfiguration;
    }

    /**
     * Backward-compatible constructor for testing and standalone execution.
     *
     * @param userService core business logic service for user accounts
     * @param tokenService token introspection and session management service
     */
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
     * Registers a new citizen account with mandatory OTP verification.
     *
     * <p>If registration-with-login is enabled in {@link UserConfiguration}, automatically creates the citizen
     * account, issues an OAuth2 bearer access token, and returns the token alongside citizen profile details.
     * Otherwise, creates the citizen account and returns standard {@link UserDetailResponse}.</p>
     *
     * @param createUserRequest the citizen registration request payload containing demographics and validated OTP; must not be {@code null}.
     * @return an {@link Object} wrapping either an authentication response map with OAuth2 tokens (when auto-login is enabled)
     *         or a {@link UserDetailResponse} containing the created citizen profile.
     * @throws org.egov.tracer.model.CustomException if OTP is invalid, mobile number already exists, or validation rules fail.
     */
    @PostMapping("/citizen/_create")
    public Object createCitizen(@RequestBody @Valid CreateUserRequest createUserRequest) {
        log.info("Received Citizen Registration Request for mobile: {}", createUserRequest != null && createUserRequest.getUser() != null ? MaskUtils.maskMobile(createUserRequest.getUser().getMobileNumber()) : null);
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
     * Administrative endpoint to create an employee or system user account without OTP verification.
     *
     * <p>Reserved for internal administrative systems, employee onboarding, and background microservices.
     * Strictly verifies mobile validation requirements against headers and configuration.</p>
     *
     * @param createUserRequest the user creation request containing user attributes and assigned roles; must not be {@code null}.
     * @param headers HTTP headers from the incoming request (used for bypass checks and audit tracing).
     * @return a {@link UserDetailResponse} wrapping the newly provisioned user account.
     * @throws org.egov.tracer.model.CustomException if mandatory fields are missing or username/mobile collisions occur.
     */
    @PostMapping("/users/_createnovalidate")
    public UserDetailResponse createUserWithoutValidation(@RequestBody @Valid CreateUserRequest createUserRequest,
                                                          @RequestHeader HttpHeaders headers) {

        User user = createUserRequest.toDomain(true);
        if (StringUtils.isBlank(user.getUsername())) {
            if (StringUtils.isNotBlank(user.getMobileNumber())) {
                user.setUsername(user.getMobileNumber());
            } else {
                user.setUsername(UserConstants.CITIZEN_USERNAME_PREFIX + UUID.randomUUID().toString().substring(0, 8));
            }
        }
        if (user.getType() == null) {
            user.setType(UserType.CITIZEN);
        }
        if (user.getActive() == null) {
            user.setActive(true);
        }
        if (CollectionUtils.isEmpty(user.getRoles()) && UserType.CITIZEN.equals(user.getType())) {
            user.setRoles(Collections.singleton(Role.getCitizenRole()));
        }
        boolean mobileRequired = isMobileValidationRequired(headers) && StringUtils.isNotBlank(user.getMobileNumber());
        user.setMobileValidationMandatory(mobileRequired);
        user.setOtpValidationMandatory(false);
        final User newUser = userService.createUser(user, createUserRequest.getRequestInfo());
        return createResponse(newUser);
    }

    /**
     * Searches user accounts based on filter criteria, defaulting to active users only.
     *
     * <p>If {@code request.getActive()} is omitted, automatically filters for {@code active = true}.
     * Enforces pagination limits on non-internal requests and decrypts authorized fields based on caller context.</p>
     *
     * @param request search request criteria containing identifiers, names, phone numbers, or role codes.
     * @param headers HTTP headers used to distinguish internal microservice calls from external client queries.
     * @return {@link UserSearchResponse} containing decrypted user records matching the filter criteria.
     */
    @PostMapping("/_search")
    public UserSearchResponse get(@RequestBody @Valid UserSearchRequest request, @RequestHeader HttpHeaders headers) {

        log.info("Received User search Request for tenant: {}, mobile: {}", request != null ? request.getTenantId() : null, request != null ? MaskUtils.maskMobile(request.getMobileNumber()) : null);
        if (request.getActive() == null) {
            request.setActive(true);
        }
        return searchUsers(request, headers);
    }

    /**
     * Searches user accounts based on filter criteria, returning both active and inactive users if unconstrained.
     *
     * <p>Unlike {@code /_search}, does not default {@code active} to true, enabling retrieval of deactivated
     * or historical accounts for administrative audit screens.</p>
     *
     * @param request search criteria filter envelope.
     * @param headers incoming HTTP headers used for internal service detection.
     * @return {@link UserSearchResponse} containing matching user accounts across all activation states.
     */
    @PostMapping("/v1/_search")
    public UserSearchResponse getV1(@RequestBody UserSearchRequest request, @RequestHeader HttpHeaders headers) {
        return searchUsers(request, headers);
    }

    /**
     * Resolves authenticated citizen or employee details using an active OAuth2 access token.
     *
     * <p>Called by the Zuul API Gateway edge filter to introspect bearer tokens, validate token validity,
     * and extract user roles and tenant affiliations for downstream context propagation.</p>
     *
     * @param accessToken the bearer access token string to introspect; must not be blank.
     * @return the common contract {@link org.egov.common.contract.request.User} object representing the authenticated principal.
     * @throws org.egov.tracer.model.CustomException if the token is expired, invalid, or unrecognized.
     */
    @PostMapping("/_details")
    public org.egov.common.contract.request.User getUser(@RequestParam(value = "access_token") String accessToken) {
        log.info("/_details endpoint called with token: {}...", accessToken.substring(0, Math.min(8, accessToken.length())));

        final UserDetail userDetail = tokenService.getUser(accessToken);
        log.info("TokenService returned userDetail: {}", userDetail != null ? "not null" : "NULL");

        // CRITICAL FIX: Return common contract User instead of CustomUserDetails for proper gateway deserialization
        org.egov.user.web.contract.auth.User authUser = userDetail.getSecureUser().getUser();
        log.info("AuthUser extracted from userDetail - id: {}, uuid: {}, userName: {}",
            authUser.getId(), authUser.getUuid(), MaskUtils.maskIdentifier(authUser.getUserName()));
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
     * Updates a user account without requiring SMS OTP verification.
     * Functions symmetrically to {@code /users/_createnovalidate} for user updates.
     *
     * @param createUserRequest the user update request payload; must not be {@code null}.
     * @param headers HTTP headers from the incoming request.
     * @return an {@link UpdateResponse} containing the updated user account details.
     * @throws org.egov.tracer.model.CustomException if update fails.
     */
    @PostMapping("/users/_updatenovalidate")
    public UpdateResponse updateUserWithoutValidation(@RequestBody final @Valid CreateUserRequest createUserRequest,
                                                      @RequestHeader HttpHeaders headers) {
        User user = createUserRequest.toDomain(false);
        boolean mobileRequired = isMobileValidationRequired(headers) && StringUtils.isNotBlank(user.getMobileNumber());
        user.setMobileValidationMandatory(mobileRequired);
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
        log.info("searchCriteria in controller for tenant: {}, mobile: {}", searchCriteria.getTenantId(), MaskUtils.maskMobile(searchCriteria.getMobileNumber()));
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
        log.info("Received User Registration Request for mobile: {}", createUserRequest != null && createUserRequest.getUser() != null ? MaskUtils.maskMobile(createUserRequest.getUser().getMobileNumber()) : null);

        User user = createUserRequest.toDomain(true);
        boolean mobileRequired = isMobileValidationRequired(headers) && StringUtils.isNotBlank(user.getMobileNumber());
        user.setMobileValidationMandatory(mobileRequired);
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
        boolean mobileRequired = isMobileValidationRequired(headers) && StringUtils.isNotBlank(user.getMobileNumber());
        user.setMobileValidationMandatory(mobileRequired);

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
