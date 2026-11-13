package org.upyog.family.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.upyog.family.web.contract.UserProperty;
import org.upyog.family.web.contract.UserVehicleDetail;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
public class FamilyServiceImpl implements FamilyService {

    private final FamilyRepository familyRepository;
    private final FamilyValidator familyValidator;
    private final IdGenRepository idGenRepository;
    private final UserClient userClient;
    private final KafkaProducer kafkaProducer;
    private final FamilyConfiguration configuration;
    private final ObjectMapper objectMapper;

    @Autowired
    public FamilyServiceImpl(FamilyRepository familyRepository,
                             FamilyValidator familyValidator,
                             IdGenRepository idGenRepository,
                             UserClient userClient,
                             KafkaProducer kafkaProducer,
                             FamilyConfiguration configuration,
                             @Autowired(required = false) ObjectMapper objectMapper) {
        this.familyRepository = familyRepository;
        this.familyValidator = familyValidator;
        this.idGenRepository = idGenRepository;
        this.userClient = userClient;
        this.kafkaProducer = kafkaProducer;
        this.configuration = configuration;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public FamilyServiceImpl(FamilyRepository familyRepository,
                             FamilyValidator familyValidator,
                             IdGenRepository idGenRepository,
                             UserClient userClient,
                             KafkaProducer kafkaProducer,
                             FamilyConfiguration configuration) {
        this(familyRepository, familyValidator, idGenRepository, userClient, kafkaProducer, configuration, null);
    }

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
        if (StringUtils.isBlank(family.getFamilySource())) {
            family.setFamilySource(FamilyConstants.SOURCE_CITIZEN_PORTAL);
        }
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

                // Inherit common tenantId and userUuid across declared assets
                if (member.getProperties() != null) {
                    for (UserProperty prop : member.getProperties()) {
                        prop.setTenantId(family.getTenantId());
                        if (StringUtils.isBlank(prop.getUserUuid())) {
                            prop.setUserUuid(member.getUserUuid());
                        }
                        prop.setAuditDetails(auditDetails);
                    }
                }
                if (member.getIncome() != null) {
                    member.getIncome().setTenantId(family.getTenantId());
                    if (StringUtils.isBlank(member.getIncome().getUserUuid())) {
                        member.getIncome().setUserUuid(member.getUserUuid());
                    }
                    member.getIncome().setAuditDetails(auditDetails);
                }
                if (member.getVehicles() != null) {
                    for (UserVehicleDetail vehicle : member.getVehicles()) {
                        vehicle.setTenantId(family.getTenantId());
                        if (StringUtils.isBlank(vehicle.getUserUuid())) {
                            vehicle.setUserUuid(member.getUserUuid());
                        }
                        vehicle.setAuditDetails(auditDetails);
                    }
                }
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

        // 3. Fix for PR Comment #10: Synchronize member demographics and declared assets to egov-user BEFORE Kafka push.
        // If declared asset synchronization fails:
        // - Records assetSyncStatus = "FAILED" on the family additionalDetails.
        // - If failOnAssetSyncError is enabled (default true), throws CustomException to abort family creation so the family
        //   is not created with missing/unattached assets.
        if (members != null && !members.isEmpty()) {
            boolean hasAssets = hasDeclaredAssets(members);
            log.info("[UPYOG-FAMILY-SERVICE] Synchronizing member assets to egov-user for family: {}, hasDeclaredAssets: {}", familyId, hasAssets);
            boolean syncSuccess = userClient.syncMemberAssets(members, family.getTenantId(), requestInfo);
            if (!syncSuccess) {
                recordAssetSyncStatus(family, FamilyConstants.STATUS_FAILED);
                log.error("[UPYOG-FAMILY-SERVICE] Member asset synchronization failed for family: {}. Members: {}, hasAssets: {}",
                        familyId, members.size(), hasAssets);
                if (hasAssets && configuration != null && Boolean.TRUE.equals(configuration.getFailOnAssetSyncError())) {
                    throw new CustomException(FamilyConstants.ERROR_MEMBER_ASSET_SYNC_FAILED, FamilyConstants.MSG_MEMBER_ASSET_SYNC_FAILED);
                }
            } else {
                recordAssetSyncStatus(family, FamilyConstants.STATUS_SUCCESS);
                log.info("[UPYOG-FAMILY-SERVICE] Successfully synchronized member assets for family: {}", familyId);
            }
        } else {
            recordAssetSyncStatus(family, FamilyConstants.ASSET_SYNC_STATUS_NOT_APPLICABLE);
        }

        // 4. Persist to Database via Kafka Persister (standard UPYOG pattern)
        kafkaProducer.push(configuration.getCreateFamilyTopic(), request);

        // 5. Enrich citizen demographics
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
            throw new CustomException(FamilyConstants.ERROR_FAMILY_NOT_FOUND, FamilyConstants.MSG_FAMILY_NOT_FOUND_PREFIX + updatePayload.getFamilyId());
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

        // Fix for PR Comment #11 (FamilyServiceImpl.java:338):
        // Capture existing members before updatePayload overwrites existingFamily.members.
        // Used to detect which members were actually modified and avoid full re-synchronization of unchanged members.
        List<FamilyMember> previousMembers = existingFamily.getMembers() != null
                ? new ArrayList<>(existingFamily.getMembers())
                : Collections.emptyList();

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

        // Fix for PR Comment #10 & #11:
        // - Comment #10: Synchronize member demographics and assets BEFORE Kafka persister push.
        // - Comment #11: Optimize updateFamily to ONLY synchronize newly added members or existing members whose
        //   asset declarations or demographics were actually modified, rather than triggering a full re-sync
        //   of every household member on every family update.
        List<FamilyMember> membersToSync = getMembersRequiringAssetSync(updatePayload.getMembers(), previousMembers);
        if (!membersToSync.isEmpty()) {
            log.info("[UPYOG-FAMILY-SERVICE] Synchronizing {} modified/new member(s) with declared assets to egov-user for family: {}",
                    membersToSync.size(), existingFamily.getFamilyId());
            boolean syncSuccess = userClient.syncMemberAssets(membersToSync, existingFamily.getTenantId(), requestInfo);
            if (!syncSuccess) {
                recordAssetSyncStatus(existingFamily, FamilyConstants.STATUS_FAILED);
                log.error("[UPYOG-FAMILY-SERVICE] Member asset update synchronization failed for family: {}. Modified members: {}",
                        existingFamily.getFamilyId(), membersToSync.size());
                if (configuration != null && Boolean.TRUE.equals(configuration.getFailOnAssetSyncError())) {
                    throw new CustomException(FamilyConstants.ERROR_MEMBER_ASSET_SYNC_FAILED, FamilyConstants.MSG_MEMBER_ASSET_SYNC_FAILED);
                }
            } else {
                recordAssetSyncStatus(existingFamily, FamilyConstants.STATUS_SUCCESS);
                log.info("[UPYOG-FAMILY-SERVICE] Successfully synchronized {} modified member(s) declared assets for family: {}",
                        membersToSync.size(), existingFamily.getFamilyId());
            }
        } else {
            recordAssetSyncStatus(existingFamily, FamilyConstants.ASSET_SYNC_STATUS_NOT_APPLICABLE);
            log.debug("[UPYOG-FAMILY-SERVICE] No modified members with declared assets to synchronize for family update: {}",
                    existingFamily.getFamilyId());
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
                .orElseThrow(() -> new CustomException(FamilyConstants.ERROR_FAMILY_NOT_FOUND, FamilyConstants.MSG_PARENT_FAMILY_NOT_FOUND_PREFIX + request.getParentFamilyId()));
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
                                ? FamilyConstants.RELATIONSHIP_SELF
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
                                ? FamilyConstants.RELATIONSHIP_SELF
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

    /**
     * Inspects family member roster to check if any member has declared assets or demographic documents.
     * Fix for PR Comment #10: Distinguishes between basic membership and declarations requiring egov-user sync.
     *
     * @param members list of family members
     * @return true if at least one member declares properties, vehicles, income, or demographic proof documents
     */
    private boolean hasDeclaredAssets(List<FamilyMember> members) {
        if (members == null || members.isEmpty()) {
            return false;
        }
        for (FamilyMember m : members) {
            if (hasDeclaredAssets(m)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if an individual family member declares properties, vehicles, income, or demographic proof documents.
     * Fix for PR Comment #10 & #11: Used for targeted synchronization filtering.
     *
     * @param m family member contract
     * @return true if member contains declared assets or demographic documents
     */
    private boolean hasDeclaredAssets(FamilyMember m) {
        if (m == null) {
            return false;
        }
        if (m.getCitizenDetails() != null) {
            if (m.getCitizenDetails().getDocuments() != null && !m.getCitizenDetails().getDocuments().isEmpty()) return true;
            if (StringUtils.isNotBlank(m.getCitizenDetails().getCaste())) return true;
            if (m.getCitizenDetails().getIsSpeciallyAbled() != null) return true;
        }
        if (m.getProperties() != null && !m.getProperties().isEmpty()) return true;
        if (m.getIncome() != null) return true;
        if (m.getVehicles() != null && !m.getVehicles().isEmpty()) return true;
        return false;
    }

    /**
     * Identifies members from an update request that require asset/demographic synchronization to egov-user.
     * Fix for PR Comment #11 (FamilyServiceImpl.java:338):
     * Instead of re-syncing every member in the household on every family update (which causes duplicate
     * processing and excessive inter-service calls), this filters only for:
     * 1) Newly added members with declared assets or demographic documents, or
     * 2) Existing members whose asset declarations or demographic documents are explicitly supplied/modified in the update.
     *
     * @param incomingMembers list of members in the update payload
     * @param previousMembers existing members from the database prior to update
     * @return list of modified/new members requiring asset synchronization
     */
    private List<FamilyMember> getMembersRequiringAssetSync(List<FamilyMember> incomingMembers, List<FamilyMember> previousMembers) {
        if (incomingMembers == null || incomingMembers.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, FamilyMember> existingMemberMap = new HashMap<>();
        if (previousMembers != null) {
            for (FamilyMember em : previousMembers) {
                if (em != null && StringUtils.isNotBlank(em.getUserUuid())) {
                    existingMemberMap.put(em.getUserUuid(), em);
                }
            }
        }

        List<FamilyMember> membersToSync = new ArrayList<>();
        for (FamilyMember incoming : incomingMembers) {
            if (incoming == null || StringUtils.isBlank(incoming.getUserUuid())) {
                continue;
            }

            // Only members with declared assets or demographic documents need egov-user sync
            if (!hasDeclaredAssets(incoming)) {
                continue;
            }

            FamilyMember existing = existingMemberMap.get(incoming.getUserUuid());
            if (existing == null) {
                // New member added during update with declared assets -> must sync
                membersToSync.add(incoming);
            } else if (isMemberAssetModified(incoming, existing)) {
                // Existing member with modified or newly supplied asset declarations
                membersToSync.add(incoming);
            }
        }
        return membersToSync;
    }

    /**
     * Checks if an incoming member's declared assets or demographic declarations differ from the existing member.
     * Fix for PR Comment #11: Avoids re-syncing unmodified existing members during family update.
     *
     * @param incoming incoming member contract from the update request
     * @param existing existing member contract from the database
     * @return true if asset declarations or demographics are modified or newly provided
     */
    private boolean isMemberAssetModified(FamilyMember incoming, FamilyMember existing) {
        if (incoming == null) {
            return false;
        }
        if (existing == null) {
            return true;
        }

        try {
            // Check properties
            boolean incomingHasProps = incoming.getProperties() != null && !incoming.getProperties().isEmpty();
            boolean existingHasProps = existing.getProperties() != null && !existing.getProperties().isEmpty();
            if (incomingHasProps != existingHasProps) {
                return true;
            }
            if (incomingHasProps && !objectMapper.valueToTree(incoming.getProperties()).equals(objectMapper.valueToTree(existing.getProperties()))) {
                return true;
            }

            // Check vehicles
            boolean incomingHasVehicles = incoming.getVehicles() != null && !incoming.getVehicles().isEmpty();
            boolean existingHasVehicles = existing.getVehicles() != null && !existing.getVehicles().isEmpty();
            if (incomingHasVehicles != existingHasVehicles) {
                return true;
            }
            if (incomingHasVehicles && !objectMapper.valueToTree(incoming.getVehicles()).equals(objectMapper.valueToTree(existing.getVehicles()))) {
                return true;
            }

            // Check income
            boolean incomingHasIncome = incoming.getIncome() != null;
            boolean existingHasIncome = existing.getIncome() != null;
            if (incomingHasIncome != existingHasIncome) {
                return true;
            }
            if (incomingHasIncome && !objectMapper.valueToTree(incoming.getIncome()).equals(objectMapper.valueToTree(existing.getIncome()))) {
                return true;
            }

            // Check citizen details / documents
            if (incoming.getCitizenDetails() != null) {
                if (existing.getCitizenDetails() == null) {
                    return true;
                }
                if (!objectMapper.valueToTree(incoming.getCitizenDetails()).equals(objectMapper.valueToTree(existing.getCitizenDetails()))) {
                    return true;
                }
            }

            // Check if member audit timestamp indicates client-side modification
            if (incoming.getAuditDetails() != null && existing.getAuditDetails() != null
                    && incoming.getAuditDetails().getLastModifiedTime() != null
                    && existing.getAuditDetails().getLastModifiedTime() != null) {
                if (incoming.getAuditDetails().getLastModifiedTime() > existing.getAuditDetails().getLastModifiedTime()) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("[UPYOG-FAMILY-SERVICE] Error comparing member asset declarations for userUuid: {}. Defaulting to modified=true",
                    incoming.getUserUuid(), e);
            return true;
        }

        return false;
    }

    /**
     * Records member asset synchronization status and timestamp into family additionalDetails.
     * Fix for PR Comment #10: Ensures sync status is visible in audit trails and query responses.
     *
     * @param family family aggregate entity
     * @param status synchronization status code (SUCCESS, FAILED, or NOT_APPLICABLE)
     */
    private void recordAssetSyncStatus(Family family, String status) {
        if (family == null) return;
        Map<String, Object> details;
        if (family.getAdditionalDetails() instanceof Map<?, ?> map) {
            details = new HashMap<>((Map<String, Object>) map);
        } else if (family.getAdditionalDetails() != null) {
            try {
                details = objectMapper.convertValue(family.getAdditionalDetails(), Map.class);
                if (details == null) {
                    details = new HashMap<>();
                }
            } catch (Exception e) {
                details = new HashMap<>();
            }
        } else {
            details = new HashMap<>();
        }
        if (details == null) {
            details = new HashMap<>();
        }
        details.put(FamilyConstants.KEY_MEMBER_ASSET_SYNC_STATUS, status);
        details.put(FamilyConstants.KEY_MEMBER_ASSET_SYNC_TIMESTAMP, System.currentTimeMillis());
        family.setAdditionalDetails(details);
    }
}
