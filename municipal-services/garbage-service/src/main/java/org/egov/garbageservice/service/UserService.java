package org.egov.garbageservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.garbageservice.config.GarbageServiceConfig;
import org.egov.garbageservice.repository.ServiceRequestRepository;
import org.egov.garbageservice.util.UserUtil;
import org.egov.garbageservice.web.models.garbage.ApplicantDetail;
import org.egov.garbageservice.web.models.garbage.AuditDetails;
import org.egov.garbageservice.web.models.garbage.GarbageApplication;
import org.egov.garbageservice.web.models.garbage.GarbageSearchCriteria;
import org.egov.garbageservice.web.models.user.CreateUserRequest;
import org.egov.garbageservice.web.models.user.User;
import org.egov.garbageservice.web.models.user.UserDetailResponse;
import org.egov.garbageservice.web.models.user.UserSearchRequest;
import org.egov.tracer.model.CustomException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service managing user creation, lookup, and search enrichment via eGov User Service.
 * Method naming and patterns aligned with tp-services.
 */
@Service
@Slf4j
public class UserService {

    private final ObjectMapper mapper;
    private final ServiceRequestRepository serviceRequestRepository;
    private final GarbageServiceConfig config;
    private final UserUtil userUtil;

    public UserService(ObjectMapper mapper, ServiceRequestRepository serviceRequestRepository,
                       GarbageServiceConfig config, UserUtil userUtil) {
        this.mapper = mapper;
        this.serviceRequestRepository = serviceRequestRepository;
        this.config = config;
        this.userUtil = userUtil;
    }

    /**
     * Retrieves an existing user for the given applicant details, or null if not found.
     *
     * @param tenantId ULB tenant
     * @param applicantDetail applicant details containing mobile number
     * @param requestInfo request context
     * @return User object or null
     */
    public User fetchExistingUser(String tenantId, ApplicantDetail applicantDetail, RequestInfo requestInfo) {
        if (applicantDetail == null || StringUtils.isBlank(applicantDetail.getMobileNumber())) {
            return null;
        }
        return fetchExistingUserByMobile(tenantId, applicantDetail.getMobileNumber(), requestInfo);
    }

    /**
     * Retrieves an existing user for a given mobile number.
     *
     * @param tenantId ULB tenant
     * @param mobileNumber mobile number to look up
     * @param requestInfo request context
     * @return User object or null
     */
    public User fetchExistingUserByMobile(String tenantId, String mobileNumber, RequestInfo requestInfo) {
        if (StringUtils.isBlank(mobileNumber)) {
            return null;
        }

        UserSearchRequest userSearchRequest = getBaseUserSearchRequest(UserUtil.getStateLevelTenant(tenantId), requestInfo);
        userSearchRequest.setMobileNumber(mobileNumber);
        userSearchRequest.setUserType("CITIZEN");

        StringBuilder uri = new StringBuilder(config.getUserServiceHostUrl())
                .append(config.getUserSearchEndpoint());

        UserDetailResponse userDetailResponse = userUtil.userCall(userSearchRequest, uri);
        if (userDetailResponse == null || CollectionUtils.isEmpty(userDetailResponse.getUser())) {
            log.info("No existing user found for mobile number: {}", mobileNumber);
            return null;
        }
        return userDetailResponse.getUser().get(0);
    }

    /**
     * Creates a new citizen user in user registry and returns the generated User details.
     *
     * @param requestInfo request context
     * @param applicantDetail applicant details
     * @param tenantId ULB tenant
     * @return created User
     */
    public User createUserHandler(RequestInfo requestInfo, ApplicantDetail applicantDetail, String tenantId) {
        Role role = userUtil.getCitizenRole();
        User user = convertApplicantToUserRequest(applicantDetail, role, tenantId);

        StringBuilder uri = new StringBuilder(config.getUserServiceHostUrl())
                .append("/user/users/_createnovalidate");

        CreateUserRequest userRequest = CreateUserRequest.builder()
                .requestInfo(requestInfo)
                .user(user)
                .build();

        UserDetailResponse userDetailResponse = userUtil.userCall(userRequest, uri);

        if (ObjectUtils.isEmpty(userDetailResponse) || CollectionUtils.isEmpty(userDetailResponse.getUser())) {
            throw new CustomException("INVALID_USER_RESPONSE",
                    "User creation failed for mobile number: " + applicantDetail.getMobileNumber());
        }
        User createdUser = userDetailResponse.getUser().get(0);
        log.info("Created new user with uuid: {}", createdUser.getUuid());
        return createdUser;
    }

    private User convertApplicantToUserRequest(ApplicantDetail applicant, Role role, String tenantId) {
        User user = new User();
        String name = applicant.getName() != null ? applicant.getName() : applicant.getApplicantName();
        user.setName(name);
        user.setUserName(applicant.getMobileNumber());
        user.setMobileNumber(applicant.getMobileNumber());
        user.setAltContactNumber(applicant.getAlternateNumber());
        user.setEmailId(applicant.getEmailId());
        user.setGender(applicant.getGender());
        user.setFatherOrHusbandName(applicant.getGuardianName());
        user.setActive(true);
        user.setTenantId(tenantId);
        user.setRoles(Collections.singletonList(role));
        user.setType("CITIZEN");
        return user;
    }

    /**
     * Searches user registry using a generic UserSearchRequest.
     *
     * @param userSearchRequest criteria
     * @return UserDetailResponse
     */
    public UserDetailResponse searchUser(UserSearchRequest userSearchRequest) {
        StringBuilder uri = new StringBuilder(config.getUserServiceHostUrl())
                .append(config.getUserSearchEndpoint());
        return userUtil.userCall(userSearchRequest, uri);
    }

    public UserDetailResponse getUser(UserSearchRequest userSearchRequest) {
        return searchUser(userSearchRequest);
    }

    /**
     * Searches user UUIDs for a given mobile number across a tenant.
     *
     * @param tenantId ULB tenant
     * @param mobileNumber mobile number
     * @param requestInfo request context
     * @return list of matching user UUIDs
     */
    public List<String> searchUserUuidsByMobile(String tenantId, String mobileNumber, RequestInfo requestInfo) {
        if (StringUtils.isBlank(mobileNumber)) {
            return Collections.emptyList();
        }
        UserSearchRequest userSearchRequest = getBaseUserSearchRequest(UserUtil.getStateLevelTenant(tenantId), requestInfo);
        userSearchRequest.setMobileNumber(mobileNumber);
        userSearchRequest.setUserType("CITIZEN");

        UserDetailResponse response = searchUser(userSearchRequest);
        if (response != null && !CollectionUtils.isEmpty(response.getUser())) {
            return response.getUser().stream().map(User::getUuid).filter(Objects::nonNull).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * Enriches a GarbageApplication with citizen contact details dynamically resolved from
     * the User Service via ownerUuid and tenantUuid (as name/mobile are not stored in DB columns).
     *
     * @param application the application to enrich
     * @param criteria search criteria
     */
    public void enrichApplicationWithUserDetails(GarbageApplication application, GarbageSearchCriteria criteria) {
        try {
            Set<String> uuidsToFetch = new HashSet<>();
            if (StringUtils.isNotBlank(application.getTenantUuid())) {
                uuidsToFetch.add(application.getTenantUuid());
            }
            if (StringUtils.isNotBlank(application.getOwnerUuid())) {
                uuidsToFetch.add(application.getOwnerUuid());
            }

            if (uuidsToFetch.isEmpty()) {
                return;
            }

            UserSearchRequest userSearchRequest = UserSearchRequest.builder()
                    .uuid(uuidsToFetch)
                    .build();

            UserDetailResponse userDetailResponse = getUser(userSearchRequest);
            if (userDetailResponse != null && !CollectionUtils.isEmpty(userDetailResponse.getUser())) {
                Map<String, User> userMap = userDetailResponse.getUser().stream()
                        .filter(u -> u != null && u.getUuid() != null)
                        .collect(Collectors.toMap(User::getUuid, (User u) -> u, (a, b) -> a));

                // If tenant application, primary applicant is tenant; otherwise owner
                String targetUuid = StringUtils.isNotBlank(application.getTenantUuid())
                        ? application.getTenantUuid()
                        : application.getOwnerUuid();

                User primaryUser = userMap.get(targetUuid);
                if (primaryUser != null) {
                    ApplicantDetail applicantDetail = convertUserToApplicantDetail(primaryUser, targetUuid,
                            application.getApplicationNo(), application.getAuditDetails());
                    application.setApplicantDetails(Collections.singletonList(applicantDetail));
                }
            }
        } catch (Exception e) {
            log.error("Error while enriching garbage application with user details: {}", e.getMessage(), e);
        }

        // Fallback enrichment from additionalDetail if applicantDetails is still empty
        try {
            if (CollectionUtils.isEmpty(application.getApplicantDetails()) && application.getAdditionalDetail() != null) {
                com.fasterxml.jackson.databind.JsonNode addNode = application.getAdditionalDetail();
                if (addNode.has("applicantDetails") && addNode.get("applicantDetails").isArray() && !addNode.get("applicantDetails").isEmpty()) {
                    com.fasterxml.jackson.databind.JsonNode appNode = addNode.get("applicantDetails").get(0);
                    ApplicantDetail ad = ApplicantDetail.builder()
                            .name(appNode.has("name") ? appNode.get("name").asText() : (appNode.has("applicantName") ? appNode.get("applicantName").asText() : ""))
                            .applicantName(appNode.has("applicantName") ? appNode.get("applicantName").asText() : (appNode.has("name") ? appNode.get("name").asText() : ""))
                            .mobileNumber(appNode.has("mobileNumber") ? appNode.get("mobileNumber").asText() : "")
                            .emailId(appNode.has("emailId") ? appNode.get("emailId").asText() : "")
                            .build();
                    application.setApplicantDetails(Collections.singletonList(ad));
                } else if (addNode.has("tenantDetails")) {
                    com.fasterxml.jackson.databind.JsonNode tNode = addNode.get("tenantDetails");
                    ApplicantDetail ad = ApplicantDetail.builder()
                            .name(tNode.has("name") ? tNode.get("name").asText() : "")
                            .applicantName(tNode.has("name") ? tNode.get("name").asText() : "")
                            .mobileNumber(tNode.has("mobileNumber") ? tNode.get("mobileNumber").asText() : "")
                            .emailId(tNode.has("emailId") ? tNode.get("emailId").asText() : (tNode.has("email") ? tNode.get("email").asText() : ""))
                            .build();
                    application.setApplicantDetails(Collections.singletonList(ad));
                }
            }
        } catch (Exception ex) {
            log.warn("Fallback applicant details enrichment from additionalDetail skipped: {}", ex.getMessage());
        }
    }

    public ApplicantDetail convertUserToApplicantDetail(User user, String applicantUuid, String applicationNo, AuditDetails auditDetails) {
        if (user == null) {
            return null;
        }
        return ApplicantDetail.builder()
                .name(user.getName())
                .applicantName(user.getName())
                .emailId(user.getEmailId())
                .mobileNumber(user.getMobileNumber())
                .alternateNumber(user.getAltContactNumber())
                .gender(user.getGender())
                .guardianName(user.getFatherOrHusbandName())
                .build();
    }

    public UserSearchRequest getBaseUserSearchRequest(String tenantId, RequestInfo requestInfo) {
        return UserSearchRequest.builder()
                .requestInfo(requestInfo)
                .userType("CITIZEN")
                .tenantId(tenantId)
                .active(true)
                .build();
    }
}
