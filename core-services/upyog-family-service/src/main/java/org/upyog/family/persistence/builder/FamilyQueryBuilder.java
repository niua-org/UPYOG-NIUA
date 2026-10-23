package org.upyog.family.persistence.builder;

import org.apache.commons.lang3.StringUtils;
import org.upyog.family.config.FamilyConfiguration;
import org.upyog.family.web.contract.FamilySearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SQL Query Builder for Family and Member search operations against ug_ tables in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
public class FamilyQueryBuilder {

    private final FamilyConfiguration configuration;

    public static final String INSERT_FAMILY_SQL =
            "INSERT INTO ug_family (id, family_id, tenant_id, head_user_uuid, category, "
            + "status, additional_details, created_by, created_time, last_modified_by, last_modified_time) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?)";

    public static final String INSERT_MEMBER_SQL =
            "INSERT INTO ug_family_member (id, family_id, tenant_id, user_uuid, relationship_with_head, "
            + "is_head, is_verified, active, status, exit_reason, exit_date, consent_token, consent_date, "
            + "effective_from, effective_to, additional_details, "
            + "created_by, created_time, last_modified_by, last_modified_time) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?)";

    public static final String INSERT_DOC_SQL =
            "INSERT INTO ug_family_document (id, family_id, tenant_id, document_type, filestore_id, "
            + "active, created_by, created_time, last_modified_by, last_modified_time) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    public static final String UPDATE_FAMILY_SQL =
            "UPDATE ug_family SET head_user_uuid = ?, category = ?, "
            + "status = ?, additional_details = ?::jsonb, last_modified_by = ?, last_modified_time = ? "
            + "WHERE family_id = ?";

    public static final String UPDATE_MEMBER_SQL =
            "UPDATE ug_family_member SET user_uuid = ?, relationship_with_head = ?, is_head = ?, is_verified = ?, "
            + "active = ?, status = ?, exit_reason = ?, exit_date = ?, consent_token = ?, consent_date = ?, "
            + "effective_from = ?, effective_to = ?, additional_details = ?::jsonb, "
            + "last_modified_by = ?, last_modified_time = ? WHERE id = ?";

    public static final String UPDATE_DOC_SQL =
            "UPDATE ug_family_document SET active = ?, last_modified_by = ?, last_modified_time = ? "
            + "WHERE id = ?";

    public static final String INSERT_LINEAGE_SQL =
            "INSERT INTO ug_family_lineage (id, parent_family_id, child_family_id, action_type, reason, created_time) "
            + "VALUES (?, ?, ?, ?, ?, ?)";

    public static final String SELECT_LINEAGE_BY_FAMILY_ID_SQL =
            "SELECT id, parent_family_id, child_family_id, action_type, reason, created_time "
            + "FROM ug_family_lineage WHERE parent_family_id = ? OR child_family_id = ? ORDER BY created_time DESC";

    public static final String EXISTS_MEMBER_SQL =
            "SELECT count(1) FROM ug_family_member WHERE id = ?";

    public static final String EXISTS_DOC_SQL =
            "SELECT count(1) FROM ug_family_document WHERE id = ?";

    private static final String BASE_FAMILY_QUERY =
            "SELECT f.id as f_id, f.family_id as f_family_id, f.tenant_id as f_tenant_id, "
            + "f.head_user_uuid as f_head_user_uuid, f.category as f_category, "
            + "f.status as f_status, "
            + "f.additional_details as f_additional_details, f.created_by as f_created_by, "
            + "f.created_time as f_created_time, f.last_modified_by as f_last_modified_by, "
            + "f.last_modified_time as f_last_modified_time, "
            + "m.id as m_id, m.family_id as m_family_id, m.tenant_id as m_tenant_id, "
            + "m.user_uuid as m_user_uuid, m.relationship_with_head as m_relationship, "
            + "m.is_head as m_is_head, m.is_verified as m_is_verified, m.active as m_active, "
            + "m.status as m_status, m.exit_reason as m_exit_reason, m.exit_date as m_exit_date, "
            + "m.consent_token as m_consent_token, m.consent_date as m_consent_date, "
            + "m.effective_from as m_effective_from, m.effective_to as m_effective_to, "
            + "m.additional_details as m_additional_details, m.created_by as m_created_by, "
            + "m.created_time as m_created_time, m.last_modified_by as m_last_modified_by, "
            + "m.last_modified_time as m_last_modified_time, "
            + "d.id as d_id, d.family_id as d_family_id, d.tenant_id as d_tenant_id, "
            + "d.document_type as d_document_type, d.filestore_id as d_filestore_id, "
            + "d.active as d_active, d.created_by as d_created_by, d.created_time as d_created_time, "
            + "d.last_modified_by as d_last_modified_by, d.last_modified_time as d_last_modified_time "
            + "FROM ug_family f "
            + "LEFT JOIN ug_family_member m ON f.family_id = m.family_id "
            + "LEFT JOIN ug_family_document d ON f.family_id = d.family_id ";

    @Autowired
    public FamilyQueryBuilder(FamilyConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Builds dynamic parameterized SQL query for searching Family units based on criteria in UPYOG.
     * Appends query parameters to preparedStmtList to prevent SQL injection vulnerabilities.
     *
     * @param criteria query search filter object
     * @param preparedStmtList list accumulating query argument values
     * @return parameterized SQL query string
     */
    public String getFamilySearchQuery(FamilySearchCriteria criteria, List<Object> preparedStmtList) {
        StringBuilder query = new StringBuilder(BASE_FAMILY_QUERY);
        StringBuilder whereClause = new StringBuilder();

        if (StringUtils.isNotBlank(criteria.getTenantId())) {
            addClause(whereClause);
            whereClause.append("f.tenant_id = ? ");
            preparedStmtList.add(criteria.getTenantId());
        }

        if (StringUtils.isNotBlank(criteria.getFamilyId())) {
            addClause(whereClause);
            whereClause.append("f.family_id = ? ");
            preparedStmtList.add(criteria.getFamilyId());
        }

        if (criteria.getFamilyIds() != null && !criteria.getFamilyIds().isEmpty()) {
            addClause(whereClause);
            whereClause.append("f.family_id IN (");
            for (int i = 0; i < criteria.getFamilyIds().size(); i++) {
                whereClause.append("?");
                if (i < criteria.getFamilyIds().size() - 1) {
                    whereClause.append(", ");
                }
                preparedStmtList.add(criteria.getFamilyIds().get(i));
            }
            whereClause.append(") ");
        }

        if (StringUtils.isNotBlank(criteria.getHeadUserUuid())) {
            addClause(whereClause);
            whereClause.append("f.head_user_uuid = ? ");
            preparedStmtList.add(criteria.getHeadUserUuid());
        }

        if (StringUtils.isNotBlank(criteria.getCategory())) {
            addClause(whereClause);
            whereClause.append("f.category = ? ");
            preparedStmtList.add(criteria.getCategory());
        }

        if (criteria.getStatus() != null) {
            addClause(whereClause);
            whereClause.append("f.status = ? ");
            preparedStmtList.add(criteria.getStatus().name());
        }

        if (StringUtils.isNotBlank(criteria.getMemberUserUuid())) {
            addClause(whereClause);
            whereClause.append("f.family_id IN (SELECT family_id FROM ug_family_member WHERE user_uuid = ? AND active = true) ");
            preparedStmtList.add(criteria.getMemberUserUuid());
        }

        if (whereClause.length() > 0) {
            query.append("WHERE ").append(whereClause);
        }

        query.append("ORDER BY f.created_time DESC ");

        int limit = (criteria.getLimit() != null && criteria.getLimit() > 0)
                ? Math.min(criteria.getLimit(), configuration.getMaxLimit())
                : configuration.getDefaultLimit();

        int offset = (criteria.getOffset() != null && criteria.getOffset() >= 0)
                ? criteria.getOffset()
                : configuration.getDefaultOffset();

        query.append("LIMIT ? OFFSET ? ");
        preparedStmtList.add(limit);
        preparedStmtList.add(offset);

        return query.toString();
    }

    /**
     * Builds parameterized SQL query to retrieve active family IDs where a given user UUID is an active member in UPYOG.
     *
     * @param userUuid citizen user UUID
     * @param preparedStmtList list accumulating query argument values
     * @return parameterized SQL query string
     */
    public String getActiveFamilyByMemberUuidQuery(String userUuid, List<Object> preparedStmtList) {
        preparedStmtList.add(userUuid);
        return "SELECT f.family_id FROM ug_family f "
                + "JOIN ug_family_member m ON f.family_id = m.family_id "
                + "WHERE m.user_uuid = ? AND m.active = true AND f.status = 'ACTIVE'";
    }

    private void addClause(StringBuilder whereClause) {
        if (whereClause.length() > 0) {
            whereClause.append("AND ");
        }
    }
}
