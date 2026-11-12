package org.upyog.family.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.tracer.model.CustomException;
import org.upyog.family.persistence.repository.FamilyRepository;
import org.upyog.family.persistence.repository.UserClient;
import org.upyog.family.util.FamilyConstants;
import org.upyog.family.util.ResponseInfoFactory;
import org.upyog.family.web.contract.CitizenProfile;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyDashboardMetrics;
import org.upyog.family.web.contract.FamilyDashboardResponse;
import org.upyog.family.web.contract.FamilyLineage;
import org.upyog.family.web.contract.FamilyMember;
import org.upyog.family.web.contract.FamilySearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Domain service implementation compiling family dashboard metrics and lineage insights in UPYOG.
 *
 * <p>Implements {@link FamilyDashboardService} by aggregating real-time demographic distributions,
 * identity verification ratios, document proof attachments, and genealogical lineage records.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class FamilyDashboardServiceImpl implements FamilyDashboardService {

    private final FamilyRepository familyRepository;
    private final UserClient userClient;
    private final ResponseInfoFactory responseInfoFactory;

    /**
     * Constructs {@link FamilyDashboardServiceImpl} with required repository and client dependencies.
     *
     * @param familyRepository data repository for family lookups and lineage queries
     * @param userClient client querying {@code egov-user} for citizen profiles
     * @param responseInfoFactory utility creating standard UPYOG {@link ResponseInfo} headers
     */
    @Autowired
    public FamilyDashboardServiceImpl(FamilyRepository familyRepository,
                                     UserClient userClient,
                                     ResponseInfoFactory responseInfoFactory) {
        this.familyRepository = familyRepository;
        this.userClient = userClient;
        this.responseInfoFactory = responseInfoFactory;
    }

    /**
     * Computes and returns aggregated household analytics for a family unit in UPYOG.
     *
     * <p>Loads the family aggregate, enriches member profiles with citizen identity details, computes active
     * vs inactive counts, calculates verification ratio, counts attached documents, and compiles lineage history.</p>
     *
     * @param familyId unique family identifier; must not be blank.
     * @param tenantId tenant identifier; must not be blank.
     * @param requestInfo UPYOG request context envelope containing auth tokens.
     * @return {@link FamilyDashboardResponse} containing computed metrics and demographic breakdowns.
     * @throws CustomException if the family is not found (Code: {@code FAMILY_NOT_FOUND}).
     */
    @Override
    public FamilyDashboardResponse getFamilyDashboard(String familyId, String tenantId, RequestInfo requestInfo) {
        log.info("[UPYOG-DASHBOARD-SERVICE] Aggregating dashboard for family: {}, tenant: {}", familyId, tenantId);

        Family family = familyRepository.findByFamilyIdAndTenantId(familyId, tenantId)
                .orElseThrow(() -> new CustomException(FamilyConstants.ERROR_FAMILY_NOT_FOUND,
                        "Family with ID " + familyId + " not found."));
        enrichCitizenDemographics(Collections.singletonList(family), requestInfo);

        List<FamilyMember> members = family.getMembers() != null ? new java.util.ArrayList<>(family.getMembers()) : new java.util.ArrayList<>();

        // Ensure the Head of Family is present in the members roster
        boolean headPresentInMembers = members.stream()
                .anyMatch(m -> StringUtils.isNotBlank(family.getHeadUserUuid()) && family.getHeadUserUuid().equals(m.getUserUuid()));

        if (!headPresentInMembers && StringUtils.isNotBlank(family.getHeadUserUuid())) {
            Map<String, CitizenProfile> headProfileMap = userClient.searchUsersByUuids(
                    Collections.singletonList(family.getHeadUserUuid()), requestInfo);
            CitizenProfile headProfile = headProfileMap != null ? headProfileMap.get(family.getHeadUserUuid()) : null;

            FamilyMember headMember = FamilyMember.builder()
                    .id(null)
                    .familyId(family.getFamilyId())
                    .tenantId(family.getTenantId())
                    .userUuid(family.getHeadUserUuid())
                    .relationshipWithHead(FamilyConstants.RELATIONSHIP_SELF)
                    .isHead(true)
                    .isVerified(headProfile != null && Boolean.TRUE.equals(headProfile.getIsIdentityVerified()))
                    .active(true)
                    .status(org.upyog.family.domain.model.MemberStatus.ACTIVE)
                    .name(headProfile != null ? headProfile.getName() : null)
                    .mobileNumber(headProfile != null ? headProfile.getMobileNumber() : null)
                    .gender(headProfile != null ? headProfile.getGender() : null)
                    .photo(headProfile != null ? headProfile.getPhoto() : null)
                    .dob(headProfile != null ? headProfile.getDob() : null)
                    .citizenDetails(headProfile)
                    .build();

            members.add(0, headMember);
            family.setMembers(members);
            if (headProfile != null && StringUtils.isNotBlank(headProfile.getName())) {
                family.setHeadName(headProfile.getName());
            }
        }

        int totalMembers = members.size();
        List<FamilyMember> activeMembers = members.stream()
                .filter(m -> Boolean.TRUE.equals(m.getActive()))
                .sorted((m1, m2) -> Boolean.compare(Boolean.TRUE.equals(m2.getIsHead()), Boolean.TRUE.equals(m1.getIsHead())))
                .collect(Collectors.toList());
        int activeCount = activeMembers.size();
        int verifiedCount = (int) members.stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsVerified()))
                .count();

        int documentsCount = family.getDocuments() != null
                ? (int) family.getDocuments().stream().filter(d -> Boolean.TRUE.equals(d.getActive())).count()
                : 0;

        List<FamilyLineage> lineages = familyRepository.findLineageByFamilyId(familyId);
        int lineageCount = lineages.size();

        String headName = StringUtils.isNotBlank(family.getHeadName())
                ? family.getHeadName()
                : null;
        if (StringUtils.isBlank(headName) && StringUtils.isNotBlank(family.getHeadUserUuid())) {
            for (FamilyMember m : members) {
                if (family.getHeadUserUuid().equals(m.getUserUuid())) {
                    if (StringUtils.isNotBlank(m.getName())) {
                        headName = m.getName();
                    } else if (m.getCitizenDetails() != null && StringUtils.isNotBlank(m.getCitizenDetails().getName())) {
                        headName = m.getCitizenDetails().getName();
                    }
                    break;
                }
            }
        }

        FamilyDashboardMetrics metrics = FamilyDashboardMetrics.builder()
                .familyId(family.getFamilyId())
                .tenantId(family.getTenantId())
                .familyStatus(family.getStatus() != null ? family.getStatus().name() : "ACTIVE")
                .headUserUuid(family.getHeadUserUuid())
                .headName(headName)
                .totalMembersCount(totalMembers)
                .activeMembersCount(activeCount)
                .verifiedMembersCount(verifiedCount)
                .documentsCount(documentsCount)
                .lineageEventsCount(lineageCount)
                .activeMembers(activeMembers)
                .recentLineage(lineages)
                .build();

        ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(requestInfo, true);
        return FamilyDashboardResponse.builder()
                .responseInfo(responseInfo)
                .dashboard(metrics)
                .build();
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
