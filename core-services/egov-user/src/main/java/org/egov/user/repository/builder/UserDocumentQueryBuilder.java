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
            "id, user_id, tenant_id, document_type, document_uid, filestore_id, file_name, " +
            "is_verified, active, additional_details, createdby, createddate, lastmodifiedby, lastmodifieddate" +
            ") VALUES (" +
            ":id, :user_id, :tenant_id, :document_type, :document_uid, :filestore_id, :file_name, " +
            ":is_verified, :active, cast(:additional_details as jsonb), :createdby, :createddate, :lastmodifiedby, :lastmodifieddate" +
            ")";

    public static final String UPDATE_QUERY =
            "UPDATE ug_user_document SET " +
            "document_type = :document_type, " +
            "document_uid = :document_uid, " +
            "filestore_id = :filestore_id, " +
            "file_name = :file_name, " +
            "is_verified = :is_verified, " +
            "active = :active, " +
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
            "SELECT d.*, u.uuid as user_uuid FROM ug_user_document d " +
            "LEFT JOIN eg_user u ON d.user_id = u.id AND d.tenant_id = u.tenantid WHERE 1=1 ";

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
        if (criteria.getUserId() != null) {
            query.append(" AND d.user_id = :userId ");
            params.put("userId", criteria.getUserId());
        }
        if (StringUtils.isNotBlank(criteria.getUserUuid())) {
            query.append(" AND d.user_id = (SELECT id FROM eg_user WHERE uuid = :userUuid) ");
            params.put("userUuid", criteria.getUserUuid());
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

        query.append(" ORDER BY d.createddate DESC ");
        return query.toString();
    }
}
