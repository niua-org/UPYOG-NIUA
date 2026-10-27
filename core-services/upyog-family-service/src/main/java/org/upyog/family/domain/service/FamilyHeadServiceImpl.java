package org.upyog.family.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.upyog.family.config.FamilyConfiguration;
import org.upyog.family.domain.validator.AssignHofValidator;
import org.upyog.family.persistence.repository.FamilyRepository;
import org.upyog.family.persistence.repository.UserClient;
import org.upyog.family.util.FamilyConstants;
import org.upyog.family.util.KafkaProducer;
import org.upyog.family.web.contract.AssignHofRequest;
import org.upyog.family.web.contract.CitizenProfile;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyMember;
import org.upyog.family.web.contract.FamilyRequest;
import org.upyog.family.web.contract.FamilySearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Domain service implementation orchestrating Head of Family succession and transfer flows in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class FamilyHeadServiceImpl implements FamilyHeadService {

    private final FamilyRepository familyRepository;
    private final AssignHofValidator assignHofValidator;
    private final UserClient userClient;
    private final KafkaProducer kafkaProducer;
    private final FamilyConfiguration configuration;

    @Autowired
    public FamilyHeadServiceImpl(FamilyRepository familyRepository,
                                 AssignHofValidator assignHofValidator,
                                 UserClient userClient,
                                 KafkaProducer kafkaProducer,
                                 FamilyConfiguration configuration) {
        this.familyRepository = familyRepository;
        this.assignHofValidator = assignHofValidator;
        this.userClient = userClient;
        this.kafkaProducer = kafkaProducer;
        this.configuration = configuration;
    }

    /**
     * Transfers or assigns the Head of Family (HoF) role to an existing active member of the family unit in UPYOG.
     * Validates that the target member is active, revokes head status from the current head, and updates family metadata.
     *
     * @param request HoF assignment payload containing family ID, new head UUID, and optional justification
     * @return updated Family aggregate
     */
    @Override
    @Transactional
    public Family assignHeadOfFamily(AssignHofRequest request) {
        log.info("[UPYOG-HEAD-SERVICE] Transferring HoF role to: {} in familyId: {}",
                request.getNewHeadUserUuid(), request.getFamilyId());

        Family existingFamily = getFamilyByIdAndTenant(request.getFamilyId(), request.getTenantId());
        assignHofValidator.validate(request, existingFamily);

        RequestInfo requestInfo = request.getRequestInfo();
        String userUuid = requestInfo != null && requestInfo.getUserInfo() != null
                ? requestInfo.getUserInfo().getUuid()
                : FamilyConstants.SYSTEM_USER;
        Long now = System.currentTimeMillis();

        existingFamily.setHeadUserUuid(request.getNewHeadUserUuid());
        existingFamily.getAuditDetails().setLastModifiedBy(userUuid);
        existingFamily.getAuditDetails().setLastModifiedTime(now);

        if (existingFamily.getMembers() != null) {
            for (FamilyMember member : existingFamily.getMembers()) {
                boolean isNewHead = request.getNewHeadUserUuid().equals(member.getUserUuid());
                member.setIsHead(isNewHead);
                if (isNewHead) {
                    member.setRelationshipWithHead(FamilyConstants.RELATIONSHIP_HEAD);
                }
                if (member.getAuditDetails() != null) {
                    member.getAuditDetails().setLastModifiedBy(userUuid);
                    member.getAuditDetails().setLastModifiedTime(now);
                }
            }
        }

        // Persist to Database via Kafka Persister (standard UPYOG pattern)
        kafkaProducer.push(configuration.getUpdateFamilyTopic(), new FamilyRequest(requestInfo, existingFamily));

        enrichCitizenDemographics(Collections.singletonList(existingFamily), requestInfo);
        log.info("[UPYOG-HEAD-SERVICE] Successfully queued HoF role transfer to citizen: {} in family: {} in Kafka persister",
                request.getNewHeadUserUuid(), existingFamily.getFamilyId());

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
}
