package org.upyog.family.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.upyog.family.domain.model.LineageActionType;
import org.upyog.family.persistence.builder.FamilyQueryBuilder;
import org.upyog.family.persistence.rowmapper.FamilyRowMapper;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyDocument;
import org.upyog.family.web.contract.FamilyLineage;
import org.upyog.family.web.contract.FamilyMember;
import org.upyog.family.web.contract.FamilySearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JDBC-backed repository implementation persisting family entities to ug_ tables in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Repository
@Slf4j
public class FamilyRepositoryImpl implements FamilyRepository {

    private final JdbcTemplate jdbcTemplate;
    private final FamilyQueryBuilder queryBuilder;
    private final FamilyRowMapper rowMapper;
    private final ObjectMapper objectMapper;

    @Autowired
    public FamilyRepositoryImpl(JdbcTemplate jdbcTemplate,
                                FamilyQueryBuilder queryBuilder,
                                FamilyRowMapper rowMapper,
                                ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.queryBuilder = queryBuilder;
        this.rowMapper = rowMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * Persists a new Family aggregate, members, and documents to ug_family, ug_family_member, and ug_family_document.
     *
     * @param family the Family aggregate to persist
     * @return the persisted Family aggregate
     */
    @Override
    @Transactional
    public Family save(Family family) {
        log.info("[UPYOG-FAMILY-REPO] Saving new family: familyId={}, tenant={}", family.getFamilyId(), family.getTenantId());

        if (StringUtils.isBlank(family.getId())) {
            family.setId(UUID.randomUUID().toString());
        }

        String additionalDetailsJson = convertToJsonString(family.getAdditionalDetails());

        jdbcTemplate.update(FamilyQueryBuilder.INSERT_FAMILY_SQL,
                family.getId(),
                family.getFamilyId(),
                family.getTenantId(),
                family.getHeadUserUuid(),
                family.getCategory(),
                family.getStatus() != null ? family.getStatus().name() : "ACTIVE",
                additionalDetailsJson,
                family.getAuditDetails() != null ? family.getAuditDetails().getCreatedBy() : null,
                family.getAuditDetails() != null ? family.getAuditDetails().getCreatedTime() : null,
                family.getAuditDetails() != null ? family.getAuditDetails().getLastModifiedBy() : null,
                family.getAuditDetails() != null ? family.getAuditDetails().getLastModifiedTime() : null
        );

        if (family.getMembers() != null) {
            for (FamilyMember member : family.getMembers()) {
                if (StringUtils.isBlank(member.getId())) {
                    member.setId(UUID.randomUUID().toString());
                }
                member.setFamilyId(family.getFamilyId());
                member.setTenantId(family.getTenantId());

                String memberDetailsJson = convertToJsonString(member.getAdditionalDetails());

                jdbcTemplate.update(FamilyQueryBuilder.INSERT_MEMBER_SQL,
                        member.getId(),
                        member.getFamilyId(),
                        member.getTenantId(),
                        member.getUserUuid(),
                        member.getRelationshipWithHead(),
                        Boolean.TRUE.equals(member.getIsHead()),
                        Boolean.TRUE.equals(member.getIsVerified()),
                        member.getActive() != null ? member.getActive() : Boolean.TRUE,
                        member.getStatus() != null ? member.getStatus().name() : (Boolean.FALSE.equals(member.getActive()) ? "EXITED" : "ACTIVE"),
                        member.getExitReason() != null ? member.getExitReason().name() : null,
                        member.getExitDate(),
                        member.getConsentToken(),
                        member.getConsentDate(),
                        member.getEffectiveFrom(),
                        member.getEffectiveTo(),
                        memberDetailsJson,
                        member.getAuditDetails() != null ? member.getAuditDetails().getCreatedBy() : null,
                        member.getAuditDetails() != null ? member.getAuditDetails().getCreatedTime() : null,
                        member.getAuditDetails() != null ? member.getAuditDetails().getLastModifiedBy() : null,
                        member.getAuditDetails() != null ? member.getAuditDetails().getLastModifiedTime() : null
                );
            }
        }

        if (family.getDocuments() != null) {
            for (FamilyDocument doc : family.getDocuments()) {
                if (StringUtils.isBlank(doc.getId())) {
                    doc.setId(UUID.randomUUID().toString());
                }
                doc.setFamilyId(family.getFamilyId());
                doc.setTenantId(family.getTenantId());

                jdbcTemplate.update(FamilyQueryBuilder.INSERT_DOC_SQL,
                        doc.getId(),
                        doc.getFamilyId(),
                        doc.getTenantId(),
                        doc.getDocumentType(),
                        doc.getFilestoreId(),
                        doc.getActive() != null ? doc.getActive() : Boolean.TRUE,
                        doc.getAuditDetails() != null ? doc.getAuditDetails().getCreatedBy() : null,
                        doc.getAuditDetails() != null ? doc.getAuditDetails().getCreatedTime() : null,
                        doc.getAuditDetails() != null ? doc.getAuditDetails().getLastModifiedBy() : null,
                        doc.getAuditDetails() != null ? doc.getAuditDetails().getLastModifiedTime() : null
                );
            }
        }

        return family;
    }

    /**
     * Updates an existing Family aggregate and synchronizes members and document records in UPYOG.
     *
     * @param family the updated Family aggregate
     * @return the updated Family aggregate
     */
    @Override
    @Transactional
    public Family update(Family family) {
        log.info("[UPYOG-FAMILY-REPO] Updating family: familyId={}, status={}", family.getFamilyId(), family.getStatus());

        String additionalDetailsJson = convertToJsonString(family.getAdditionalDetails());

        jdbcTemplate.update(FamilyQueryBuilder.UPDATE_FAMILY_SQL,
                family.getHeadUserUuid(),
                family.getCategory(),
                family.getStatus() != null ? family.getStatus().name() : "ACTIVE",
                additionalDetailsJson,
                family.getAuditDetails() != null ? family.getAuditDetails().getLastModifiedBy() : null,
                family.getAuditDetails() != null ? family.getAuditDetails().getLastModifiedTime() : null,
                family.getFamilyId()
        );

        if (family.getMembers() != null) {
            for (FamilyMember member : family.getMembers()) {
                String memberDetailsJson = convertToJsonString(member.getAdditionalDetails());

                if (StringUtils.isNotBlank(member.getId()) && existsMember(member.getId())) {
                    jdbcTemplate.update(FamilyQueryBuilder.UPDATE_MEMBER_SQL,
                            member.getUserUuid(),
                            member.getRelationshipWithHead(),
                            Boolean.TRUE.equals(member.getIsHead()),
                            Boolean.TRUE.equals(member.getIsVerified()),
                            member.getActive() != null ? member.getActive() : Boolean.TRUE,
                            member.getStatus() != null ? member.getStatus().name() : (Boolean.FALSE.equals(member.getActive()) ? "EXITED" : "ACTIVE"),
                            member.getExitReason() != null ? member.getExitReason().name() : null,
                            member.getExitDate(),
                            member.getConsentToken(),
                            member.getConsentDate(),
                            member.getEffectiveFrom(),
                            member.getEffectiveTo(),
                            memberDetailsJson,
                            member.getAuditDetails() != null ? member.getAuditDetails().getLastModifiedBy() : null,
                            member.getAuditDetails() != null ? member.getAuditDetails().getLastModifiedTime() : null,
                            member.getId()
                    );
                } else {
                    if (StringUtils.isBlank(member.getId())) {
                        member.setId(UUID.randomUUID().toString());
                    }
                    member.setFamilyId(family.getFamilyId());
                    member.setTenantId(family.getTenantId());

                    jdbcTemplate.update(FamilyQueryBuilder.INSERT_MEMBER_SQL,
                            member.getId(),
                            member.getFamilyId(),
                            member.getTenantId(),
                            member.getUserUuid(),
                            member.getRelationshipWithHead(),
                            Boolean.TRUE.equals(member.getIsHead()),
                            Boolean.TRUE.equals(member.getIsVerified()),
                            member.getActive() != null ? member.getActive() : Boolean.TRUE,
                            member.getStatus() != null ? member.getStatus().name() : (Boolean.FALSE.equals(member.getActive()) ? "EXITED" : "ACTIVE"),
                            member.getExitReason() != null ? member.getExitReason().name() : null,
                            member.getExitDate(),
                            member.getConsentToken(),
                            member.getConsentDate(),
                            member.getEffectiveFrom(),
                            member.getEffectiveTo(),
                            memberDetailsJson,
                            member.getAuditDetails() != null ? member.getAuditDetails().getCreatedBy() : null,
                            member.getAuditDetails() != null ? member.getAuditDetails().getCreatedTime() : null,
                            member.getAuditDetails() != null ? member.getAuditDetails().getLastModifiedBy() : null,
                            member.getAuditDetails() != null ? member.getAuditDetails().getLastModifiedTime() : null
                    );
                }
            }
        }

        if (family.getDocuments() != null) {
            for (FamilyDocument doc : family.getDocuments()) {
                if (StringUtils.isNotBlank(doc.getId()) && existsDocument(doc.getId())) {
                    jdbcTemplate.update(FamilyQueryBuilder.UPDATE_DOC_SQL,
                            doc.getActive() != null ? doc.getActive() : Boolean.TRUE,
                            doc.getAuditDetails() != null ? doc.getAuditDetails().getLastModifiedBy() : null,
                            doc.getAuditDetails() != null ? doc.getAuditDetails().getLastModifiedTime() : null,
                            doc.getId()
                    );
                } else {
                    if (StringUtils.isBlank(doc.getId())) {
                        doc.setId(UUID.randomUUID().toString());
                    }
                    doc.setFamilyId(family.getFamilyId());
                    doc.setTenantId(family.getTenantId());

                    jdbcTemplate.update(FamilyQueryBuilder.INSERT_DOC_SQL,
                            doc.getId(),
                            doc.getFamilyId(),
                            doc.getTenantId(),
                            doc.getDocumentType(),
                            doc.getFilestoreId(),
                            doc.getActive() != null ? doc.getActive() : Boolean.TRUE,
                            doc.getAuditDetails() != null ? doc.getAuditDetails().getCreatedBy() : null,
                            doc.getAuditDetails() != null ? doc.getAuditDetails().getCreatedTime() : null,
                            doc.getAuditDetails() != null ? doc.getAuditDetails().getLastModifiedBy() : null,
                            doc.getAuditDetails() != null ? doc.getAuditDetails().getLastModifiedTime() : null
                    );
                }
            }
        }

        return family;
    }

    /**
     * Executes dynamic SQL query search for family units in UPYOG using JdbcTemplate and FamilyRowMapper.
     *
     * @param criteria query search filters
     * @return list of matching Family domain objects
     */
    @Override
    public List<Family> search(FamilySearchCriteria criteria) {
        List<Object> preparedStmtList = new ArrayList<>();
        String sql = queryBuilder.getFamilySearchQuery(criteria, preparedStmtList);
        log.debug("[UPYOG-FAMILY-REPO] Executing search SQL: {} with params: {}", sql, preparedStmtList);
        return jdbcTemplate.query(sql, preparedStmtList.toArray(), rowMapper);
    }

    /**
     * Queries active family IDs for a citizen UUID to enforce multi-family exclusivity in UPYOG.
     *
     * @param userUuid citizen user UUID
     * @return list of active family ID strings
     */
    @Override
    public List<String> findActiveFamilyIdsByMemberUuid(String userUuid) {
        List<Object> preparedStmtList = new ArrayList<>();
        String sql = queryBuilder.getActiveFamilyByMemberUuidQuery(userUuid, preparedStmtList);
        return jdbcTemplate.queryForList(sql, preparedStmtList.toArray(), String.class);
    }

    /**
     * Inserts a historical genealogy event into ug_family_lineage in UPYOG.
     *
     * @param lineage the lineage event record
     */
    @Override
    @Transactional
    public void saveLineage(FamilyLineage lineage) {
        log.info("[UPYOG-FAMILY-REPO] Persisting family lineage: parent={}, child={}, action={}",
                lineage.getParentFamilyId(), lineage.getChildFamilyId(), lineage.getActionType());

        if (StringUtils.isBlank(lineage.getId())) {
            lineage.setId(UUID.randomUUID().toString());
        }

        jdbcTemplate.update(FamilyQueryBuilder.INSERT_LINEAGE_SQL,
                lineage.getId(),
                lineage.getParentFamilyId(),
                lineage.getChildFamilyId(),
                lineage.getActionType() != null ? lineage.getActionType().name() : null,
                lineage.getReason(),
                lineage.getCreatedTime() != null ? lineage.getCreatedTime() : System.currentTimeMillis()
        );
    }

    /**
     * Retrieves lineage history for a family unit from ug_family_lineage in UPYOG.
     *
     * @param familyId unique family identifier
     * @return list of FamilyLineage events
     */
    @Override
    public List<FamilyLineage> findLineageByFamilyId(String familyId) {
        String sql = FamilyQueryBuilder.SELECT_LINEAGE_BY_FAMILY_ID_SQL;
        return jdbcTemplate.query(sql, (rs, rowNum) -> FamilyLineage.builder()
                .id(rs.getString("id"))
                .parentFamilyId(rs.getString("parent_family_id"))
                .childFamilyId(rs.getString("child_family_id"))
                .actionType(LineageActionType.fromValue(rs.getString("action_type")))
                .reason(rs.getString("reason"))
                .createdTime(rs.getLong("created_time"))
                .build(), familyId, familyId);
    }

    private boolean existsMember(String memberId) {
        Integer count = jdbcTemplate.queryForObject(FamilyQueryBuilder.EXISTS_MEMBER_SQL, Integer.class, memberId);
        return count != null && count > 0;
    }

    private boolean existsDocument(String docId) {
        Integer count = jdbcTemplate.queryForObject(FamilyQueryBuilder.EXISTS_DOC_SQL, Integer.class, docId);
        return count != null && count > 0;
    }

    private String convertToJsonString(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("[UPYOG-FAMILY-REPO] Error converting object to JSON string", e);
            return null;
        }
    }
}
