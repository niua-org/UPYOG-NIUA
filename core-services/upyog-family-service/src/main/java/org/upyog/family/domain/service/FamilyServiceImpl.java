package org.upyog.family.domain.service;

import lombok.RequiredArgsConstructor;
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
import org.upyog.family.util.MaskUtils;
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
 * Core business orchestrator implementation for the UPYOG Family Registry.
 *
 * <p>Implements {@link FamilyService} managing all household lifecycle events including creation,
 * query resolution with citizen demographic enrichment, metadata and member updates, household split,
 * and household consolidation (merge). Adheres to transactional boundaries and coordinates asynchronous
 * database indexing via Kafka persister topics.</p>
 *
 * <p><b>Thread Safety & Architecture:</b></p>
 * <ul>
 *   <li>Stateless Spring {@link Service} singleton with constructor-injected thread-safe components.</li>
 *   <li>Database write operations are demarcated with {@link Transactional} semantics to maintain local consistency
 *       prior to dispatching events to distributed Kafka topics.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FamilyServiceImpl implements FamilyService {

    private final FamilyRepository familyRepository;
    private final FamilyValidator familyValidator;
    private final IdGenRepository idGenRepository;
    private final UserClient userClient;
    private final KafkaProducer kafkaProducer;
    private final FamilyConfiguration configuration;

    /**
     * Creates a new household Family Unit in UPYOG with sequential ID generation, audit logging, and persistence.
     *
     * <p>Enforces domain validations via {@link FamilyValidator#validateCreate}, generates a unique
     * family identifier using {@link IdGenRepository#generateFamilyId}, decorates all sub-entities
     * with system audit metadata, enqueues the creation command to Kafka topic {@code configuration.getCreateFamilyTopic()},
     * and synchronously enriches the returned aggregate with demographics from {@code egov-user}.</p>
     *
     * @param request the family creation request containing metadata, head of family, and members; must not be {@code null}.
     * @return the created {@link Family} aggregate populated with the generated {@code familyId} and enriched member profiles.
     * @throws CustomException if validation fails (e.g. invalid tenant, missing or multiple heads, duplicate member UUIDs,
     *                         or multi-family exclusivity violation).
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
        if (StringUtils.isBlank(family.getFamilyType())) {
            family.setFamilyType(FamilyConstants.TYPE_UPYOG_INTERNAL);
        }

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

        List<FamilyMember> members = family.getMembers();
        if (members != null && !members.isEmpty()) {
            int unassignedCount = (int) members.stream()
                    .filter(m -> StringUtils.isBlank(m.getId()))
                    .count();
            List<String> generatedMemberIds = Collections.emptyList();
            if (unassignedCount > 0 && idGenRepository != null) {
                try {
                    generatedMemberIds = idGenRepository.generateMemberIds(requestInfo, family.getTenantId(), unassignedCount);
                } catch (Exception e) {
                    log.warn("[UPYOG-FAMILY-SERVICE] IdGen failed to generate member IDs, falling back to UUID: {}", e.getMessage());
                }
            }
            int memberIdIdx = 0;
            for (FamilyMember member : members) {
                if (StringUtils.isBlank(member.getId())) {
                    if (generatedMemberIds != null && memberIdIdx < generatedMemberIds.size()
                            && StringUtils.isNotBlank(generatedMemberIds.get(memberIdIdx))) {
                        member.setId(generatedMemberIds.get(memberIdIdx++));
                    } else {
                        member.setId(UUID.randomUUID().toString());
                    }
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
     * Searches Family Units based on query filters (family ID, member UUID, mobile, or tenant) and enriches results.
     *
     * <p>If {@code mobileNumber} is provided in the search criteria without a {@code memberUserUuid}, the service
     * first contacts {@code egov-user} via {@link UserClient#searchUsersByMobile} to resolve the citizen UUID.
     * Returns an empty list if no user matches the supplied mobile. Matched households from {@link FamilyRepository}
     * are subsequently decorated with citizen profiles via {@link #enrichCitizenDemographics}.</p>
     *
     * @param criteria query criteria containing filter attributes; must include valid {@code tenantId}.
     * @param requestInfo UPYOG request context envelope containing the authorization token for inter-service communication.
     * @return a {@link List} of matching {@link Family} aggregates with fully populated member demographics; never {@code null}.
     * @throws CustomException if {@code criteria} is {@code null} or {@code tenantId} is missing.
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
                log.info("[UPYOG-FAMILY-SERVICE] No citizen found for mobile: {}. Returning empty.", MaskUtils.maskMobile(criteria.getMobileNumber()));
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
     *
     * <p>Retrieves the existing family record, runs domain validations via {@link FamilyValidator#validateUpdate},
     * stamps audit attributes, merges modified fields (category, headUserUuid, additionalDetails, members, documents),
     * publishes the update command to the Kafka topic {@code configuration.getUpdateFamilyTopic()},
     * and refreshes demographic profile caches for all active members.</p>
     *
     * @param request the family update request containing modified attributes and the active {@link RequestInfo}; must not be {@code null}.
     * @return the updated and enriched {@link Family} aggregate.
     * @throws CustomException if:
     *         <ul>
     *           <li>Target family is not found in database (Code: {@code FAMILY_NOT_FOUND})</li>
     *           <li>Caller does not have permission to mutate the family (Code: {@code UNAUTHORIZED_ACCESS})</li>
     *           <li>The updated member list lacks an active Head of Family (Code: {@code HEAD_OF_FAMILY_REQUIRED})</li>
     *         </ul>
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
     * Splits an existing Family Unit into a child family in UPYOG, recording full genealogical lineage.
     *
     * <p>Executes the following operations atomically within a database transaction:</p>
     * <ol>
     *   <li>Retrieves and validates the parent family using {@link FamilyValidator#validateSplit}.</li>
     *   <li>Soft-deactivates splitting members from the parent family, setting {@code active = false} and {@code effectiveTo = now}.</li>
     *   <li>Generates a new sequential {@code familyId} for the child household unit via {@link IdGenRepository}.</li>
     *   <li>Assigns the designated {@code newFamilyHeadUserUuid} as the child unit's HoF.</li>
     *   <li>Appends an immutable genealogical audit record to the {@code ug_family_lineage} table.</li>
     *   <li>Pushes update events to Kafka topics for both parent and child units.</li>
     * </ol>
     *
     * @param request the family split request containing parent family ID, splitting member UUIDs, and new HoF UUID.
     * @return the newly formed child {@link Family} aggregate populated with migrated active members.
     * @throws CustomException if parent family is missing/inactive, splitting members are invalid, or new head is not part of the split.
     */
    @Override
    @Transactional
    public Family splitFamily(FamilySplitRequest request) {
        log.info("[UPYOG-FAMILY-SERVICE] Splitting family: parent={}", request.getParentFamilyId());

        Family parentFamily = familyRepository.findByFamilyIdAndTenantId(request.getParentFamilyId(), request.getTenantId())
                .orElseThrow(() -> new CustomException(FamilyConstants.ERROR_FAMILY_NOT_FOUND, "Parent family not found: " + request.getParentFamilyId()));
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
        int unassignedSplitCount = (int) splittingMembers.stream()
                .filter(m -> StringUtils.isBlank(m.getId()))
                .count();
        List<String> generatedSplitMemberIds = Collections.emptyList();
        if (unassignedSplitCount > 0 && idGenRepository != null) {
            try {
                generatedSplitMemberIds = idGenRepository.generateMemberIds(requestInfo, request.getTenantId(), unassignedSplitCount);
            } catch (Exception e) {
                log.warn("[UPYOG-FAMILY-SERVICE] IdGen failed to generate split member IDs, falling back to UUID: {}", e.getMessage());
            }
        }
        int splitMemberIdIdx = 0;
        for (FamilyMember m : splittingMembers) {
            if (StringUtils.isBlank(m.getId())) {
                if (generatedSplitMemberIds != null && splitMemberIdIdx < generatedSplitMemberIds.size()
                        && StringUtils.isNotBlank(generatedSplitMemberIds.get(splitMemberIdIdx))) {
                    m.setId(generatedSplitMemberIds.get(splitMemberIdIdx++));
                } else {
                    m.setId(UUID.randomUUID().toString());
                }
            }
        }
        Family childFamily = Family.builder()
                .id(UUID.randomUUID().toString())
                .familyId(newFamilyId)
                .tenantId(request.getTenantId())
                .headUserUuid(request.getNewFamilyHeadUserUuid())
                .status(FamilyStatus.ACTIVE)
                .familyType(parentFamily.getFamilyType() != null ? parentFamily.getFamilyType() : FamilyConstants.TYPE_UPYOG_INTERNAL)
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
     *
     * <p>Executes the following operations atomically within a database transaction:</p>
     * <ol>
     *   <li>Validates participating families via {@link FamilyValidator#validateMerge}.</li>
     *   <li>Deactivates members in the merging family and appends them as active members in the surviving family.</li>
     *   <li>Marks the merging family status as {@link FamilyStatus#MERGED}.</li>
     *   <li>Applies the {@code commonHeadUserUuid} across the consolidated surviving unit.</li>
     *   <li>Persists an immutable {@link FamilyLineage} record linking the two units with action type {@link LineageActionType#MERGE}.</li>
     *   <li>Dispatches Kafka events to persister topics for database synchronization.</li>
     *   <li>Enriches the surviving family's members with citizen identity details from {@code egov-user}.</li>
     * </ol>
     *
     * @param request the family merge request containing surviving and merging family IDs, common HoF UUID, and reason.
     * @return the consolidated surviving {@link Family} aggregate.
     * @throws CustomException if either family is missing or not active, tenants differ, or the common head is invalid.
     */
    @Override
    @Transactional
    public Family mergeFamily(FamilyMergeRequest request) {
        log.info("[UPYOG-FAMILY-SERVICE] Merging family: merging={} into surviving={}",
                request.getMergingFamilyId(), request.getSurvivingFamilyId());

        Family survivingFamily = familyRepository.findByFamilyIdAndTenantId(request.getSurvivingFamilyId(), request.getTenantId()).orElse(null);
        Family mergingFamily = familyRepository.findByFamilyIdAndTenantId(request.getMergingFamilyId(), request.getTenantId()).orElse(null);

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
        if (StringUtils.isBlank(survivingFamily.getFamilyType())) {
            survivingFamily.setFamilyType(FamilyConstants.TYPE_UPYOG_INTERNAL);
        }
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

    /**
     * Enriches a collection of {@link Family} aggregates with live citizen demographic details from {@code egov-user}.
     *
     * <p>Collects distinct member UUIDs across all supplied families, dispatches a single bulk lookup
     * to {@link UserClient#searchUsersByUuids}, and maps the resolved {@link CitizenProfile} objects back
     * into their corresponding {@link FamilyMember#setCitizenDetails(CitizenProfile)} slots.
     * Operates safely when inputs are null, empty, or when inter-service lookups return partial results.</p>
     *
     * @param families the list of family aggregates whose members require demographic enrichment; may be {@code null}.
     * @param requestInfo the active request context containing auth tokens for inter-service communication.
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

        for (Family f : families) {
            if (StringUtils.isNotBlank(f.getHeadUserUuid()) && !allMemberUuids.contains(f.getHeadUserUuid())) {
                allMemberUuids.add(f.getHeadUserUuid());
            }
        }

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
            if (StringUtils.isBlank(family.getHeadName()) && StringUtils.isNotBlank(family.getHeadUserUuid())
                    && profileMap.containsKey(family.getHeadUserUuid()) && profileMap.get(family.getHeadUserUuid()) != null) {
                family.setHeadName(profileMap.get(family.getHeadUserUuid()).getName());
            }
        }
    }
}
