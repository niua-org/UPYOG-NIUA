
package org.upyog.family.domain.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.domain.model.MemberStatus;
import org.upyog.family.domain.service.StateFamilySyncService;
import org.upyog.family.persistence.repository.FamilyRepository;
import org.upyog.family.persistence.repository.StateFamilySyncRepository;
import org.upyog.family.util.FamilyConstants;
import org.upyog.family.util.ResponseInfoFactory;
import org.upyog.family.web.contract.AuditDetails;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyMember;
import org.upyog.family.web.contract.FamilySearchCriteria;
import org.upyog.family.web.contract.state.StateFamily;
import org.upyog.family.web.contract.state.StateFamilyJitProvisionRequest;
import org.upyog.family.web.contract.state.StateFamilyMember;
import org.upyog.family.web.contract.state.StateFamilySyncRequest;
import org.upyog.family.web.contract.state.StateFamilySyncResponse;
import org.upyog.family.config.FamilyConfiguration;
import org.upyog.family.util.KafkaProducer;
import org.upyog.family.web.contract.FamilyRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link StateFamilySyncService} managing UC2 State Family Registry workflows.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class StateFamilySyncServiceImpl implements StateFamilySyncService {

    private final StateFamilySyncRepository stateFamilySyncRepository;
    private final FamilyRepository familyRepository;
    private final ResponseInfoFactory responseInfoFactory;
    private final ObjectMapper objectMapper;

    public StateFamilySyncServiceImpl(StateFamilySyncRepository stateFamilySyncRepository,
                                      FamilyRepository familyRepository,
                                      ResponseInfoFactory responseInfoFactory) {
        this(stateFamilySyncRepository, familyRepository, responseInfoFactory, new ObjectMapper());
    }

    @Autowired
    public StateFamilySyncServiceImpl(StateFamilySyncRepository stateFamilySyncRepository,
                                      FamilyRepository familyRepository,
                                      ResponseInfoFactory responseInfoFactory,
                                      ObjectMapper objectMapper) {
        this.stateFamilySyncRepository = stateFamilySyncRepository;
        this.familyRepository = familyRepository;
        this.responseInfoFactory = responseInfoFactory;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public StateFamilySyncResponse syncStateFamily(StateFamilySyncRequest request) {
        log.info("[STATE-FAMILY-SYNC-SVC] Received State Family Sync request from State Wrapper");

        if (request == null || request.getStateFamily() == null) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_STATE_FAMILY_REQUEST, "StateFamily payload cannot be null");
        }

        StateFamily stateFamily = request.getStateFamily();
        if (StringUtils.isBlank(stateFamily.getStateFamilyId())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_STATE_FAMILY_ID, "stateFamilyId is mandatory from State Registry");
        }
        if (StringUtils.isBlank(stateFamily.getTenantId())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_TENANT_ID, "tenantId is mandatory for state family sync");
        }
        if (CollectionUtils.isEmpty(stateFamily.getMembers())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_MEMBERS_LIST, "State Family must contain at least one member");
        }

        // Generate unique reference ID for UPYOG UI ingestion and session binding
        String referenceId = FamilyConstants.PREFIX_REF + UUID.randomUUID().toString();
        log.info("[STATE-FAMILY-SYNC-SVC] Staging state family: stateFamilyId={}, refId={}, membersCount={}",
                stateFamily.getStateFamilyId(), referenceId, stateFamily.getMembers().size());

        // Assign memberIds if absent
        int memberIndex = 1;
        for (StateFamilyMember member : stateFamily.getMembers()) {
            if (StringUtils.isBlank(member.getMemberId())) {
                member.setMemberId(FamilyConstants.PREFIX_MEM + memberIndex++);
            }
        }

        // Persist original payload in staging repository
        stateFamilySyncRepository.save(referenceId, stateFamily);

        // Build sanitized masked copy for "Family Found" UI response
        StateFamily maskedCopy = buildMaskedCopy(stateFamily);

        ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true);
        return StateFamilySyncResponse.builder()
                .responseInfo(responseInfo)
                .referenceId(referenceId)
                .stateFamily(maskedCopy)
                .status(FamilyConstants.STATUS_STAGED)
                .message("State Family successfully staged in UPYOG. Ready for Member Selection.")
                .build();
    }

    @Override
    public StateFamilySyncResponse getStateFamily(String referenceId, String stateFamilyId, RequestInfo requestInfo) {
        log.info("[STATE-FAMILY-SYNC-SVC] Fetching State Family for UI: refId={}, stateFamilyId={}",
                referenceId, stateFamilyId);

        StateFamily stateFamily = null;
        if (StringUtils.isNotBlank(referenceId)) {
            stateFamily = stateFamilySyncRepository.findByReferenceId(referenceId);
        }
        if (stateFamily == null && StringUtils.isNotBlank(stateFamilyId)) {
            stateFamily = stateFamilySyncRepository.findByStateFamilyId(stateFamilyId);
        }

        if (stateFamily == null) {
            log.error("[STATE-FAMILY-SYNC-SVC] State Family not found for refId: {}, stateFamilyId: {}",
                    referenceId, stateFamilyId);
            throw new CustomException(FamilyConstants.ERROR_STATE_FAMILY_NOT_FOUND, "State Family record not found in UPYOG staging");
        }

        StateFamily maskedCopy = buildMaskedCopy(stateFamily);
        ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(requestInfo, true);

        return StateFamilySyncResponse.builder()
                .responseInfo(responseInfo)
                .referenceId(referenceId)
                .stateFamily(maskedCopy)
                .status("FOUND")
                .message("State Family details retrieved successfully")
                .build();
    }

    @Override
    @Transactional
    public Family jitProvisionFamilyAndMember(StateFamilyJitProvisionRequest request) {
        String stateFamilyId = request.getStateFamilyId();
        String tenantId = request.getTenantId();
        String userUuid = request.getUserUuid();

        log.info("[STATE-FAMILY-SYNC-SVC] Executing JIT check & provision: stateFamilyId={}, userUuid={}, tenant={}",
                stateFamilyId, userUuid, tenantId);

        // Enforce Multi-Family Exclusivity: Verify citizen is not already an active member of another family
        if (StringUtils.isNotBlank(userUuid)) {
            List<String> activeFamilyIds = familyRepository.findActiveFamilyIdsByMemberUuid(userUuid);
            if (!CollectionUtils.isEmpty(activeFamilyIds)) {
                for (String activeFamId : activeFamilyIds) {
                    if (!activeFamId.equalsIgnoreCase(stateFamilyId)) {
                        log.error("[STATE-FAMILY-SYNC-SVC] Multi-family exclusivity violation: Citizen {} is already an active member of family: {}",
                                userUuid, activeFamId);
                        throw new CustomException(
                                FamilyConstants.ERROR_MEMBER_ALREADY_EXISTS,
                                "Citizen profile " + userUuid + " is already an active member of family: " + activeFamId + ". A citizen can only belong to one family."
                        );
                    }
                }
            }
        }

        // 1. Check whether the family already exists in UPYOG
        FamilySearchCriteria criteria = FamilySearchCriteria.builder()
                .familyId(stateFamilyId)
                .tenantId(tenantId)
                .build();
        List<Family> existingFamilies = familyRepository.search(criteria);

        long now = System.currentTimeMillis();

        // Branch A: Family DOES exist in UPYOG
        if (!CollectionUtils.isEmpty(existingFamilies)) {
            Family existingFamily = existingFamilies.get(0);
            log.info("[STATE-FAMILY-SYNC-SVC] Family EXISTS in UPYOG (familyId: {}). Checking member mapping for userUuid: {}",
                    stateFamilyId, userUuid);

            boolean memberAlreadyMapped = existingFamily.getMembers() != null && existingFamily.getMembers().stream()
                    .anyMatch(m -> Boolean.TRUE.equals(m.getActive()) && userUuid.equals(m.getUserUuid()));

            if (memberAlreadyMapped) {
                log.info("[STATE-FAMILY-SYNC-SVC] User {} is already an active member of family {}.",
                        userUuid, stateFamilyId);
                return existingFamily;
            }

            // Staged state family lookup to assist member matching
            StateFamily stagedStateFamily = null;
            if (StringUtils.isNotBlank(request.getReferenceId())) {
                stagedStateFamily = stateFamilySyncRepository.findByReferenceId(request.getReferenceId());
            }
            if (stagedStateFamily == null) {
                stagedStateFamily = stateFamilySyncRepository.findByStateFamilyId(stateFamilyId);
            }

            // Locate target StateFamilyMember from staged records if available
            StateFamilyMember stagedMember = null;
            int stagedMemberIndex = -1;
            if (stagedStateFamily != null && !CollectionUtils.isEmpty(stagedStateFamily.getMembers())) {
                for (int i = 0; i < stagedStateFamily.getMembers().size(); i++) {
                    StateFamilyMember sm = stagedStateFamily.getMembers().get(i);
                    boolean match = (StringUtils.isNotBlank(request.getSelectedMemberId()) && request.getSelectedMemberId().equals(sm.getMemberId()))
                            || (StringUtils.isNotBlank(request.getMobileNumber()) && request.getMobileNumber().equals(sm.getMobileNumber()))
                            || (StringUtils.isNotBlank(request.getAadhaarNumber()) && request.getAadhaarNumber().equals(sm.getAadhaarNumber()));
                    if (match) {
                        stagedMember = sm;
                        stagedMemberIndex = i;
                        break;
                    }
                }
            }

            // Identify eligible placeholder members awaiting citizen account linkage
            List<FamilyMember> placeholderMembers = existingFamily.getMembers() != null ? existingFamily.getMembers().stream()
                    .filter(m -> Boolean.TRUE.equals(m.getActive()) && (m.getUserUuid() == null || m.getUserUuid().startsWith(FamilyConstants.PREFIX_EXT)))
                    .collect(Collectors.toList()) : new ArrayList<>();

            FamilyMember matchedMember = null;
            String targetMemberId = request.getSelectedMemberId() != null ? request.getSelectedMemberId() : (stagedMember != null ? stagedMember.getMemberId() : null);
            String targetMobile = request.getMobileNumber() != null ? request.getMobileNumber() : (stagedMember != null ? stagedMember.getMobileNumber() : null);
            String targetAadhaar = request.getAadhaarNumber() != null ? request.getAadhaarNumber() : (stagedMember != null ? stagedMember.getAadhaarNumber() : null);

            // 1. Match by memberId in additionalDetails
            if (StringUtils.isNotBlank(targetMemberId)) {
                matchedMember = placeholderMembers.stream()
                        .filter(m -> targetMemberId.equals(extractFieldFromAdditionalDetails(m.getAdditionalDetails(), "memberId")))
                        .findFirst().orElse(null);
            }

            // 2. Match by mobileNumber
            if (matchedMember == null && StringUtils.isNotBlank(targetMobile)) {
                matchedMember = placeholderMembers.stream()
                        .filter(m -> targetMobile.equals(m.getMobileNumber()) || targetMobile.equals(extractFieldFromAdditionalDetails(m.getAdditionalDetails(), "mobileNumber")))
                        .findFirst().orElse(null);
            }

            // 3. Match by aadhaarNumber
            if (matchedMember == null && StringUtils.isNotBlank(targetAadhaar)) {
                matchedMember = placeholderMembers.stream()
                        .filter(m -> targetAadhaar.equals(extractFieldFromAdditionalDetails(m.getAdditionalDetails(), "aadhaarNumber"))
                                || maskAadhaar(targetAadhaar).equals(extractFieldFromAdditionalDetails(m.getAdditionalDetails(), "maskedAadhaar")))
                        .findFirst().orElse(null);
            }

            if (matchedMember != null) {
                log.info("[STATE-FAMILY-SYNC-SVC] Updating existing placeholder member (id={}, oldUuid={}) to userUuid={}",
                        matchedMember.getId(), matchedMember.getUserUuid(), userUuid);

                matchedMember.setUserUuid(userUuid);
                matchedMember.setIsVerified(true);
                matchedMember.setActive(true);
                matchedMember.setStatus(MemberStatus.ACTIVE);
                if (matchedMember.getAuditDetails() == null) {
                    matchedMember.setAuditDetails(AuditDetails.builder().createdBy(userUuid).createdTime(now).build());
                }
                matchedMember.getAuditDetails().setLastModifiedBy(userUuid);
                matchedMember.getAuditDetails().setLastModifiedTime(now);

                Map<String, Object> memberDetailsMap = getOrConvertToMap(matchedMember.getAdditionalDetails());
                if (StringUtils.isNotBlank(targetMemberId)) memberDetailsMap.put("memberId", targetMemberId);
                if (StringUtils.isNotBlank(targetMobile)) memberDetailsMap.put("mobileNumber", targetMobile);
                if (StringUtils.isNotBlank(targetAadhaar)) memberDetailsMap.put("maskedAadhaar", maskAadhaar(targetAadhaar));
                if (stagedMember != null) {
                    if (StringUtils.isNotBlank(stagedMember.getName()) && !memberDetailsMap.containsKey("name")) {
                        memberDetailsMap.put("name", stagedMember.getName());
                    }
                    if (StringUtils.isNotBlank(stagedMember.getGender()) && !memberDetailsMap.containsKey("gender")) {
                        memberDetailsMap.put("gender", stagedMember.getGender());
                    }
                }
                matchedMember.setAdditionalDetails(memberDetailsMap);

                if (Boolean.TRUE.equals(matchedMember.getIsHead()) || (existingFamily.getHeadUserUuid() != null && existingFamily.getHeadUserUuid().startsWith(FamilyConstants.PREFIX_EXT))) {
                    existingFamily.setHeadUserUuid(userUuid);
                }
            } else {
                // Member not mapped yet and no placeholder slot exists -> Add member to existing family
                log.info("[STATE-FAMILY-SYNC-SVC] No placeholder slot found. Mapping user {} as new member to existing family {}",
                        userUuid, stateFamilyId);

                Map<String, Object> newMemberDetails = new HashMap<>();
                if (StringUtils.isNotBlank(targetMemberId)) newMemberDetails.put("memberId", targetMemberId);
                if (StringUtils.isNotBlank(targetMobile)) newMemberDetails.put("mobileNumber", targetMobile);
                if (StringUtils.isNotBlank(targetAadhaar)) newMemberDetails.put("maskedAadhaar", maskAadhaar(targetAadhaar));
                if (stagedMember != null) {
                    if (StringUtils.isNotBlank(stagedMember.getName())) newMemberDetails.put("name", stagedMember.getName());
                    if (StringUtils.isNotBlank(stagedMember.getGender())) newMemberDetails.put("gender", stagedMember.getGender());
                }

                FamilyMember newMember = FamilyMember.builder()
                        .id(UUID.randomUUID().toString())
                        .familyId(existingFamily.getFamilyId())
                        .tenantId(tenantId)
                        .userUuid(userUuid)
                        .relationshipWithHead(stagedMember != null && StringUtils.isNotBlank(stagedMember.getRelationshipWithHead()) ? stagedMember.getRelationshipWithHead() : "MEMBER")
                        .isHead(stagedMember != null && Boolean.TRUE.equals(stagedMember.getIsHead()))
                        .isVerified(true)
                        .active(true)
                        .status(MemberStatus.ACTIVE)
                        .effectiveFrom(now)
                        .additionalDetails(newMemberDetails)
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
                existingFamily.getMembers().add(newMember);
            }

            if (existingFamily.getAuditDetails() != null) {
                existingFamily.getAuditDetails().setLastModifiedBy(userUuid);
                existingFamily.getAuditDetails().setLastModifiedTime(now);
            }
            return familyRepository.update(existingFamily);
        }

        // Branch B: Family DOES NOT exist in UPYOG -> Create Family & Map User
        log.info("[STATE-FAMILY-SYNC-SVC] Family DOES NOT exist in UPYOG. Creating new family unit for stateFamilyId: {}",
                stateFamilyId);

        StateFamily stagedStateFamily = null;
        if (StringUtils.isNotBlank(request.getReferenceId())) {
            stagedStateFamily = stateFamilySyncRepository.findByReferenceId(request.getReferenceId());
        }
        if (stagedStateFamily == null) {
            stagedStateFamily = stateFamilySyncRepository.findByStateFamilyId(stateFamilyId);
        }

        Map<String, Object> additionalDetails = new HashMap<>();
        additionalDetails.put("source", FamilyConstants.SOURCE_STATE_REGISTRY_WRAPPER);
        if (stagedStateFamily != null) {
            additionalDetails.put("stateCode", stagedStateFamily.getStateCode());
            additionalDetails.put("familyName", stagedStateFamily.getFamilyName());
            additionalDetails.put("address", stagedStateFamily.getAddress());
        }

        List<FamilyMember> members = new ArrayList<>();
        boolean headAssigned = false;

        if (stagedStateFamily != null && !CollectionUtils.isEmpty(stagedStateFamily.getMembers())) {
            for (StateFamilyMember sm : stagedStateFamily.getMembers()) {
                boolean isSelectedMember = (StringUtils.isNotBlank(request.getSelectedMemberId()) && request.getSelectedMemberId().equals(sm.getMemberId()))
                        || (StringUtils.isNotBlank(request.getMobileNumber()) && request.getMobileNumber().equals(sm.getMobileNumber()))
                        || (StringUtils.isNotBlank(request.getAadhaarNumber()) && request.getAadhaarNumber().equals(sm.getAadhaarNumber()));

                String memberUserUuid = isSelectedMember ? userUuid : FamilyConstants.PREFIX_EXT + UUID.randomUUID().toString().substring(0, 16);
                boolean isHead = Boolean.TRUE.equals(sm.getIsHead()) || (isSelectedMember && !headAssigned);
                if (isHead) headAssigned = true;

                Map<String, Object> memberAdditionalDetails = new HashMap<>();
                if (StringUtils.isNotBlank(sm.getMemberId())) {
                    memberAdditionalDetails.put("memberId", sm.getMemberId());
                }
                if (StringUtils.isNotBlank(sm.getName())) {
                    memberAdditionalDetails.put("name", sm.getName());
                }
                if (StringUtils.isNotBlank(sm.getMobileNumber())) {
                    memberAdditionalDetails.put("mobileNumber", sm.getMobileNumber());
                }
                if (StringUtils.isNotBlank(sm.getGender())) {
                    memberAdditionalDetails.put("gender", sm.getGender());
                }
                if (StringUtils.isNotBlank(sm.getAadhaarNumber())) {
                    memberAdditionalDetails.put("aadhaarNumber", sm.getAadhaarNumber());
                }

                FamilyMember fm = FamilyMember.builder()
                        .id(UUID.randomUUID().toString())
                        .familyId(stateFamilyId)
                        .tenantId(tenantId)
                        .userUuid(memberUserUuid)
                        .name(sm.getName())
                        .mobileNumber(sm.getMobileNumber())
                        .gender(sm.getGender())
                        .relationshipWithHead(StringUtils.isNotBlank(sm.getRelationshipWithHead()) ? sm.getRelationshipWithHead() : (isHead ? "HEAD" : "MEMBER"))
                        .isHead(isHead)
                        .isVerified(isSelectedMember)
                        .active(true)
                        .status(MemberStatus.ACTIVE)
                        .effectiveFrom(now)
                        .additionalDetails(memberAdditionalDetails)
                        .auditDetails(AuditDetails.builder()
                                .createdBy(userUuid)
                                .createdTime(now)
                                .lastModifiedBy(userUuid)
                                .lastModifiedTime(now)
                                .build())
                        .build();
                members.add(fm);
            }
        }

        // If no members derived from staging, add the authenticated citizen as HoF
        if (members.isEmpty()) {
            FamilyMember hofMember = FamilyMember.builder()
                    .id(UUID.randomUUID().toString())
                    .familyId(stateFamilyId)
                    .tenantId(tenantId)
                    .userUuid(userUuid)
                    .relationshipWithHead("HEAD")
                    .isHead(true)
                    .isVerified(true)
                    .active(true)
                    .status(MemberStatus.ACTIVE)
                    .effectiveFrom(now)
                    .auditDetails(AuditDetails.builder()
                            .createdBy(userUuid)
                            .createdTime(now)
                            .lastModifiedBy(userUuid)
                            .lastModifiedTime(now)
                            .build())
                    .build();
            members.add(hofMember);
        }

        Family newFamily = Family.builder()
                .id(UUID.randomUUID().toString())
                .familyId(stateFamilyId)
                .tenantId(tenantId)
                .headUserUuid(userUuid)
                .category(stagedStateFamily != null && StringUtils.isNotBlank(stagedStateFamily.getCategory()) ? stagedStateFamily.getCategory() : FamilyConstants.CATEGORY_GENERAL)
                .status(FamilyStatus.ACTIVE)
                .additionalDetails(additionalDetails)
                .members(members)
                .auditDetails(AuditDetails.builder()
                        .createdBy(userUuid)
                        .createdTime(now)
                        .lastModifiedBy(userUuid)
                        .lastModifiedTime(now)
                        .build())
                .build();

        Family savedFamily = familyRepository.save(newFamily);
        log.info("[STATE-FAMILY-SYNC-SVC] Successfully created Family unit and mapped user in UPYOG: familyId={}",
                savedFamily.getFamilyId());

        return savedFamily;
    }

    /**
     * Builds a sanitized copy of StateFamily with masked Mobile numbers and Aadhaar numbers for UI display.
     */
    private StateFamily buildMaskedCopy(StateFamily original) {
        List<StateFamilyMember> maskedMembers = new ArrayList<>();
        if (!CollectionUtils.isEmpty(original.getMembers())) {
            for (StateFamilyMember m : original.getMembers()) {
                StateFamilyMember copy = StateFamilyMember.builder()
                        .memberId(m.getMemberId())
                        .name(m.getName())
                        .relationshipWithHead(m.getRelationshipWithHead())
                        .isHead(m.getIsHead())
                        .gender(m.getGender())
                        .dob(m.getDob())
                        .isVerified(m.getIsVerified())
                        .userUuid(m.getUserUuid())
                        .maskedMobile(maskMobile(m.getMobileNumber()))
                        .maskedAadhaar(maskAadhaar(m.getAadhaarNumber()))
                        .build();
                maskedMembers.add(copy);
            }
        }

        return StateFamily.builder()
                .stateCode(original.getStateCode())
                .stateFamilyId(original.getStateFamilyId())
                .familyName(original.getFamilyName())
                .tenantId(original.getTenantId())
                .category(original.getCategory())
                .address(original.getAddress())
                .members(maskedMembers)
                .build();
    }

    private String maskMobile(String mobile) {
        if (StringUtils.isBlank(mobile)) return null;
        if (mobile.length() >= 6) {
            return mobile.substring(0, 2) + "******" + mobile.substring(mobile.length() - 2);
        }
        return "******";
    }

    private String maskAadhaar(String aadhaar) {
        if (StringUtils.isBlank(aadhaar)) return null;
        String clean = aadhaar.trim().replaceAll("\\s+", "");
        if (clean.length() == 12) {
            return "XXXXXXXX" + clean.substring(8);
        }
        return "XXXXXXXX" + (clean.length() >= 4 ? clean.substring(clean.length() - 4) : "****");
    }

    private String extractFieldFromAdditionalDetails(Object additionalDetails, String fieldName) {
        if (additionalDetails == null) {
            return null;
        }
        if (additionalDetails instanceof JsonNode jsonNode) {
            JsonNode node = jsonNode.get(fieldName);
            return (node != null && !node.isNull()) ? node.asText() : null;
        }
        if (additionalDetails instanceof Map<?, ?> map) {
            Object val = map.get(fieldName);
            return val != null ? val.toString() : null;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getOrConvertToMap(Object additionalDetails) {
        if (additionalDetails == null) {
            return new HashMap<>();
        }
        if (additionalDetails instanceof Map<?, ?> map) {
            return new HashMap<>((Map<String, Object>) map);
        }
        if (additionalDetails instanceof JsonNode jsonNode) {
            try {
                return objectMapper.convertValue(jsonNode, Map.class);
            } catch (Exception e) {
                log.warn("[STATE-FAMILY-SYNC-SVC] Error converting JsonNode to Map: {}", e.getMessage());
            }
        }
        return new HashMap<>();
    }
}
