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

    @Autowired
    public FamilyMemberServiceImpl(FamilyRepository familyRepository,
                                  AddMemberValidator addMemberValidator,
                                  JoinFamilyValidator joinFamilyValidator,
                                  LeaveFamilyValidator leaveFamilyValidator,
                                  UserClient userClient,
                                  KafkaProducer kafkaProducer,
                                  FamilyConfiguration configuration) {
        this.familyRepository = familyRepository;
        this.addMemberValidator = addMemberValidator;
        this.joinFamilyValidator = joinFamilyValidator;
        this.leaveFamilyValidator = leaveFamilyValidator;
        this.userClient = userClient;
        this.kafkaProducer = kafkaProducer;
        this.configuration = configuration;
    }

    /**
     * Adds a new verified member to an existing family unit with verified Aadhaar OTP consent in UPYOG.
     * Validates consent token, auto-looks up or provisions citizen profile in the user registry, and links member.
     *
     * @param request member addition payload containing family ID, member attributes, and consent token
     * @return updated Family aggregate
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
            newMember.setId(UUID.randomUUID().toString());
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
     * Verifies HoF authorization and registers the applicant as an active member of the family.
     *
     * @param request join family request with applicant UUID and HoF authorization token
     * @return updated Family aggregate
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

        FamilyMember joinedMember = FamilyMember.builder()
                .id(UUID.randomUUID().toString())
                .familyId(existingFamily.getFamilyId())
                .tenantId(existingFamily.getTenantId())
                .userUuid(request.getApplicantUserUuid())
                .relationshipWithHead(request.getRelationshipWithHead())
                .isHead(false)
                .isVerified(StringUtils.isNotBlank(request.getHofConsentOtp()))
                .active(true)
                .status(MemberStatus.ACTIVE)
                .consentToken(maskOrHashConsentToken(request.getHofConsentOtp()))
                .consentDate(now)
                .effectiveFrom(now)
                .auditDetails(AuditDetails.builder()
                        .createdBy(userUuid)
                        .createdTime(now)
                        .lastModifiedBy(userUuid)
                        .lastModifiedTime(now)
                        .build())
                .build();

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
     * Sets exit date, exit reason, and status to EXITED, while preventing a solitary Head of Family from leaving.
     *
     * @param request leave family request containing member UUID and exit reason
     * @return updated Family aggregate
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

    private Family getFamilyByIdAndTenant(String familyId, String tenantId) {
        FamilySearchCriteria criteria = FamilySearchCriteria.builder()
                .familyId(familyId)
                .tenantId(tenantId)
                .build();
        List<Family> families = familyRepository.search(criteria);
        if (families.isEmpty()) {
            throw new CustomException(FamilyConstants.ERROR_FAMILY_NOT_FOUND,
                    "Family with ID " + familyId + " not found.");
        }
        return families.get(0);
    }

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
                        member.setCitizenDetails(profileMap.get(member.getUserUuid()));
                    }
                }
            }
        }
    }

    private String maskOrHashConsentToken(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }
        if (token.startsWith("REF-") || token.startsWith("DX-") || token.length() > 20) {
            return token;
        }
        return "CONSENT-REF-" + Integer.toHexString(token.hashCode()).toUpperCase();
    }
}
