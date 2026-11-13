package org.egov.user.repository.builder;

import org.apache.commons.lang3.StringUtils;
import org.egov.user.web.contract.document.UserDocumentSearchCriteria;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Map;

/**
 * SQL Query Builder component for generating queries targeting {@code ug_user_document}.
 * Follows the standard UPYOG QueryBuilder architectural pattern.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
public class UserDocumentQueryBuilder {

    // Fix for PR Comment #8: Dual-write user_id (bigint) and user_uuid (varchar) during rolling deployment.
    // Preserves backwards compatibility for old pods, views, and external services filtering on user_id.
    public static final String INSERT_QUERY =
            "INSERT INTO ug_user_document (" +
            "id, user_id, user_uuid, tenant_id, document_type, document_uid, filestore_id, file_name, " +
            "is_verified, active, additional_details, reference_type, reference_id, createdby, createddate, lastmodifiedby, lastmodifieddate" +
            ") VALUES (" +
            ":id, :user_id, :user_uuid, :tenant_id, :document_type, :document_uid, :filestore_id, :file_name, " +
            ":is_verified, :active, cast(:additional_details as jsonb), :reference_type, :reference_id, :createdby, :createddate, :lastmodifiedby, :lastmodifieddate" +
            ") ON CONFLICT (id) DO UPDATE SET " +
            "user_id = COALESCE(ug_user_document.user_id, EXCLUDED.user_id), " +
            "user_uuid = COALESCE(ug_user_document.user_uuid, EXCLUDED.user_uuid), " +
            "document_type = EXCLUDED.document_type, " +
            "document_uid = EXCLUDED.document_uid, " +
            "filestore_id = EXCLUDED.filestore_id, " +
            "file_name = EXCLUDED.file_name, " +
            "is_verified = EXCLUDED.is_verified, " +
            "active = EXCLUDED.active, " +
            "reference_type = EXCLUDED.reference_type, " +
            "reference_id = EXCLUDED.reference_id, " +
            "additional_details = EXCLUDED.additional_details, " +
            "lastmodifiedby = EXCLUDED.lastmodifiedby, " +
            "lastmodifieddate = EXCLUDED.lastmodifieddate " +
            // Security: scope conflict update to owner user_uuid (or NULL for backfills) to prevent cross-user document overwrites
            "WHERE (ug_user_document.user_uuid = EXCLUDED.user_uuid OR ug_user_document.user_uuid IS NULL)";

    // Fix for PR Comment #8: Support both user_uuid and user_id in natural key lookups for rolling deployment safety
    public static final String SELECT_DOCUMENT_ID_BY_USER_AND_FILESTORE_SQL =
            "SELECT id FROM ug_user_document WHERE (user_uuid = :user_uuid OR user_id = (SELECT id FROM eg_user WHERE uuid = :user_uuid)) AND filestore_id = :filestore_id AND active = true LIMIT 1";

    public static final String SELECT_DOCUMENT_ID_BY_USER_AND_REF_SQL =
            "SELECT id FROM ug_user_document WHERE (user_uuid = :user_uuid OR user_id = (SELECT id FROM eg_user WHERE uuid = :user_uuid)) AND reference_type = :reference_type AND reference_id = :reference_id AND filestore_id = :filestore_id AND active = true LIMIT 1";

    public static final String SELECT_DOCUMENT_USER_UUID_BY_ID_SQL =
            "SELECT COALESCE(d.user_uuid, u.uuid) AS user_uuid FROM ug_user_document d LEFT JOIN eg_user u ON d.user_id = u.id WHERE d.id = :id";

    public static final String UPDATE_QUERY =
            "UPDATE ug_user_document SET " +
            "user_id = COALESCE(ug_user_document.user_id, :user_id), " +
            "user_uuid = COALESCE(ug_user_document.user_uuid, :user_uuid), " +
            "document_type = :document_type, " +
            "document_uid = :document_uid, " +
            "filestore_id = :filestore_id, " +
            "file_name = :file_name, " +
            "is_verified = :is_verified, " +
            "active = :active, " +
            "reference_type = :reference_type, " +
            "reference_id = :reference_id, " +
            "additional_details = cast(:additional_details as jsonb), " +
            "lastmodifiedby = :lastmodifiedby, " +
            "lastmodifieddate = :lastmodifieddate " +
            "WHERE id = :id";

    public static final String SOFT_DELETE_QUERY =
            "UPDATE ug_user_document SET " +
            "active = false, " +
            "lastmodifiedby = :lastmodifiedby, " +
            "lastmodifieddate = :lastmodifieddate " +
            "WHERE id = :id";

    private static final String BASE_SEARCH_QUERY =
            "SELECT d.* FROM ug_user_document d WHERE 1=1 ";

    /**
     * Default constructor for Spring component lifecycle.
     */
    public UserDocumentQueryBuilder() {
        // Default constructor
    }

    /**
     * Returns the parameterized SQL insert query for citizen verification documents.
     *
     * @return SQL insert statement string
     */
    public String getInsertQuery() {
        return INSERT_QUERY;
    }

    /**
     * Returns the parameterized SQL update query for citizen verification documents.
     *
     * @return SQL update statement string
     */
    public String getUpdateQuery() {
        return UPDATE_QUERY;
    }

    /**
     * Returns the parameterized SQL soft-delete query for citizen verification documents.
     *
     * @return SQL soft delete statement string
     */
    public String getSoftDeleteQuery() {
        return SOFT_DELETE_QUERY;
    }

    /**
     * Constructs a parameterized dynamic SQL select query to search user documents matching {@link UserDocumentSearchCriteria}.
     * Populates the params map with named parameter values.
     *
     * @param criteria search filter parameters
     * @param params   named parameter map
     * @return fully constructed SQL query string
     */
    public String getDocumentSearchQuery(UserDocumentSearchCriteria criteria, Map<String, Object> params) {
        StringBuilder query = new StringBuilder(BASE_SEARCH_QUERY);

        if (StringUtils.isNotBlank(criteria.getId())) {
            query.append(" AND d.id = :id ");
            params.put("id", criteria.getId());
        }
        if (!CollectionUtils.isEmpty(criteria.getIds())) {
            query.append(" AND d.id IN (:ids) ");
            params.put("ids", criteria.getIds());
        }
        // Fix for PR Comment #8: Support dual-read during rolling deployment.
        // Matches documents where user_uuid matches OR user_id matches eg_user sequence ID,
        // ensuring rows written by older pods with user_uuid = NULL are seamlessly returned.
        if (!CollectionUtils.isEmpty(criteria.getUserUuids())) {
            query.append(" AND (d.user_uuid IN (:userUuids) OR d.user_id IN (SELECT id FROM eg_user WHERE uuid IN (:userUuids))) ");
            params.put("userUuids", criteria.getUserUuids());
        } else if (StringUtils.isNotBlank(criteria.getUserUuid())) {
            query.append(" AND (d.user_uuid = :userUuid OR d.user_id = (SELECT id FROM eg_user WHERE uuid = :userUuid)) ");
            params.put("userUuid", criteria.getUserUuid());
        } else if (StringUtils.isNotBlank(criteria.getUuid())) {
            query.append(" AND (d.user_uuid = :userUuid OR d.user_id = (SELECT id FROM eg_user WHERE uuid = :userUuid)) ");
            params.put("userUuid", criteria.getUuid());
        } else if (!CollectionUtils.isEmpty(criteria.getUserIds())) {
            query.append(" AND (d.user_id IN (:userIds) OR d.user_uuid IN (SELECT uuid FROM eg_user WHERE id IN (:userIds))) ");
            params.put("userIds", criteria.getUserIds());
        } else if (criteria.getUserId() != null) {
            query.append(" AND (d.user_id = :userId OR d.user_uuid = (SELECT uuid FROM eg_user WHERE id = :userId)) ");
            params.put("userId", criteria.getUserId());
        }
        if (StringUtils.isNotBlank(criteria.getTenantId())) {
            if (criteria.getTenantId().contains(".")) {
                query.append(" AND (d.tenant_id = :tenantId OR d.tenant_id = :stateTenantId) ");
                params.put("tenantId", criteria.getTenantId());
                params.put("stateTenantId", criteria.getTenantId().split("\\.")[0]);
            } else {
                query.append(" AND d.tenant_id = :tenantId ");
                params.put("tenantId", criteria.getTenantId());
            }
        }
        if (StringUtils.isNotBlank(criteria.getDocumentType())) {
            query.append(" AND d.document_type = :docType ");
            params.put("docType", criteria.getDocumentType());
        }
        if (StringUtils.isNotBlank(criteria.getFilestoreId())) {
            query.append(" AND d.filestore_id = :filestoreId ");
            params.put("filestoreId", criteria.getFilestoreId());
        }
        if (criteria.getIsVerified() != null) {
            query.append(" AND d.is_verified = :isVerified ");
            params.put("isVerified", criteria.getIsVerified());
        }
        if (criteria.getActive() != null) {
            query.append(" AND d.active = :active ");
            params.put("active", criteria.getActive());
        }
        if (StringUtils.isNotBlank(criteria.getReferenceType())) {
            query.append(" AND d.reference_type = :referenceType ");
            params.put("referenceType", criteria.getReferenceType());
        }
        if (StringUtils.isNotBlank(criteria.getReferenceId())) {
            query.append(" AND d.reference_id = :referenceId ");
            params.put("referenceId", criteria.getReferenceId());
        }

        query.append(" ORDER BY d.createddate DESC ");
        return query.toString();
    }
}
