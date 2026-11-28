package org.egov.user.domain.service.family;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.config.UserConfiguration;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.AddressSearchCriteria;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.asset.AssetAuthorizationService;
import org.egov.user.domain.service.config.FamilyConfigurationService;
import org.egov.user.domain.service.utils.EncryptionDecryptionUtil;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.AddressRepository;
import org.egov.user.persistence.repository.FamilyRestRepository;
import org.egov.user.persistence.repository.UserDocumentRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.UserSearchResponseContent;
import org.egov.user.web.contract.document.UserDocumentSearchCriteria;
import org.egov.user.web.contract.family.FamilyDetail;
import org.egov.user.web.contract.family.FamilyMemberDetail;
import org.egov.user.web.contract.family.FamilyProfileResponse;
import org.egov.user.web.contract.family.FamilyProfileSearchRequest;
import org.egov.user.web.contract.profile.ProfileIncludeEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation managing family full profile discovery.
 *
 * <p>Orchestrates:</p>
 * <ul>
 *   <li>Household resolution via {@code upyog-family-service}.</li>
 *   <li>Citizen security isolation: verifying caller identity against family membership or privileged role.</li>
 *   <li>Batch pre-fetching and decryption of member profiles, documents, and addresses from {@code egov-user}.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class FamilyProfileServiceImpl implements FamilyProfileService {

    private static final Set<ProfileIncludeEntity> ALL_INCLUDES = Collections.unmodifiableSet(
            EnumSet.allOf(ProfileIncludeEntity.class)
    );

    private static final Set<ProfileIncludeEntity> SUMMARY_INCLUDES = Collections.unmodifiableSet(
            EnumSet.of(ProfileIncludeEntity.PROFILE)
    );

    private final FamilyRestRepository familyRestRepository;
    private final AssetAuthorizationService assetAuthorizationService;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final UserDocumentRepository userDocumentRepository;
    private final EncryptionDecryptionUtil encryptionDecryptionUtil;
    private final FamilyConfigurationService familyConfigurationService;
    private final UserConfiguration userConfiguration;

    /**
     * Primary constructor injecting all persistence, authorization, configuration, and helper components.
     *
     * @param familyRestRepository       REST client for upyog-family-service
     * @param assetAuthorizationService  centralized authorization evaluator
     * @param userRepository             citizen profile repository
     * @param addressRepository          postal address repository
     * @param userDocumentRepository     attached documents repository
     * @param encryptionDecryptionUtil   PII encryption/decryption utility
     * @param familyConfigurationService tenant-level MDMS family configuration service
     * @param userConfiguration          central application configuration
     */
    @Autowired
    public FamilyProfileServiceImpl(FamilyRestRepository familyRestRepository,
                                   AssetAuthorizationService assetAuthorizationService,
                                   UserRepository userRepository,
                                   AddressRepository addressRepository,
                                   UserDocumentRepository userDocumentRepository,
                                   EncryptionDecryptionUtil encryptionDecryptionUtil,
                                   @Autowired(required = false) FamilyConfigurationService familyConfigurationService,
                                   @Autowired(required = false) UserConfiguration userConfiguration) {
        this.familyRestRepository = familyRestRepository;
        this.assetAuthorizationService = assetAuthorizationService;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.userDocumentRepository = userDocumentRepository;
        this.encryptionDecryptionUtil = encryptionDecryptionUtil;
        this.familyConfigurationService = familyConfigurationService;
        this.userConfiguration = userConfiguration;
    }

    /**
     * Backward-compatible constructor without {@link UserConfiguration}.
     */
    public FamilyProfileServiceImpl(FamilyRestRepository familyRestRepository,
                                   AssetAuthorizationService assetAuthorizationService,
                                   UserRepository userRepository,
                                   AddressRepository addressRepository,
                                   UserDocumentRepository userDocumentRepository,
                                   EncryptionDecryptionUtil encryptionDecryptionUtil,
                                   FamilyConfigurationService familyConfigurationService) {
        this(familyRestRepository, assetAuthorizationService, userRepository, addressRepository,
             userDocumentRepository, encryptionDecryptionUtil, familyConfigurationService, null);
    }

    /**
     * Searches complete family household details by familyId, resolving active members
     * from upyog-family-service and enriching each member with demographics, addresses, and documents
     * from egov-user core registry.
     *
     * @param request family profile search request payload
     * @return {@link FamilyProfileResponse} containing the complete family unit aggregate with enriched members
     */
    @Override
    public FamilyProfileResponse searchFamilyProfile(FamilyProfileSearchRequest request) {
        validateRequest(request);

        String familyId = request.getFamilyId().trim();
        String tenantId = request.getTenantId();
        RequestInfo requestInfo = request.getRequestInfo();

        log.info("[FAMILY-PROFILE-SERVICE] Processing family profile search for familyId: {}, tenantId: {}",
                familyId, tenantId);

        // 1. Resolve family aggregate from upyog-family-service
        FamilyDetail familyDetail = familyRestRepository.getFamilyByFamilyId(familyId, tenantId, requestInfo);
        if (familyDetail == null) {
            log.warn("[FAMILY-PROFILE-SERVICE] No family record found in upyog-family-service for familyId: {}", familyId);
            throw new CustomException(UserConstants.ERROR_FAMILY_NOT_FOUND, UserConstants.MSG_FAMILY_NOT_FOUND + ": " + familyId);
        }

        // 2. Authorize caller access: Citizens must belong to the requested family; Employees/Admins have cross-family access
        FamilyAccessContext accessContext = authorizeAccess(familyDetail, requestInfo);

        // 3. Extract unique member citizen UUIDs (including Head of Family)
        List<String> memberUuids = extractMemberUuids(familyDetail);

        if (!memberUuids.isEmpty()) {
            String stateTenant = resolveStateTenant(tenantId != null ? tenantId : familyDetail.getTenantId());

            // 4. Batch lookup and decrypt citizen profiles
            Map<String, User> userMap = batchFetchAndDecryptUsers(memberUuids, stateTenant, requestInfo);

            // 5. Batch pre-fetch related postal addresses and uploaded KYC documents
            batchEnrichAddressesAndDocuments(userMap, memberUuids, stateTenant);

            // 6. Enrich member aggregates with profiles (enforcing data minimisation for non-HoF callers)
            enrichFamilyMembers(familyDetail, userMap, accessContext);

            // 8. Enrich headName on family aggregate if missing
            if (StringUtils.isBlank(familyDetail.getHeadName()) && StringUtils.isNotBlank(familyDetail.getHeadUserUuid())) {
                User headUser = userMap.get(familyDetail.getHeadUserUuid());
                if (headUser != null && StringUtils.isNotBlank(headUser.getName())) {
                    familyDetail.setHeadName(headUser.getName());
                }
            }
        }

        Long ts = (requestInfo != null && requestInfo.getTs() != null)
                ? requestInfo.getTs()
                : System.currentTimeMillis();
        ResponseInfo responseInfo = ResponseInfo.builder()
                .apiId(requestInfo != null ? requestInfo.getApiId() : null)
                .ver(requestInfo != null ? requestInfo.getVer() : null)
                .ts(ts)
                .resMsgId(UUID.randomUUID().toString())
                .msgId(requestInfo != null ? requestInfo.getMsgId() : null)
                .status(UserConstants.STATUS_SUCCESS)
                .build();

        return FamilyProfileResponse.builder()
                .responseInfo(responseInfo)
                .family(familyDetail)
                .build();
    }

    private void validateRequest(FamilyProfileSearchRequest request) {
        if (request == null) {
            throw new CustomException(UserConstants.ERROR_INVALID_SEARCH_REQUEST, "Search request cannot be null");
        }
        if (request.getRequestInfo() == null) {
            throw new CustomException(UserConstants.ERROR_INVALID_REQUEST, "RequestInfo is mandatory");
        }
        if (StringUtils.isBlank(request.getFamilyId())) {
            throw new CustomException(UserConstants.ERROR_FAMILY_ID_REQUIRED, "familyId is mandatory for family profile search");
        }
    }

    private FamilyAccessContext authorizeAccess(FamilyDetail familyDetail, RequestInfo requestInfo) {
        org.egov.common.contract.request.User callerInfo = requestInfo != null ? requestInfo.getUserInfo() : null;
        if (callerInfo == null || StringUtils.isBlank(callerInfo.getUuid())) {
            log.warn("[FAMILY-PROFILE-SERVICE] Access denied: RequestInfo.userInfo is missing or blank");
            throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_ACCESS, "Unauthorized: Caller userInfo is mandatory");
        }

        String callerUuid = assetAuthorizationService.resolveCallerUuid(requestInfo);
        String userType = callerInfo.getType();

        // 1. Privileged Employee / System caller authorization
        boolean isEmployeeOrSystem = UserType.EMPLOYEE.name().equalsIgnoreCase(userType)
                || UserType.SYSTEM.name().equalsIgnoreCase(userType);

        if (isEmployeeOrSystem) {
            Set<String> dedicatedRoles = userConfiguration.getFamilyPrivilegedRoles();

            String matchingRole = null;
            if (callerInfo.getRoles() != null) {
                for (Role role : callerInfo.getRoles()) {
                    if (role != null && StringUtils.isNotBlank(role.getCode())
                            && dedicatedRoles.contains(role.getCode().trim().toUpperCase())) {
                        matchingRole = role.getCode().trim().toUpperCase();
                        break;
                    }
                }
            }

            if (matchingRole == null) {
                log.warn("[FAMILY-PROFILE-SERVICE] Access denied: Caller {} ({}) lacks dedicated family access role. Roles: {}",
                        callerUuid, userType, callerInfo.getRoles());
                throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_ACCESS,
                        "Unauthorized: Caller cannot access family profile without dedicated authorization");
            }

            // Employee Tenant Check
            String employeeTenant = callerInfo.getTenantId();
            String familyTenant = familyDetail.getTenantId();
            if (!isTenantAllowed(employeeTenant, familyTenant)) {
                log.warn("[FAMILY-PROFILE-SERVICE] Access denied: Employee {} (tenant: {}) attempted access to family {} in tenant {}",
                        callerUuid, employeeTenant, familyDetail.getFamilyId(), familyTenant);
                throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_ACCESS,
                        "Unauthorized: Tenant mismatch between employee and family");
            }

            // Audit log entry: who viewed which family
            log.info("[FAMILY-PROFILE-AUDIT] Privileged employee {} (tenant: {}, role: {}) viewed details for family: {} (tenant: {})",
                    callerUuid, employeeTenant, matchingRole, familyDetail.getFamilyId(), familyTenant);

            return new FamilyAccessContext(true, callerUuid);
        }

        // 2. Citizen caller authorization
        boolean isHead = callerUuid.equalsIgnoreCase(familyDetail.getHeadUserUuid());
        FamilyMemberDetail callerMember = familyDetail.getMembers() != null ? familyDetail.getMembers().stream()
                .filter(m -> m != null && callerUuid.equalsIgnoreCase(m.getUserUuid()))
                .findFirst()
                .orElse(null) : null;

        boolean isMemberActive = callerMember != null
                && Boolean.TRUE.equals(callerMember.getActive())
                && UserConstants.STATUS_ACTIVE_UPPERCASE.equalsIgnoreCase(callerMember.getStatus());

        if (isHead) {
            // If HoF exists in member list, ensure not deactivated or rejected
            if (callerMember != null && (Boolean.FALSE.equals(callerMember.getActive())
                    || "INACTIVE".equalsIgnoreCase(callerMember.getStatus())
                    || "REJECTED".equalsIgnoreCase(callerMember.getStatus()))) {
                log.warn("[FAMILY-PROFILE-SERVICE] Access denied: HoF caller {} is marked inactive/rejected in family {}",
                        callerUuid, familyDetail.getFamilyId());
                throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_ACCESS,
                        "Unauthorized: You are not authorized to view information for family " + familyDetail.getFamilyId());
            }
            return new FamilyAccessContext(true, callerUuid);
        }

        if (!isMemberActive) {
            log.warn("[FAMILY-PROFILE-SERVICE] Access denied: Citizen caller {} is not an active member of family {}",
                    callerUuid, familyDetail.getFamilyId());
            throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_ACCESS,
                    "Unauthorized: You are not authorized to view information for family " + familyDetail.getFamilyId());
        }

        // Citizen non-HoF active member: allowed access with data minimisation (full view for self only)
        return new FamilyAccessContext(false, callerUuid);
    }

    private boolean isTenantAllowed(String employeeTenant, String familyTenant) {
        if (StringUtils.isBlank(employeeTenant) || StringUtils.isBlank(familyTenant)) {
            return false;
        }
        String empTrim = employeeTenant.trim();
        String famTrim = familyTenant.trim();

        if (empTrim.equalsIgnoreCase(famTrim)) {
            return true;
        }

        if (famTrim.toLowerCase().startsWith(empTrim.toLowerCase() + ".")) {
            return true;
        }

        return false;
    }

    private List<String> extractMemberUuids(FamilyDetail familyDetail) {
        List<String> uuids = new ArrayList<>();
        if (familyDetail.getMembers() != null) {
            for (FamilyMemberDetail member : familyDetail.getMembers()) {
                if (member != null && StringUtils.isNotBlank(member.getUserUuid())) {
                    uuids.add(member.getUserUuid().trim());
                }
            }
        }
        if (StringUtils.isNotBlank(familyDetail.getHeadUserUuid()) && !uuids.contains(familyDetail.getHeadUserUuid().trim())) {
            uuids.add(familyDetail.getHeadUserUuid().trim());
        }
        return uuids.stream().distinct().collect(Collectors.toList());
    }

    private Map<String, User> batchFetchAndDecryptUsers(List<String> memberUuids, String stateTenant, RequestInfo requestInfo) {
        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .tenantId(stateTenant)
                .uuid(memberUuids)
                .type(UserType.CITIZEN)
                .excludeAddressDetails(true)
                .build();

        if (encryptionDecryptionUtil != null) {
            try {
                UserSearchCriteria encryptedCriteria = encryptionDecryptionUtil.encryptObject(criteria, UserConstants.USER_ENCRYPTION_KEY, UserSearchCriteria.class);
                if (encryptedCriteria != null) {
                    criteria = encryptedCriteria;
                }
            } catch (Exception e) {
                log.warn("[FAMILY-PROFILE-SERVICE] Search criteria encryption skipped: {}", e.getMessage());
            }
        }

        List<User> userList = userRepository.findAll(criteria);
        if (CollectionUtils.isEmpty(userList)) {
            return Collections.emptyMap();
        }

        Map<String, User> userMap = new HashMap<>();
        for (User matchedUser : userList) {
            if (matchedUser == null || StringUtils.isBlank(matchedUser.getUuid())) {
                continue;
            }
            User decrypted = matchedUser;
            if (encryptionDecryptionUtil != null) {
                try {
                    decrypted = encryptionDecryptionUtil.decryptObject(matchedUser, UserConstants.USER_ENCRYPTION_KEY, User.class, requestInfo);
                    if (decrypted == null) {
                        decrypted = matchedUser;
                    }
                } catch (Exception e) {
                    log.warn("[FAMILY-PROFILE-SERVICE] Decryption failed for user {}: {}", matchedUser.getUuid(), e.getMessage());
                    decrypted = matchedUser;
                }
            }
            userMap.put(decrypted.getUuid(), decrypted);
        }

        return userMap;
    }

    private void batchEnrichAddressesAndDocuments(Map<String, User> userMap, List<String> memberUuids, String stateTenant) {
        if (userMap.isEmpty() || memberUuids.isEmpty()) {
            return;
        }

        // Batch pre-fetch active postal addresses
        try {
            AddressSearchCriteria addressCriteria = AddressSearchCriteria.builder()
                    .userUuids(memberUuids)
                    .status(UserConstants.ADDRESS_ACTIVE_STATUS)
                    .build();
            List<Address> addresses = addressRepository.getAddressV2(addressCriteria);
            if (!CollectionUtils.isEmpty(addresses)) {
                Map<String, List<Address>> addressByUser = new HashMap<>();
                for (Address addr : addresses) {
                    if (addr != null && StringUtils.isNotBlank(addr.getUserUuid())) {
                        addressByUser.computeIfAbsent(addr.getUserUuid(), k -> new ArrayList<>()).add(addr);
                    }
                }
                for (Map.Entry<String, List<Address>> entry : addressByUser.entrySet()) {
                    User u = userMap.get(entry.getKey());
                    if (u != null) {
                        u.setAddresses(new LinkedHashSet<>(entry.getValue()));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[FAMILY-PROFILE-SERVICE] Non-blocking: Batch address retrieval encountered: {}", e.getMessage());
        }

        // Batch pre-fetch active KYC documents
        try {
            UserDocumentSearchCriteria docCriteria = UserDocumentSearchCriteria.builder()
                    .userUuids(memberUuids)
                    .tenantId(stateTenant)
                    .active(Boolean.TRUE)
                    .build();
            List<UserDocument> documents = userDocumentRepository.search(docCriteria);
            if (!CollectionUtils.isEmpty(documents)) {
                Map<String, List<UserDocument>> docsByUser = new HashMap<>();
                for (UserDocument doc : documents) {
                    if (doc != null && StringUtils.isNotBlank(doc.getUserUuid())) {
                        docsByUser.computeIfAbsent(doc.getUserUuid(), k -> new ArrayList<>()).add(doc);
                    }
                }
                for (Map.Entry<String, List<UserDocument>> entry : docsByUser.entrySet()) {
                    User u = userMap.get(entry.getKey());
                    if (u != null) {
                        u.setDocuments(entry.getValue());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[FAMILY-PROFILE-SERVICE] Non-blocking: Batch document retrieval encountered: {}", e.getMessage());
        }
    }

    private void enrichFamilyMembers(FamilyDetail familyDetail,
                                     Map<String, User> userMap,
                                     FamilyAccessContext accessContext) {
        if (familyDetail.getMembers() == null) {
            return;
        }

        boolean allowFullViewAll = accessContext != null && accessContext.isFullView();
        String callerUuid = accessContext != null ? accessContext.getCallerUuid() : null;

        for (FamilyMemberDetail member : familyDetail.getMembers()) {
            if (member == null) {
                continue;
            }
            String userUuid = member.getUserUuid();
            User user = userMap.get(userUuid);

            boolean isCallerSelf = callerUuid != null && callerUuid.equalsIgnoreCase(userUuid);
            boolean canViewFull = allowFullViewAll || isCallerSelf;

            if (user != null) {
                if (canViewFull) {
                    user.setFamilyId(familyDetail.getFamilyId());

                    UserSearchResponseContent profileContent = new UserSearchResponseContent(user, ALL_INCLUDES);
                    member.setProfile(profileContent);
                } else {
                    // Data minimisation: return only summary profile for other family members
                    UserSearchResponseContent summaryContent = new UserSearchResponseContent(user, SUMMARY_INCLUDES);
                    member.setProfile(summaryContent);
                }
            }
        }
    }

    private String resolveStateTenant(String tenantId) {
        if (StringUtils.isNotBlank(tenantId)) {
            return tenantId.contains(".") ? tenantId.split("\\.")[0] : tenantId.trim();
        }

        String stateTenant = userConfiguration.getStateLevelTenantId();
        if (StringUtils.isNotBlank(stateTenant)) {
            return stateTenant.trim();
        }

        log.error("[FAMILY-PROFILE-SERVICE] Tenant ID cannot be resolved: neither request/family tenantId nor state.level.tenant.id configured");
        throw new CustomException(UserConstants.ERROR_TENANT_ID_REQUIRED,
                UserConstants.MSG_TENANT_ID_UNDETERMINED);
    }

    /**
     * Authorization context holding evaluated caller scope and permissions for family profile search.
     */
    private static class FamilyAccessContext {
        private final boolean fullView;
        private final String callerUuid;

        public FamilyAccessContext(boolean fullView, String callerUuid) {
            this.fullView = fullView;
            this.callerUuid = callerUuid;
        }

        public boolean isFullView() {
            return fullView;
        }

        public String getCallerUuid() {
            return callerUuid;
        }
    }
}
