package org.egov.user.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.web.contract.document.UserDocumentSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Repository performing persistence and retrieval operations on {@code ug_user_document}.
 */
@Repository
@Slf4j
public class UserDocumentRepository {

    private static final String INSERT_QUERY =
            "INSERT INTO ug_user_document (" +
            "id, user_id, tenant_id, document_type, document_uid, filestore_id, file_name, " +
            "is_verified, active, additional_details, createdby, createddate, lastmodifiedby, lastmodifieddate" +
            ") VALUES (" +
            ":id, :user_id, :tenant_id, :document_type, :document_uid, :filestore_id, :file_name, " +
            ":is_verified, :active, cast(:additional_details as jsonb), :createdby, :createddate, :lastmodifiedby, :lastmodifieddate" +
            ")";

    private static final String UPDATE_QUERY =
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

    private static final String SOFT_DELETE_QUERY =
            "UPDATE ug_user_document SET " +
            "active = false, " +
            "lastmodifiedby = :lastmodifiedby, " +
            "lastmodifieddate = :lastmodifieddate " +
            "WHERE id = :id";

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public UserDocumentRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate, ObjectMapper objectMapper) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Persists a new {@link UserDocument} record in the database.
     * Generates a random UUID for the document if not already provided.
     *
     * @param document the document entity to insert
     * @return the persisted {@link UserDocument}
     */
    public UserDocument create(UserDocument document) {
        if (StringUtils.isBlank(document.getId())) {
            document.setId(UUID.randomUUID().toString());
        }
        Date now = new Date();
        document.setCreatedDate(now);
        document.setLastModifiedDate(now);
        if (document.getActive() == null) {
            document.setActive(true);
        }
        if (document.getIsVerified() == null) {
            document.setIsVerified(false);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("id", document.getId());
        params.put("user_id", document.getUserId());
        params.put("tenant_id", document.getTenantId());
        params.put("document_type", document.getDocumentType());
        params.put("document_uid", document.getDocumentUid());
        params.put("filestore_id", document.getFilestoreId());
        params.put("file_name", document.getFileName());
        params.put("is_verified", document.getIsVerified());
        params.put("active", document.getActive());
        params.put("createdby", document.getCreatedBy());
        params.put("createddate", document.getCreatedDate());
        params.put("lastmodifiedby", document.getLastModifiedBy());
        params.put("lastmodifieddate", document.getLastModifiedDate());
        params.put("additional_details", serializeJson(document.getAdditionalDetails()));

        log.debug("[USER-DOC-REPO] Executing insert for document ID: {}, userId: {}", document.getId(), document.getUserId());
        namedParameterJdbcTemplate.update(INSERT_QUERY, params);
        return document;
    }

    /**
     * Updates an existing document's metadata, verification status, or attachment details.
     *
     * @param document the {@link UserDocument} containing modified attributes
     */
    public void update(UserDocument document) {
        document.setLastModifiedDate(new Date());

        Map<String, Object> params = new HashMap<>();
        params.put("id", document.getId());
        params.put("document_type", document.getDocumentType());
        params.put("document_uid", document.getDocumentUid());
        params.put("filestore_id", document.getFilestoreId());
        params.put("file_name", document.getFileName());
        params.put("is_verified", document.getIsVerified() != null ? document.getIsVerified() : false);
        params.put("active", document.getActive() != null ? document.getActive() : true);
        params.put("lastmodifiedby", document.getLastModifiedBy());
        params.put("lastmodifieddate", document.getLastModifiedDate());
        params.put("additional_details", serializeJson(document.getAdditionalDetails()));

        log.debug("[USER-DOC-REPO] Executing update for document ID: {}", document.getId());
        namedParameterJdbcTemplate.update(UPDATE_QUERY, params);
    }

    /**
     * Soft-deletes a document by marking {@code active = false} and auditing the actor.
     *
     * @param id     the document identifier
     * @param userId the ID of the user performing the deletion
     */
    public void delete(String id, Long userId) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("lastmodifiedby", userId);
        params.put("lastmodifieddate", new Date());

        log.debug("[USER-DOC-REPO] Executing soft delete for document ID: {}", id);
        namedParameterJdbcTemplate.update(SOFT_DELETE_QUERY, params);
    }

    /**
     * Searches document records based on {@link UserDocumentSearchCriteria}.
     *
     * @param criteria search filter parameters
     * @return list of matching {@link UserDocument} records
     */
    public List<UserDocument> search(UserDocumentSearchCriteria criteria) {
        StringBuilder query = new StringBuilder("SELECT d.*, u.uuid as user_uuid FROM ug_user_document d LEFT JOIN eg_user u ON d.user_id = u.id AND d.tenant_id = u.tenantid WHERE 1=1 ");
        Map<String, Object> params = new HashMap<>();

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

        return namedParameterJdbcTemplate.query(query.toString(), params, this::mapRow);
    }

    private UserDocument mapRow(ResultSet rs, int rowNum) throws SQLException {
        String userUuid = null;
        try {
            userUuid = rs.getString("user_uuid");
        } catch (SQLException e) {
            log.trace("[USER-DOC-REPO] user_uuid column not present in result set: {}", e.getMessage());
        }

        return UserDocument.builder()
                .id(rs.getString("id"))
                .userId(rs.getLong("user_id"))
                .userUuid(userUuid)
                .tenantId(rs.getString("tenant_id"))
                .documentType(rs.getString("document_type"))
                .documentUid(rs.getString("document_uid"))
                .filestoreId(rs.getString("filestore_id"))
                .fileName(rs.getString("file_name"))
                .isVerified(rs.getBoolean("is_verified"))
                .active(rs.getBoolean("active"))
                .createdBy(rs.getLong("createdby"))
                .createdDate(rs.getTimestamp("createddate"))
                .lastModifiedBy(rs.getLong("lastmodifiedby"))
                .lastModifiedDate(rs.getTimestamp("lastmodifieddate"))
                .additionalDetails(deserializeJson(rs.getString("additional_details")))
                .build();
    }

    private String serializeJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("[USER-DOC-REPO] Could not serialize additional details: {}", e.getMessage());
            return null;
        }
    }

    private Object deserializeJson(String json) {
        if (StringUtils.isBlank(json)) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            log.warn("[USER-DOC-REPO] Could not deserialize additional details: {}", e.getMessage());
            return null;
        }
    }
}
