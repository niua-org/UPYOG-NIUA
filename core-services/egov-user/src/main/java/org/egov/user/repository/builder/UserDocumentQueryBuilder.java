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

    public static final String INSERT_QUERY =
            "INSERT INTO ug_user_document (" +
            "id, user_uuid, tenant_id, document_type, document_uid, filestore_id, file_name, " +
            "is_verified, active, additional_details, reference_type, reference_id, createdby, createddate, lastmodifiedby, lastmodifieddate" +
            ") VALUES (" +
            ":id, :user_uuid, :tenant_id, :document_type, :document_uid, :filestore_id, :file_name, " +
            ":is_verified, :active, cast(:additional_details as jsonb), :reference_type, :reference_id, :createdby, :createddate, :lastmodifiedby, :lastmodifieddate" +
            ")";

    public static final String UPDATE_QUERY =
            "UPDATE ug_user_document SET " +
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
        if (!CollectionUtils.isEmpty(criteria.getUserUuids())) {
            query.append(" AND d.user_uuid IN (:userUuids) ");
            params.put("userUuids", criteria.getUserUuids());
        } else if (StringUtils.isNotBlank(criteria.getUserUuid())) {
            query.append(" AND d.user_uuid = :userUuid ");
            params.put("userUuid", criteria.getUserUuid());
        } else if (StringUtils.isNotBlank(criteria.getUuid())) {
            query.append(" AND d.user_uuid = :userUuid ");
            params.put("userUuid", criteria.getUuid());
        } else if (!CollectionUtils.isEmpty(criteria.getUserIds())) {
            query.append(" AND d.user_uuid IN (SELECT uuid FROM eg_user WHERE id IN (:userIds)) ");
            params.put("userIds", criteria.getUserIds());
        } else if (criteria.getUserId() != null) {
            query.append(" AND d.user_uuid = (SELECT uuid FROM eg_user WHERE id = :userId) ");
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
