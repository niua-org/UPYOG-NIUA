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
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class FamilyDashboardServiceImpl implements FamilyDashboardService {

    private final FamilyRepository familyRepository;
    private final UserClient userClient;
    private final ResponseInfoFactory responseInfoFactory;

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
     * Aggregates active member counts, identity verification ratio, attached document counts, and lineage events.
     *
     * @param familyId unique family identifier
     * @param tenantId tenant identifier
     * @param requestInfo UPYOG request context envelope
     * @return FamilyDashboardResponse containing aggregated metrics
     */
    @Override
    public FamilyDashboardResponse getFamilyDashboard(String familyId, String tenantId, RequestInfo requestInfo) {
        log.info("[UPYOG-DASHBOARD-SERVICE] Aggregating dashboard for family: {}, tenant: {}", familyId, tenantId);

        FamilySearchCriteria criteria = FamilySearchCriteria.builder()
                .familyId(familyId)
                .tenantId(tenantId)
                .build();

        List<Family> families = familyRepository.search(criteria);
        if (families.isEmpty()) {
            throw new CustomException(FamilyConstants.ERROR_FAMILY_NOT_FOUND,
                    "Family with ID " + familyId + " not found.");
        }

        Family family = families.get(0);
        enrichCitizenDemographics(Collections.singletonList(family), requestInfo);

        List<FamilyMember> members = family.getMembers() != null ? family.getMembers() : Collections.emptyList();
        int totalMembers = members.size();
        List<FamilyMember> activeMembers = members.stream()
                .filter(m -> Boolean.TRUE.equals(m.getActive()))
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

        String headName = null;
        if (StringUtils.isNotBlank(family.getHeadUserUuid())) {
            for (FamilyMember m : members) {
                if (family.getHeadUserUuid().equals(m.getUserUuid()) && m.getCitizenDetails() != null) {
                    headName = m.getCitizenDetails().getName();
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
