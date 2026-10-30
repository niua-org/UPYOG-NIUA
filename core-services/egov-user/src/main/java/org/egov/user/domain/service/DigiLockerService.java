package org.egov.user.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.config.UserConfiguration;
import org.egov.user.domain.model.TokenReq;
import org.egov.user.domain.model.TokenRequest;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.web.contract.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.egov.tracer.http.HttpUtils.isInterServiceCall;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * Service orchestrating citizen authentication and profile linking via DigiLocker OAuth2 tokens.
 *
 * <p>Validates incoming DigiLocker tokens against the requester service adapter, matches citizen
 * by mobile number or username, ensures accounts are active, and provisions or updates citizen profiles.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class DigiLockerService {

    private final UserService userService;
    private final UserConfiguration userConfiguration;

    /**
     * Primary constructor injecting user service and configuration properties.
     *
     * @param userService       core user domain service
     * @param userConfiguration application configuration properties
     */
    @Autowired
    public DigiLockerService(UserService userService, @Autowired(required = false) UserConfiguration userConfiguration) {
        this.userService = userService;
        this.userConfiguration = userConfiguration;
    }

    /**
     * Testing constructor with user service.
     *
     * @param userService core user domain service
     */
    public DigiLockerService(UserService userService) {
        this(userService, null);
    }

    private boolean isDigiLockerRegistration() {
        return userConfiguration != null && Boolean.TRUE.equals(userConfiguration.getDigilockerRegister());
    }

    private boolean isDigiLockerSearch() {
        return userConfiguration != null && Boolean.TRUE.equals(userConfiguration.getDigilockerSearch());
    }

    private String getRequesterServiceHost() {
        return userConfiguration != null && userConfiguration.getRequesterServiceHost() != null
                ? userConfiguration.getRequesterServiceHost() : "";
    }

    private String getRequesterServiceEndpoint() {
        return userConfiguration != null && userConfiguration.getRequesterServiceEndpoint() != null
                ? userConfiguration.getRequesterServiceEndpoint() : "";
    }

    private int getDefaultSearchSize() {
        return userConfiguration != null && userConfiguration.getDefaultSearchSize() != null
                ? userConfiguration.getDefaultSearchSize() : 10;
    }

    /**
     * Authenticates and registers/updates a citizen profile via DigiLocker.
     *
     * @param createUserRequest request containing DigiLocker access token and citizen parameters
     * @param headers           HTTP headers from incoming request
     * @return response containing authenticated user details and access token
     */
    public Object authDigiLocker(CreateUserRequest createUserRequest, HttpHeaders headers) {
        RestTemplate restTemplate = new RestTemplate();
        // String url = "http://localhost:8270/requester-services-dx/user/details";

        UserSearchRequest request = new UserSearchRequest();

        TokenReq tokenReq = TokenReq.builder()
                .authToken(createUserRequest.getUser().getAccess_token())
                .build();

        TokenRequest tokenRequest = TokenRequest.builder()
                .requestInfo(createUserRequest.getRequestInfo())
                .tokenReq(tokenReq)
                .build();

        HttpEntity<TokenRequest> entity = new HttpEntity<>(tokenRequest, headers);

        ResponseEntity<String> validationApiResponse;
        try {
            validationApiResponse = restTemplate.exchange(getRequesterServiceHost() + getRequesterServiceEndpoint(), HttpMethod.POST, entity, String.class);
            //validationApiResponse = restTemplate.exchange(url, HttpMethod.POST, entity, Strings.class);
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Validation failed: " + e.getResponseBodyAsString(), e);
        }

        String validationApiResponseBody = validationApiResponse.getBody();
        JSONObject jsonResponse = new JSONObject(validationApiResponseBody);
        JSONObject userRes = jsonResponse.getJSONObject("UserRes");
        String validationMobileNumber = userRes.getString("mobile");
        log.info("validationMobileNumber" + validationMobileNumber);
        log.info("user Mobile Number is" + (createUserRequest.getUser().getMobileNumber()));

        if (validationMobileNumber.equalsIgnoreCase(createUserRequest.getUser().getMobileNumber())) {
            log.info("entering IF condition");

            request.setUserName(createUserRequest.getUser().getMobileNumber());
            request.setTenantId(createUserRequest.getUser().getTenantId());

            List<UserSearchResponseContent> userContracts = searchUsers(request, headers).getUserSearchResponseContent();
            if (userContracts.isEmpty()) {
                UserSearchRequest mobileReq = new UserSearchRequest();
                mobileReq.setMobileNumber(createUserRequest.getUser().getMobileNumber());
                mobileReq.setTenantId(createUserRequest.getUser().getTenantId());
                userContracts = searchUsers(mobileReq, headers).getUserSearchResponseContent();
            }

            if (!userContracts.isEmpty()) {

                User existingUser = userContracts.get(0).toUser();
                if (!Boolean.TRUE.equals(existingUser.getActive())) {
                    log.error("[DIGILOCKER] Account is inactive for user: {}", existingUser.getUsername());
                    throw new CustomException(UserConstants.ERROR_ACCOUNT_INACTIVE, UserConstants.MSG_ACCOUNT_INACTIVE);
                }
                User user = new User().toUser(existingUser);
                user.setDigilockerid(createUserRequest.getUser().getDigilockerid());
                user.setDigilockerRegistration(isDigiLockerRegistration());
                user.setIsIdentityVerified(true);
                user.setIdentityVerifiedDate(new Date());
                user.setIdentityVerifiedSource(UserConstants.IDENTITY_SOURCE_DIGILOCKER);
                Object updatedUser = userService.updateDigilockerID(user, existingUser, createUserRequest.getRequestInfo());
                return new ResponseEntity<>(updatedUser, HttpStatus.OK);

            } else {
                User user = createUserRequest.toDomain(true);
                user.setUsername(user.getMobileNumber());
                user.setDigilockerRegistration(isDigiLockerRegistration());
                user.setDigilockerid(createUserRequest.getUser().getDigilockerid());
                user.setIsIdentityVerified(true);
                user.setIdentityVerifiedDate(new Date());
                user.setIdentityVerifiedSource(UserConstants.IDENTITY_SOURCE_DIGILOCKER);
                Object createdUser = userService.registerWithLogin(user, createUserRequest.getRequestInfo());
                return new ResponseEntity<>(createdUser, HttpStatus.OK);
            }
        }
        return new ResponseEntity<>("Mobile number validation failed", HttpStatus.BAD_REQUEST);
    }

    private UserSearchResponse searchUsers(UserSearchRequest request, HttpHeaders headers) {

        UserSearchCriteria searchCriteria = request.toDomain();
        log.info("searchCriteria_in controller" + searchCriteria);
        log.info("headers" + headers);

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
}
