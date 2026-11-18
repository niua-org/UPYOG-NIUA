package org.upyog.family.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.upyog.family.config.FamilyConfiguration;
import org.upyog.family.domain.model.MemberStatus;
import org.upyog.family.domain.validator.AddMemberValidator;
import org.upyog.family.domain.validator.JoinFamilyValidator;
import org.upyog.family.domain.validator.LeaveFamilyValidator;
import org.upyog.family.persistence.repository.FamilyRepository;
import org.upyog.family.persistence.repository.IdGenRepository;
import org.upyog.family.persistence.repository.UserClient;
import org.upyog.family.util.FamilyConstants;
import org.upyog.family.util.KafkaProducer;
import org.upyog.family.web.contract.AddMemberRequest;
import org.upyog.family.web.contract.AuditDetails;
import org.upyog.family.web.contract.CitizenProfile;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyMember;
import org.upyog.family.web.contract.FamilyRequest;
import org.upyog.family.web.contract.FamilySearchCriteria;
import org.upyog.family.web.contract.JoinFamilyRequest;
import org.upyog.family.web.contract.LeaveFamilyRequest;
import org.upyog.family.web.contract.UserIncomeDetail;
import org.upyog.family.web.contract.UserProperty;
import org.upyog.family.web.contract.UserVehicleDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Domain service implementation orchestrating member addition, joining, and leaving flows in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
/**
 * Domain service implementation managing individual family member lifecycle events in UPYOG.
 *
 * <p>Implements {@link FamilyMemberService} handling member additions with Aadhaar consent, direct member join flows
 * with HoF authorization, and soft-deactivations for member departure (leave). All operations enforce transactional
 * integrity and dispatch persistence events to Kafka.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class FamilyMemberServiceImpl implements FamilyMemberService {

    private final FamilyRepository familyRepository;
    private final AddMemberValidator addMemberValidator;
    private final JoinFamilyValidator joinFamilyValidator;
    private final LeaveFamilyValidator leaveFamilyValidator;
    private final UserClient userClient;
    private final KafkaProducer kafkaProducer;
    private final FamilyConfiguration configuration;
    private final IdGenRepository idGenRepository;

    /**
     * Constructs {@link FamilyMemberServiceImpl} with all required repository and validator dependencies.
     *
     * @param familyRepository data repository for family lookups and queries
     * @param addMemberValidator validator verifying consent and residency rules for adding members
     * @param joinFamilyValidator validator checking applicant join tokens and exclusivity
     * @param leaveFamilyValidator validator enforcing departure policies and HoF preservation
     * @param userClient client communicating with {@code egov-user} for demographic lookups
     * @param kafkaProducer asynchronous messaging producer for DB persistence
     * @param configuration service configuration properties
     * @param idGenRepository idgen repository for generating formatted member IDs
     */
    @Autowired
    public FamilyMemberServiceImpl(FamilyRepository familyRepository,
                                  AddMemberValidator addMemberValidator,
                                  JoinFamilyValidator joinFamilyValidator,
                                  LeaveFamilyValidator leaveFamilyValidator,
                                  UserClient userClient,
                                  KafkaProducer kafkaProducer,
                                  FamilyConfiguration configuration,
                                  @Autowired(required = false) IdGenRepository idGenRepository) {
        this.familyRepository = familyRepository;
        this.addMemberValidator = addMemberValidator;
        this.joinFamilyValidator = joinFamilyValidator;
        this.leaveFamilyValidator = leaveFamilyValidator;
        this.userClient = userClient;
        this.kafkaProducer = kafkaProducer;
        this.configuration = configuration;
        this.idGenRepository = idGenRepository;
    }

    /**
     * Constructs {@link FamilyMemberServiceImpl} without optional IdGenRepository for testing/backwards compatibility.
     */
    public FamilyMemberServiceImpl(FamilyRepository familyRepository,
                                  AddMemberValidator addMemberValidator,
                                  JoinFamilyValidator joinFamilyValidator,
                                  LeaveFamilyValidator leaveFamilyValidator,
                                  UserClient userClient,
                                  KafkaProducer kafkaProducer,
                                  FamilyConfiguration configuration) {
        this(familyRepository, addMemberValidator, joinFamilyValidator, leaveFamilyValidator, userClient, kafkaProducer, configuration, null);
    }

    /**
     * Adds a new verified member to an existing family unit with verified Aadhaar OTP consent in UPYOG.
     *
     * <p>If the member does not yet have a user UUID, searches {@code egov-user} by mobile number or auto-provisions
     * a new citizen profile. Verifies multi-family exclusivity, hashes the consent token, updates audit records,
     * and publishes the update to Kafka topic {@code configuration.getUpdateFamilyTopic()}.</p>
     *
     * @param request member addition payload containing family ID, member attributes, and consent token; must not be {@code null}.
     * @return the updated {@link Family} aggregate populated with the newly added member.
     * @throws CustomException if target family does not exist, caller is unauthorized, or candidate is already in an active family.
     */
    @Override
    @Transactional
    public Family addMember(AddMemberRequest request) {
        log.info("[UPYOG-MEMBER-SERVICE] Adding member to familyId: {}, tenant: {}",
                request.getFamilyId(), request.getTenantId());

        Family existingFamily = getFamilyByIdAndTenant(request.getFamilyId(), request.getTenantId());
        addMemberValidator.validate(request, existingFamily);

        RequestInfo requestInfo = request.getRequestInfo();
        String userUuid = requestInfo != null && requestInfo.getUserInfo() != null
                ? requestInfo.getUserInfo().getUuid()
                : FamilyConstants.SYSTEM_USER;
        Long now = System.currentTimeMillis();

        FamilyMember newMember = request.getMember();
        if (StringUtils.isBlank(newMember.getUserUuid()) && StringUtils.isNotBlank(newMember.getMobileNumber())) {
            List<CitizenProfile> profiles = userClient.searchUsersByMobile(newMember.getMobileNumber(), request.getTenantId(), requestInfo);
            if (profiles != null && !profiles.isEmpty()) {
                newMember.setUserUuid(profiles.get(0).getUuid());
            } else {
                CitizenProfile created = userClient.createCitizenProfile(
                        CitizenProfile.builder()
                                .mobileNumber(newMember.getMobileNumber())
                                .name(newMember.getName())
                                .gender(newMember.getGender())
                                .build(),
                        requestInfo);
                if (created == null || StringUtils.isBlank(created.getUuid())) {
                    throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "Unable to create citizen profile for new member");
                }
                newMember.setUserUuid(created.getUuid());
            }

            // Verify exclusivity after resolving user UUID
            if (existingFamily.getMembers() != null) {
                boolean alreadyActive = existingFamily.getMembers().stream()
                        .anyMatch(m -> newMember.getUserUuid().equals(m.getUserUuid()) && Boolean.TRUE.equals(m.getActive()));
                if (alreadyActive) {
                    throw new CustomException(FamilyConstants.ERROR_MEMBER_ALREADY_EXISTS,
                            "Citizen is already an active member of family " + existingFamily.getFamilyId());
                }
            }
            List<String> activeFamilyIds = familyRepository.findActiveFamilyIdsByMemberUuid(newMember.getUserUuid());
            for (String activeFamId : activeFamilyIds) {
                if (!activeFamId.equals(existingFamily.getFamilyId())) {
                    throw new CustomException(FamilyConstants.ERROR_MEMBER_ALREADY_EXISTS,
                            "Citizen " + newMember.getUserUuid() + " already belongs to active family: " + activeFamId);
                }
            }
        }

        if (StringUtils.isBlank(newMember.getId())) {
            newMember.setId(generateMemberIdOrDefault(requestInfo, existingFamily.getTenantId()));
        }
        newMember.setFamilyId(existingFamily.getFamilyId());
        newMember.setTenantId(existingFamily.getTenantId());
        boolean verified = StringUtils.isNotBlank(request.getConsentToken())
                || Boolean.TRUE.equals(newMember.getIsVerified());
        newMember.setIsVerified(verified);
        newMember.setActive(true);
        newMember.setStatus(MemberStatus.ACTIVE);
        newMember.setConsentToken(maskOrHashConsentToken(request.getConsentToken()));
        newMember.setConsentDate(now);
        newMember.setEffectiveFrom(now);
        newMember.setAuditDetails(AuditDetails.builder()
                .createdBy(userUuid)
                .createdTime(now)
                .lastModifiedBy(userUuid)
                .lastModifiedTime(now)
                .build());

        // Fix for PR parity with createFamily / 3NF member assets:
        // Ensure userUuid and tenantId are properly populated on declared properties, income, and vehicles
        enrichMemberAssets(newMember, existingFamily.getTenantId());

        // Synchronize added member profile demographics and declared 3NF assets to egov-user BEFORE Kafka push.
        // This ensures asset records (properties, income, vehicles) exist in egov-user before the family update event
        // is queued to Kafka persister. If failOnAssetSyncError is enabled (default true) and assets are declared,
        // aborts transaction to prevent unattached or orphaned asset declarations.
        boolean hasAddedAssets = hasDeclaredAssets(newMember);
        log.info("[UPYOG-MEMBER-SERVICE] Synchronizing added member assets to egov-user for family: {}, hasDeclaredAssets: {}",
                existingFamily.getFamilyId(), hasAddedAssets);
        boolean addedSyncSuccess = userClient.syncMemberAssets(Collections.singletonList(newMember), existingFamily.getTenantId(), requestInfo);
        if (!addedSyncSuccess) {
            log.error("[UPYOG-MEMBER-SERVICE] Member asset synchronization failed for added member: {} in family: {}",
                    newMember.getUserUuid(), existingFamily.getFamilyId());
            if (hasAddedAssets && configuration != null && Boolean.TRUE.equals(configuration.getFailOnAssetSyncError())) {
                throw new CustomException(FamilyConstants.ERROR_MEMBER_ASSET_SYNC_FAILED, FamilyConstants.MSG_MEMBER_ASSET_SYNC_FAILED);
            }
        } else {
            log.info("[UPYOG-MEMBER-SERVICE] Successfully synchronized assets for added member: {} in family: {}",
                    newMember.getUserUuid(), existingFamily.getFamilyId());
        }

        if (existingFamily.getMembers() == null) {
            existingFamily.setMembers(new ArrayList<>());
        }
        existingFamily.getMembers().add(newMember);

        existingFamily.getAuditDetails().setLastModifiedBy(userUuid);
        existingFamily.getAuditDetails().setLastModifiedTime(now);

        // Persist to Database via Kafka Persister (standard UPYOG pattern)
        kafkaProducer.push(configuration.getUpdateFamilyTopic(), new FamilyRequest(requestInfo, existingFamily));

        enrichCitizenDemographics(Collections.singletonList(existingFamily), requestInfo);
        log.info("[UPYOG-MEMBER-SERVICE] Successfully queued member {} link to family {} in Kafka persister",
                newMember.getUserUuid(), existingFamily.getFamilyId());

        return existingFamily;
    }

    /**
     * Processes an independent citizen request to join an existing family unit upon Head of Family authorization in UPYOG.
     *
     * <p>Validates applicant credentials, 3NF profile and asset declarations, and the verified HoF consent token via
     * {@link JoinFamilyValidator}. Synchronizes declared assets to {@code egov-user}, adds the applicant to the active
     * members roster with status {@link MemberStatus#JOINED}, pushes the event to Kafka persister, and decorates the
     * family aggregate with demographics from {@code egov-user}.</p>
     *
     * @param request join family request containing applicant UUID, relationship with head, verified HoF consent token, and asset declarations.
     * @return the updated {@link Family} aggregate including the newly joined member.
     * @throws CustomException if target family does not exist, consent is invalid, applicant already belongs to another family, or asset sync fails.
     */
    @Override
    @Transactional
    public Family joinFamily(JoinFamilyRequest request) {
        log.info("[UPYOG-MEMBER-SERVICE] Citizen {} joining family {}",
                request.getApplicantUserUuid(), request.getFamilyId());

        Family existingFamily = getFamilyByIdAndTenant(request.getFamilyId(), request.getTenantId());
        joinFamilyValidator.validate(request, existingFamily);

        RequestInfo requestInfo = request.getRequestInfo();
        String userUuid = requestInfo != null && requestInfo.getUserInfo() != null
                ? requestInfo.getUserInfo().getUuid()
                : FamilyConstants.SYSTEM_USER;
        Long now = System.currentTimeMillis();

        // Citizen demographics (name, mobile, gender, dob, photo) are derived from citizenDetails as the Single Source of Truth (3NF)
        CitizenProfile profile = request.getCitizenDetails();

        // Construct joined member aggregate binding 3NF asset declarations (properties, income, vehicles)
        FamilyMember joinedMember = FamilyMember.builder()
                .id(generateMemberIdOrDefault(requestInfo, existingFamily.getTenantId()))
                .familyId(existingFamily.getFamilyId())
                .tenantId(existingFamily.getTenantId())
                .userUuid(request.getApplicantUserUuid())
                .name(profile != null && StringUtils.isNotBlank(profile.getName()) ? profile.getName() : null)
                .mobileNumber(profile != null && StringUtils.isNotBlank(profile.getMobileNumber()) ? profile.getMobileNumber() : null)
                .gender(profile != null && StringUtils.isNotBlank(profile.getGender()) ? profile.getGender() : null)
                .dob(profile != null ? profile.getDob() : null)
                .photo(profile != null ? profile.getPhoto() : null)
                .relationshipWithHead(request.getRelationshipWithHead())
                .isHead(false)
                .isVerified(StringUtils.isNotBlank(request.getTxnId()))
                .active(true)
                .status(MemberStatus.JOINED)
                .consentToken(maskOrHashConsentToken(request.getTxnId()))
                .consentDate(now)
                .effectiveFrom(now)
                .citizenDetails(profile)
                .properties(request.getProperties() != null ? request.getProperties() : new ArrayList<>())
                .income(request.getIncome())
                .vehicles(request.getVehicles() != null ? request.getVehicles() : new ArrayList<>())
                .auditDetails(AuditDetails.builder()
                        .createdBy(userUuid)
                        .createdTime(now)
                        .lastModifiedBy(userUuid)
                        .lastModifiedTime(now)
                        .build())
                .build();

        // Propagate citizen userUuid and tenantId across declared properties, income, and vehicles
        enrichMemberAssets(joinedMember, existingFamily.getTenantId());

        // Synchronize joined member profile and declared 3NF assets to egov-user BEFORE Kafka push.
        // Adheres to the createFamily architecture: ensures 3NF records in egov-user are updated synchronously
        // before the family membership record is committed to Kafka persister topic.
        boolean hasAssets = hasDeclaredAssets(joinedMember);
        log.info("[UPYOG-MEMBER-SERVICE] Synchronizing joined member assets to egov-user for family: {}, hasDeclaredAssets: {}",
                existingFamily.getFamilyId(), hasAssets);
        boolean syncSuccess = userClient.syncMemberAssets(Collections.singletonList(joinedMember), existingFamily.getTenantId(), requestInfo);
        if (!syncSuccess) {
            log.error("[UPYOG-MEMBER-SERVICE] Member asset synchronization failed for joined member: {} in family: {}",
                    joinedMember.getUserUuid(), existingFamily.getFamilyId());
            if (hasAssets && configuration != null && Boolean.TRUE.equals(configuration.getFailOnAssetSyncError())) {
                throw new CustomException(FamilyConstants.ERROR_MEMBER_ASSET_SYNC_FAILED, FamilyConstants.MSG_MEMBER_ASSET_SYNC_FAILED);
            }
        } else {
            log.info("[UPYOG-MEMBER-SERVICE] Successfully synchronized assets for joined member: {} in family: {}",
                    joinedMember.getUserUuid(), existingFamily.getFamilyId());
        }

        if (existingFamily.getMembers() == null) {
            existingFamily.setMembers(new ArrayList<>());
        }
        existingFamily.getMembers().add(joinedMember);

        existingFamily.getAuditDetails().setLastModifiedBy(userUuid);
        existingFamily.getAuditDetails().setLastModifiedTime(now);

        // Persist to Database via Kafka Persister (standard UPYOG pattern)
        kafkaProducer.push(configuration.getUpdateFamilyTopic(), new FamilyRequest(requestInfo, existingFamily));

        enrichCitizenDemographics(Collections.singletonList(existingFamily), requestInfo);
        log.info("[UPYOG-MEMBER-SERVICE] Successfully queued joined member {} to family {} in Kafka persister",
                joinedMember.getUserUuid(), existingFamily.getFamilyId());

        return existingFamily;
    }

    /**
     * Processes a member exit from a family unit due to marriage, demise, or relocation in UPYOG.
     *
     * <p>Enforces authorization (must be self or family HoF) and validates via {@link LeaveFamilyValidator}
     * that a sole Head of Family cannot leave without designating a successor. Soft-deactivates the member,
     * updates the audit timestamps, and dispatches an update event to the Kafka persister topic.</p>
     *
     * @param request leave family request containing family ID, member UUID, and exit reason; must not be {@code null}.
     * @return the updated {@link Family} aggregate reflecting the member's deactivated status.
     * @throws CustomException if family or member is not found, or departing member is the sole HoF.
     */
    @Override
    @Transactional
    public Family leaveFamily(LeaveFamilyRequest request) {
        log.info("[UPYOG-MEMBER-SERVICE] Member {} leaving family {}",
                request.getMemberUserUuid(), request.getFamilyId());

        Family existingFamily = getFamilyByIdAndTenant(request.getFamilyId(), request.getTenantId());
        leaveFamilyValidator.validate(request, existingFamily);

        RequestInfo requestInfo = request.getRequestInfo();
        String userUuid = requestInfo != null && requestInfo.getUserInfo() != null
                ? requestInfo.getUserInfo().getUuid()
                : FamilyConstants.SYSTEM_USER;
        Long now = System.currentTimeMillis();

        for (FamilyMember member : existingFamily.getMembers()) {
            if (request.getMemberUserUuid().equals(member.getUserUuid()) && Boolean.TRUE.equals(member.getActive())) {
                member.setActive(false);
                member.setStatus(MemberStatus.EXITED);
                member.setExitReason(request.getExitReason());
                member.setExitDate(request.getExitDate() != null ? request.getExitDate() : now);
                member.setEffectiveTo(now);
                if (member.getAuditDetails() == null) {
                    member.setAuditDetails(AuditDetails.builder().createdBy(userUuid).createdTime(now).build());
                }
                member.getAuditDetails().setLastModifiedBy(userUuid);
                member.getAuditDetails().setLastModifiedTime(now);
                break;
            }
        }

        existingFamily.getAuditDetails().setLastModifiedBy(userUuid);
        existingFamily.getAuditDetails().setLastModifiedTime(now);

        // Persist to Database via Kafka Persister (standard UPYOG pattern)
        kafkaProducer.push(configuration.getUpdateFamilyTopic(), new FamilyRequest(requestInfo, existingFamily));

        enrichCitizenDemographics(Collections.singletonList(existingFamily), requestInfo);
        log.info("[UPYOG-MEMBER-SERVICE] Successfully queued exit for member {} from family {} in Kafka persister",
                request.getMemberUserUuid(), existingFamily.getFamilyId());

        return existingFamily;
    }

    /**
     * Retrieves a Family aggregate by familyId and tenantId, throwing a CustomException if not found.
     *
     * @param familyId unique family identifier
     * @param tenantId tenant identifier representing jurisdiction
     * @return Family aggregate
     * @throws CustomException if family is not found (Code: {@code FAMILY_NOT_FOUND})
     */
    private Family getFamilyByIdAndTenant(String familyId, String tenantId) {
        return familyRepository.findByFamilyIdAndTenantId(familyId, tenantId)
                .orElseThrow(() -> new CustomException(FamilyConstants.ERROR_FAMILY_NOT_FOUND,
                        "Family with ID " + familyId + " not found."));
    }

    /**
     * Enriches family members with live citizen demographic data retrieved from egov-user service.
     *
     * @param families list of Family domain aggregates to enrich
     * @param requestInfo UPYOG request context envelope
     */
    private void enrichCitizenDemographics(List<Family> families, RequestInfo requestInfo) {
        if (families == null || families.isEmpty()) {
            return;
        }

        List<String> allMemberUuids = families.stream()
                .filter(f -> f.getMembers() != null)
                .flatMap(f -> f.getMembers().stream())
                .map(FamilyMember::getUserUuid)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());

        if (allMemberUuids.isEmpty()) {
            return;
        }

        Map<String, CitizenProfile> profileMap = userClient.searchUsersByUuids(allMemberUuids, requestInfo);
        for (Family family : families) {
            if (family.getMembers() != null) {
                for (FamilyMember member : family.getMembers()) {
                    if (profileMap.containsKey(member.getUserUuid())) {
                        member.enrichFromCitizenProfile(profileMap.get(member.getUserUuid()));
                    }
                }
                family.enrichHeadName();
            }
        }
    }

    /**
     * Resolves an IDGen-formatted member ID or falls back to standard UUID if IDGen is unconfigured or unavailable.
     *
     * @param requestInfo request context
     * @param tenantId    tenant identifier
     * @return generated member identifier
     */
    private String generateMemberIdOrDefault(RequestInfo requestInfo, String tenantId) {
        if (idGenRepository != null) {
            try {
                String memberId = idGenRepository.generateMemberId(requestInfo, tenantId);
                if (StringUtils.isNotBlank(memberId)) {
                    return memberId;
                }
            } catch (Exception e) {
                log.warn("[UPYOG-MEMBER-SERVICE] IdGen failed to generate member ID, falling back to UUID: {}", e.getMessage());
            }
        }
        return UUID.randomUUID().toString();
    }

    /**
     * Sanitizes or hashes raw OTP tokens to prevent leaking plain OTP codes in persistent storage.
     *
     * @param token raw consent token or OTP string
     * @return sanitized reference token or masked hash string
     */
    private String maskOrHashConsentToken(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }
        if (token.startsWith("REF-") || token.startsWith("DX-") || token.length() > 20) {
            return token;
        }
        return "CONSENT-REF-" + Integer.toHexString(token.hashCode()).toUpperCase();
    }

    /**
     * Checks if an individual family member declares properties, vehicles, income, or demographic proof documents.
     *
     * @param m family member contract
     * @return true if member contains declared assets or demographic documents
     */
    private boolean hasDeclaredAssets(FamilyMember m) {
        if (m == null) {
            return false;
        }
        if (m.getCitizenDetails() != null) {
            if (m.getCitizenDetails().getDocuments() != null && !m.getCitizenDetails().getDocuments().isEmpty()) {
                return true;
            }
            if (StringUtils.isNotBlank(m.getCitizenDetails().getCaste())) {
                return true;
            }
            if (m.getCitizenDetails().getIsSpeciallyAbled() != null) {
                return true;
            }
        }
        if (m.getProperties() != null && !m.getProperties().isEmpty()) {
            return true;
        }
        if (m.getIncome() != null) {
            return true;
        }
        if (m.getVehicles() != null && !m.getVehicles().isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * Enriches declared member assets with citizen user UUID and tenant ID where absent.
     *
     * @param member target family member
     * @param tenantId common tenant identifier
     */
    private void enrichMemberAssets(FamilyMember member, String tenantId) {
        if (member == null) {
            return;
        }
        String userUuid = member.getUserUuid();
        if (member.getProperties() != null) {
            for (UserProperty prop : member.getProperties()) {
                if (StringUtils.isBlank(prop.getUserUuid())) {
                    prop.setUserUuid(userUuid);
                }
                if (StringUtils.isBlank(prop.getTenantId())) {
                    prop.setTenantId(tenantId);
                }
            }
        }
        if (member.getIncome() != null) {
            if (StringUtils.isBlank(member.getIncome().getUserUuid())) {
                member.getIncome().setUserUuid(userUuid);
            }
            if (StringUtils.isBlank(member.getIncome().getTenantId())) {
                member.getIncome().setTenantId(tenantId);
            }
        }
        if (member.getVehicles() != null) {
            for (UserVehicleDetail vehicle : member.getVehicles()) {
                if (StringUtils.isBlank(vehicle.getUserUuid())) {
                    vehicle.setUserUuid(userUuid);
                }
                if (StringUtils.isBlank(vehicle.getTenantId())) {
                    vehicle.setTenantId(tenantId);
                }
            }
        }
    }
}
