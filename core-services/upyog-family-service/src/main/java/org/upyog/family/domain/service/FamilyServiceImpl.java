package org.upyog.family.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.upyog.family.config.FamilyConfiguration;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.domain.model.LineageActionType;
import org.upyog.family.domain.validator.FamilyValidator;
import org.upyog.family.persistence.repository.FamilyRepository;
import org.upyog.family.persistence.repository.IdGenRepository;
import org.upyog.family.persistence.repository.UserClient;
import org.upyog.family.util.FamilyConstants;
import org.upyog.family.util.KafkaProducer;
import org.upyog.family.web.contract.AuditDetails;
import org.upyog.family.web.contract.CitizenProfile;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyDocument;
import org.upyog.family.web.contract.FamilyLineage;
import org.upyog.family.web.contract.FamilyMember;
import org.upyog.family.web.contract.FamilyMergeRequest;
import org.upyog.family.web.contract.FamilyRequest;
import org.upyog.family.web.contract.FamilySearchCriteria;
import org.upyog.family.web.contract.FamilySplitRequest;
import org.egov.tracer.model.CustomException;
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
 * Core business orchestrator for the UPYOG Family Management registry.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class FamilyServiceImpl implements FamilyService {

    private final FamilyRepository familyRepository;
    private final FamilyValidator familyValidator;
    private final IdGenRepository idGenRepository;
    private final UserClient userClient;
    private final KafkaProducer kafkaProducer;
    private final FamilyConfiguration configuration;

    @Autowired
    public FamilyServiceImpl(FamilyRepository familyRepository,
                             FamilyValidator familyValidator,
                             IdGenRepository idGenRepository,
                             UserClient userClient,
                             KafkaProducer kafkaProducer,
                             FamilyConfiguration configuration) {
        this.familyRepository = familyRepository;
        this.familyValidator = familyValidator;
        this.idGenRepository = idGenRepository;
        this.userClient = userClient;
        this.kafkaProducer = kafkaProducer;
        this.configuration = configuration;
    }

    /**
     * Creates a new household Family Unit in UPYOG with sequential ID generation, audit logging, and persistence.
     * Validates that the head of family exists and that members are not active in any other family unit.
     *
     * @param request the family creation request containing metadata, head of family, and members
     * @return the created Family aggregate
     */
    @Override
    @Transactional
    public Family createFamily(FamilyRequest request) {
        log.info("[UPYOG-FAMILY-SERVICE] Creating family for tenant: {}", request.getFamily().getTenantId());

        familyValidator.validateCreate(request);

        Family family = request.getFamily();
        RequestInfo requestInfo = request.getRequestInfo();
        String userUuid = requestInfo != null && requestInfo.getUserInfo() != null
                ? requestInfo.getUserInfo().getUuid()
                : FamilyConstants.SYSTEM_USER;
        Long now = System.currentTimeMillis();

        // 1. Generate formatted family_id
        String familyId = idGenRepository.generateFamilyId(requestInfo, family.getTenantId());
        family.setFamilyId(familyId);
        family.setStatus(FamilyStatus.ACTIVE);

        // 2. Enrich Audit Details
        AuditDetails auditDetails = AuditDetails.builder()
                .createdBy(userUuid)
                .createdTime(now)
                .lastModifiedBy(userUuid)
                .lastModifiedTime(now)
                .build();
        family.setAuditDetails(auditDetails);

        if (StringUtils.isBlank(family.getId())) {
            family.setId(UUID.randomUUID().toString());
        }

        for (FamilyMember member : family.getMembers()) {
            if (StringUtils.isBlank(member.getId())) {
                member.setId(UUID.randomUUID().toString());
            }
            member.setFamilyId(familyId);
            member.setTenantId(family.getTenantId());
            if (member.getActive() == null) {
                member.setActive(true);
            }
            if (member.getEffectiveFrom() == null) {
                member.setEffectiveFrom(now);
            }
            member.setAuditDetails(auditDetails);
        }

        if (family.getDocuments() != null) {
            for (FamilyDocument doc : family.getDocuments()) {
                if (StringUtils.isBlank(doc.getId())) {
                    doc.setId(UUID.randomUUID().toString());
                }
                doc.setFamilyId(familyId);
                doc.setTenantId(family.getTenantId());
                if (doc.getActive() == null) {
                    doc.setActive(true);
                }
                doc.setAuditDetails(auditDetails);
            }
        }

        // 3. Persist to Database via Kafka Persister (standard UPYOG pattern)
        kafkaProducer.push(configuration.getCreateFamilyTopic(), request);

        // 4. Enrich citizen demographics
        enrichCitizenDemographics(Collections.singletonList(family), requestInfo);

        log.info("[UPYOG-FAMILY-SERVICE] Successfully queued family creation to Kafka persister: {}", family.getFamilyId());
        return family;
    }

    /**
     * Searches Family Units in UPYOG based on query parameters such as family ID, member UUID, mobile, or tenant.
     * Enriches matched families with demographic profiles from the citizen identity registry.
     *
     * @param criteria search criteria containing query filter attributes
     * @param requestInfo UPYOG request context envelope
     * @return list of matching Family aggregates
     */
    @Override
    public List<Family> searchFamilies(FamilySearchCriteria criteria, RequestInfo requestInfo) {
        log.info("[UPYOG-FAMILY-SERVICE] Searching families for tenant: {}, familyId: {}, member: {}",
                criteria.getTenantId(), criteria.getFamilyId(), criteria.getMemberUserUuid());

        // Resolve member UUID from mobile number if mobile is passed
        if (StringUtils.isNotBlank(criteria.getMobileNumber()) && StringUtils.isBlank(criteria.getMemberUserUuid())) {
            List<CitizenProfile> matchedProfiles = userClient.searchUsersByMobile(criteria.getMobileNumber(), criteria.getTenantId(), requestInfo);
            if (matchedProfiles != null && !matchedProfiles.isEmpty()) {
                criteria.setMemberUserUuid(matchedProfiles.get(0).getUuid());
            } else {
                log.info("[UPYOG-FAMILY-SERVICE] No citizen found for mobile: {}. Returning empty.", criteria.getMobileNumber());
                return Collections.emptyList();
            }
        }

        List<Family> families = familyRepository.search(criteria);
        if (families.isEmpty()) {
            return Collections.emptyList();
        }

        enrichCitizenDemographics(families, requestInfo);
        return families;
    }

    /**
     * Updates an existing Family Unit's metadata and active member roster in UPYOG.
     * Validates relationships, manages soft deletes, and verifies that exactly one active Head of Family remains.
     *
     * @param request the family update request containing modified family attributes
     * @return the updated Family aggregate
     */
    @Override
    @Transactional
    public Family updateFamily(FamilyRequest request) {
        Family updatePayload = request.getFamily();
        log.info("[UPYOG-FAMILY-SERVICE] Updating family: {}", updatePayload.getFamilyId());

        FamilySearchCriteria criteria = FamilySearchCriteria.builder()
                .familyId(updatePayload.getFamilyId())
                .tenantId(updatePayload.getTenantId())
                .build();

        List<Family> existingList = familyRepository.search(criteria);
        if (existingList.isEmpty()) {
            throw new CustomException(FamilyConstants.ERROR_FAMILY_NOT_FOUND, "Family not found: " + updatePayload.getFamilyId());
        }

        Family existingFamily = existingList.get(0);
        familyValidator.validateUpdate(request, existingFamily);

        RequestInfo requestInfo = request.getRequestInfo();
        String userUuid = requestInfo != null && requestInfo.getUserInfo() != null
                ? requestInfo.getUserInfo().getUuid()
                : FamilyConstants.SYSTEM_USER;
        Long now = System.currentTimeMillis();

        existingFamily.getAuditDetails().setLastModifiedBy(userUuid);
        existingFamily.getAuditDetails().setLastModifiedTime(now);

        if (StringUtils.isNotBlank(updatePayload.getCategory())) {
            existingFamily.setCategory(updatePayload.getCategory());
        }
        if (StringUtils.isNotBlank(updatePayload.getHeadUserUuid())) {
            existingFamily.setHeadUserUuid(updatePayload.getHeadUserUuid());
        }
        if (updatePayload.getAdditionalDetails() != null) {
            existingFamily.setAdditionalDetails(updatePayload.getAdditionalDetails());
        }

        if (updatePayload.getMembers() != null) {
            existingFamily.setMembers(updatePayload.getMembers());
            for (FamilyMember m : existingFamily.getMembers()) {
                m.setFamilyId(existingFamily.getFamilyId());
                m.setTenantId(existingFamily.getTenantId());
                if (m.getAuditDetails() == null) {
                    m.setAuditDetails(existingFamily.getAuditDetails());
                } else {
                    m.getAuditDetails().setLastModifiedBy(userUuid);
                    m.getAuditDetails().setLastModifiedTime(now);
                }
            }
        }

        if (updatePayload.getDocuments() != null) {
            existingFamily.setDocuments(updatePayload.getDocuments());
            for (FamilyDocument d : existingFamily.getDocuments()) {
                d.setFamilyId(existingFamily.getFamilyId());
                d.setTenantId(existingFamily.getTenantId());
                if (d.getAuditDetails() == null) {
                    d.setAuditDetails(existingFamily.getAuditDetails());
                } else {
                    d.getAuditDetails().setLastModifiedBy(userUuid);
                    d.getAuditDetails().setLastModifiedTime(now);
                }
            }
        }

        // Asynchronously persist via Kafka Persister (standard UPYOG pattern)
        kafkaProducer.push(configuration.getUpdateFamilyTopic(), new FamilyRequest(requestInfo, existingFamily));

        enrichCitizenDemographics(Collections.singletonList(existingFamily), requestInfo);
        return existingFamily;
    }

    /**
     * Splits an existing Family Unit into a child family in UPYOG, recording full lineage genealogy.
     * Deactivates splitting members from the parent family and provisions a new family unit with the specified head.
     *
     * @param request the family split request containing parent family ID and splitting member UUIDs
     * @return the newly formed child Family aggregate
     */
    @Override
    @Transactional
    public Family splitFamily(FamilySplitRequest request) {
        log.info("[UPYOG-FAMILY-SERVICE] Splitting family: parent={}", request.getParentFamilyId());

        FamilySearchCriteria criteria = FamilySearchCriteria.builder()
                .familyId(request.getParentFamilyId())
                .tenantId(request.getTenantId())
                .build();

        List<Family> families = familyRepository.search(criteria);
        if (families.isEmpty()) {
            throw new CustomException(FamilyConstants.ERROR_FAMILY_NOT_FOUND, "Parent family not found: " + request.getParentFamilyId());
        }

        Family parentFamily = families.get(0);
        familyValidator.validateSplit(request, parentFamily);

        RequestInfo requestInfo = request.getRequestInfo();
        String userUuid = requestInfo != null && requestInfo.getUserInfo() != null
                ? requestInfo.getUserInfo().getUuid()
                : FamilyConstants.SYSTEM_USER;
        Long now = System.currentTimeMillis();

        // 1. Separate splitting members out of parent family
        List<FamilyMember> splittingMembers = new ArrayList<>();
        for (FamilyMember member : parentFamily.getMembers()) {
            if (request.getSplittingMemberUserUuids().contains(member.getUserUuid())) {
                member.setActive(false);
                member.setEffectiveTo(now);
                if (member.getAuditDetails() == null) {
                    member.setAuditDetails(AuditDetails.builder().createdBy(userUuid).createdTime(now).build());
                }
                member.getAuditDetails().setLastModifiedBy(userUuid);
                member.getAuditDetails().setLastModifiedTime(now);

                FamilyMember newUnitMember = FamilyMember.builder()
                        .userUuid(member.getUserUuid())
                        .tenantId(request.getTenantId())
                        .relationshipWithHead(member.getUserUuid().equals(request.getNewFamilyHeadUserUuid())
                                ? FamilyConstants.RELATIONSHIP_HEAD
                                : member.getRelationshipWithHead())
                        .isHead(member.getUserUuid().equals(request.getNewFamilyHeadUserUuid()))
                        .isVerified(member.getIsVerified())
                        .active(true)
                        .effectiveFrom(now)
                        .additionalDetails(member.getAdditionalDetails())
                        .build();
                splittingMembers.add(newUnitMember);
            }
        }

        // Update parent family via Kafka persister
        parentFamily.getAuditDetails().setLastModifiedBy(userUuid);
        parentFamily.getAuditDetails().setLastModifiedTime(now);
        kafkaProducer.push(configuration.getUpdateFamilyTopic(), new FamilyRequest(requestInfo, parentFamily));

        // 2. Create new child family unit via Kafka persister
        String newFamilyId = idGenRepository.generateFamilyId(requestInfo, request.getTenantId());
        for (FamilyMember m : splittingMembers) {
            if (StringUtils.isBlank(m.getId())) {
                m.setId(UUID.randomUUID().toString());
            }
        }
        Family childFamily = Family.builder()
                .id(UUID.randomUUID().toString())
                .familyId(newFamilyId)
                .tenantId(request.getTenantId())
                .headUserUuid(request.getNewFamilyHeadUserUuid())
                .status(FamilyStatus.ACTIVE)
                .category(parentFamily.getCategory())
                .members(splittingMembers)
                .documents(request.getDocuments() != null ? request.getDocuments() : new ArrayList<>())
                .auditDetails(AuditDetails.builder()
                        .createdBy(userUuid)
                        .createdTime(now)
                        .lastModifiedBy(userUuid)
                        .lastModifiedTime(now)
                        .build())
                .build();

        kafkaProducer.push(configuration.getCreateFamilyTopic(), new FamilyRequest(requestInfo, childFamily));

        // 3. Record lineage history
        FamilyLineage lineage = FamilyLineage.builder()
                .parentFamilyId(parentFamily.getFamilyId())
                .childFamilyId(newFamilyId)
                .actionType(LineageActionType.SPLIT)
                .reason(request.getReason())
                .createdTime(now)
                .build();
        familyRepository.saveLineage(lineage);

        kafkaProducer.push(configuration.getSplitFamilyTopic(), request);

        enrichCitizenDemographics(Collections.singletonList(childFamily), requestInfo);
        return childFamily;
    }

    /**
     * Merges two existing Family Units into a consolidated household unit in UPYOG.
     * Migrates members from the merging family into the surviving family, marks the merging family as MERGED, and records lineage.
     *
     * @param request the family merge request containing surviving family ID and merging family ID
     * @return the consolidated surviving Family aggregate
     */
    @Override
    @Transactional
    public Family mergeFamily(FamilyMergeRequest request) {
        log.info("[UPYOG-FAMILY-SERVICE] Merging family: merging={} into surviving={}",
                request.getMergingFamilyId(), request.getSurvivingFamilyId());

        FamilySearchCriteria survivingCriteria = FamilySearchCriteria.builder()
                .familyId(request.getSurvivingFamilyId())
                .tenantId(request.getTenantId())
                .build();
        FamilySearchCriteria mergingCriteria = FamilySearchCriteria.builder()
                .familyId(request.getMergingFamilyId())
                .tenantId(request.getTenantId())
                .build();

        List<Family> survivingList = familyRepository.search(survivingCriteria);
        List<Family> mergingList = familyRepository.search(mergingCriteria);

        Family survivingFamily = survivingList.isEmpty() ? null : survivingList.get(0);
        Family mergingFamily = mergingList.isEmpty() ? null : mergingList.get(0);

        familyValidator.validateMerge(request, survivingFamily, mergingFamily);

        RequestInfo requestInfo = request.getRequestInfo();
        String userUuid = requestInfo != null && requestInfo.getUserInfo() != null
                ? requestInfo.getUserInfo().getUuid()
                : FamilyConstants.SYSTEM_USER;
        Long now = System.currentTimeMillis();

        // 1. Move members of merging family into surviving family
        for (FamilyMember member : mergingFamily.getMembers()) {
            if (Boolean.TRUE.equals(member.getActive())) {
                member.setActive(false);
                member.setEffectiveTo(now);
                if (member.getAuditDetails() == null) {
                    member.setAuditDetails(AuditDetails.builder().createdBy(userUuid).createdTime(now).build());
                }
                member.getAuditDetails().setLastModifiedBy(userUuid);
                member.getAuditDetails().setLastModifiedTime(now);

                FamilyMember transferredMember = FamilyMember.builder()
                        .userUuid(member.getUserUuid())
                        .tenantId(request.getTenantId())
                        .relationshipWithHead(member.getUserUuid().equals(request.getCommonHeadUserUuid())
                                ? FamilyConstants.RELATIONSHIP_HEAD
                                : member.getRelationshipWithHead())
                        .isHead(member.getUserUuid().equals(request.getCommonHeadUserUuid()))
                        .isVerified(member.getIsVerified())
                        .active(true)
                        .effectiveFrom(now)
                        .additionalDetails(member.getAdditionalDetails())
                        .build();
                survivingFamily.getMembers().add(transferredMember);
            }
        }

        // Deactivate merging family via Kafka persister
        mergingFamily.setStatus(FamilyStatus.MERGED);
        mergingFamily.getAuditDetails().setLastModifiedBy(userUuid);
        mergingFamily.getAuditDetails().setLastModifiedTime(now);
        kafkaProducer.push(configuration.getUpdateFamilyTopic(), new FamilyRequest(requestInfo, mergingFamily));

        // Update surviving family with designated common HoF via Kafka persister
        survivingFamily.setHeadUserUuid(request.getCommonHeadUserUuid());
        for (FamilyMember m : survivingFamily.getMembers()) {
            if (StringUtils.isBlank(m.getId())) {
                m.setId(UUID.randomUUID().toString());
            }
            m.setIsHead(m.getUserUuid().equals(request.getCommonHeadUserUuid()));
        }
        survivingFamily.getAuditDetails().setLastModifiedBy(userUuid);
        survivingFamily.getAuditDetails().setLastModifiedTime(now);

        kafkaProducer.push(configuration.getUpdateFamilyTopic(), new FamilyRequest(requestInfo, survivingFamily));

        // 2. Record lineage
        FamilyLineage lineage = FamilyLineage.builder()
                .parentFamilyId(mergingFamily.getFamilyId())
                .childFamilyId(survivingFamily.getFamilyId())
                .actionType(LineageActionType.MERGE)
                .reason(request.getReason())
                .createdTime(now)
                .build();
        familyRepository.saveLineage(lineage);

        kafkaProducer.push(configuration.getMergeFamilyTopic(), request);

        enrichCitizenDemographics(Collections.singletonList(survivingFamily), requestInfo);
        return survivingFamily;
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
}
