package org.upyog.family.persistence.rowmapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.upyog.family.domain.model.ExitReason;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.domain.model.MemberStatus;
import org.upyog.family.web.contract.AuditDetails;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyDocument;
import org.upyog.family.web.contract.FamilyMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ResultSetExtractor aggregating flat relational rows into hierarchical Family domain objects in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class FamilyRowMapper implements ResultSetExtractor<List<Family>> {

    private final ObjectMapper objectMapper;

    @Autowired
    public FamilyRowMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Maps and aggregates relational result set rows from ug_family joined with ug_family_member
     * and ug_family_document into rich Family aggregate domain objects in UPYOG.
     *
     * @param rs SQL ResultSet containing joined family rows
     * @return list of distinct Family domain aggregates
     * @throws SQLException in case of SQL errors
     * @throws DataAccessException in case of Spring JDBC data access errors
     */
    @Override
    public List<Family> extractData(ResultSet rs) throws SQLException, DataAccessException {
        Map<String, Family> familyMap = new LinkedHashMap<>();
        Map<String, Set<String>> memberIdMap = new LinkedHashMap<>();
        Map<String, Set<String>> docIdMap = new LinkedHashMap<>();

        while (rs.next()) {
            String familyId = rs.getString("f_family_id");
            if (StringUtils.isBlank(familyId)) {
                continue;
            }

            Family family = familyMap.get(familyId);
            if (family == null) {
                AuditDetails familyAudit = AuditDetails.builder()
                        .createdBy(rs.getString("f_created_by"))
                        .createdTime(rs.getLong("f_created_time"))
                        .lastModifiedBy(rs.getString("f_last_modified_by"))
                        .lastModifiedTime(rs.getLong("f_last_modified_time"))
                        .build();

                Object additionalDetails = null;
                String additionalDetailsJson = rs.getString("f_additional_details");
                if (StringUtils.isNotBlank(additionalDetailsJson)) {
                    try {
                        additionalDetails = objectMapper.readTree(additionalDetailsJson);
                    } catch (Exception e) {
                        log.warn("[UPYOG-ROW-MAPPER] Error deserializing family additional_details for familyId: {}", familyId, e);
                    }
                }

                family = Family.builder()
                        .id(rs.getString("f_id"))
                        .familyId(familyId)
                        .tenantId(rs.getString("f_tenant_id"))
                        .headUserUuid(rs.getString("f_head_user_uuid"))
                        .category(rs.getString("f_category"))
                        .status(FamilyStatus.fromValue(rs.getString("f_status")))
                        .additionalDetails(additionalDetails)
                        .members(new ArrayList<>())
                        .documents(new ArrayList<>())
                        .auditDetails(familyAudit)
                        .build();

                familyMap.put(familyId, family);
                memberIdMap.put(familyId, new HashSet<>());
                docIdMap.put(familyId, new HashSet<>());
            }

            // Extract member
            String memberId = rs.getString("m_id");
            if (StringUtils.isNotBlank(memberId) && !memberIdMap.get(familyId).contains(memberId)) {
                AuditDetails memberAudit = AuditDetails.builder()
                        .createdBy(rs.getString("m_created_by"))
                        .createdTime(rs.getLong("m_created_time"))
                        .lastModifiedBy(rs.getString("m_last_modified_by"))
                        .lastModifiedTime(rs.getLong("m_last_modified_time"))
                        .build();

                Object memberAdditionalDetails = null;
                String memberAdditionalJson = rs.getString("m_additional_details");
                if (StringUtils.isNotBlank(memberAdditionalJson)) {
                    try {
                        memberAdditionalDetails = objectMapper.readTree(memberAdditionalJson);
                    } catch (Exception e) {
                        log.warn("[UPYOG-ROW-MAPPER] Error deserializing member additional_details for memberId: {}", memberId, e);
                    }
                }

                FamilyMember member = FamilyMember.builder()
                        .id(memberId)
                        .familyId(rs.getString("m_family_id"))
                        .tenantId(rs.getString("m_tenant_id"))
                        .userUuid(rs.getString("m_user_uuid"))
                        .relationshipWithHead(rs.getString("m_relationship"))
                        .isHead(rs.getBoolean("m_is_head"))
                        .isVerified(rs.getBoolean("m_is_verified"))
                        .active(rs.getBoolean("m_active"))
                        .status(MemberStatus.fromValue(rs.getString("m_status")))
                        .exitReason(ExitReason.fromValue(rs.getString("m_exit_reason")))
                        .exitDate(rs.getObject("m_exit_date") != null ? rs.getLong("m_exit_date") : null)
                        .consentToken(rs.getString("m_consent_token"))
                        .consentDate(rs.getObject("m_consent_date") != null ? rs.getLong("m_consent_date") : null)
                        .effectiveFrom(rs.getLong("m_effective_from"))
                        .effectiveTo(rs.getLong("m_effective_to"))
                        .additionalDetails(memberAdditionalDetails)
                        .auditDetails(memberAudit)
                        .build();

                family.getMembers().add(member);
                memberIdMap.get(familyId).add(memberId);
            }

            // Extract document
            String docId = rs.getString("d_id");
            if (StringUtils.isNotBlank(docId) && !docIdMap.get(familyId).contains(docId)) {
                AuditDetails docAudit = AuditDetails.builder()
                        .createdBy(rs.getString("d_created_by"))
                        .createdTime(rs.getLong("d_created_time"))
                        .lastModifiedBy(rs.getString("d_last_modified_by"))
                        .lastModifiedTime(rs.getLong("d_last_modified_time"))
                        .build();

                FamilyDocument doc = FamilyDocument.builder()
                        .id(docId)
                        .familyId(rs.getString("d_family_id"))
                        .tenantId(rs.getString("d_tenant_id"))
                        .documentType(rs.getString("d_document_type"))
                        .filestoreId(rs.getString("d_filestore_id"))
                        .active(rs.getBoolean("d_active"))
                        .auditDetails(docAudit)
                        .build();

                family.getDocuments().add(doc);
                docIdMap.get(familyId).add(docId);
            }
        }

        return new ArrayList<>(familyMap.values());
    }
}
