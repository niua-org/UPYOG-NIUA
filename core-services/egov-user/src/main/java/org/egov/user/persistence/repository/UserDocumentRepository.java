package org.egov.user.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.repository.builder.UserDocumentQueryBuilder;
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

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ObjectMapper objectMapper;
    private final UserDocumentQueryBuilder queryBuilder;

    /**
     * Constructs a {@code UserDocumentRepository} with required JDBC templates and helpers.
     *
     * @param namedParameterJdbcTemplate named parameter JDBC operations template
     * @param objectMapper Jackson JSON mapper for JSONB column handling
     * @param queryBuilder dynamic SQL query builder for user document search criteria
     */
    @Autowired
    public UserDocumentRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate,
                                  ObjectMapper objectMapper,
                                  UserDocumentQueryBuilder queryBuilder) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.objectMapper = objectMapper;
        this.queryBuilder = queryBuilder;
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

        String userUuid = document.getUserUuid();

        Map<String, Object> params = new HashMap<>();
        params.put("id", document.getId());
        params.put("user_uuid", userUuid);
        params.put("tenant_id", document.getTenantId());
        params.put("document_type", document.getDocumentType());
        params.put("document_uid", document.getDocumentUid());
        params.put("filestore_id", document.getFilestoreId());
        params.put("file_name", document.getFileName());
        params.put("is_verified", document.getIsVerified());
        params.put("active", document.getActive());
        params.put("reference_type", document.getReferenceType());
        params.put("reference_id", document.getReferenceId());
        params.put("createdby", document.getCreatedBy());
        params.put("createddate", document.getCreatedDate());
        params.put("lastmodifiedby", document.getLastModifiedBy());
        params.put("lastmodifieddate", document.getLastModifiedDate());
        params.put("additional_details", serializeJson(document.getAdditionalDetails()));

        log.debug("[USER-DOC-REPO] Executing insert for document ID: {}, userUuid: {}", document.getId(), userUuid);
        namedParameterJdbcTemplate.update(queryBuilder.getInsertQuery(), params);
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
        params.put("reference_type", document.getReferenceType());
        params.put("reference_id", document.getReferenceId());
        params.put("lastmodifiedby", document.getLastModifiedBy());
        params.put("lastmodifieddate", document.getLastModifiedDate());
        params.put("additional_details", serializeJson(document.getAdditionalDetails()));

        log.debug("[USER-DOC-REPO] Executing update for document ID: {}", document.getId());
        namedParameterJdbcTemplate.update(queryBuilder.getUpdateQuery(), params);
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
        namedParameterJdbcTemplate.update(queryBuilder.getSoftDeleteQuery(), params);
    }

    /**
     * Searches document records based on {@link UserDocumentSearchCriteria}.
     *
     * @param criteria search filter parameters
     * @return list of matching {@link UserDocument} records
     */
    public List<UserDocument> search(UserDocumentSearchCriteria criteria) {
        Map<String, Object> params = new HashMap<>();
        String query = queryBuilder.getDocumentSearchQuery(criteria, params);
        return namedParameterJdbcTemplate.query(query, params, this::mapRow);
    }

    /**
     * Maps a single SQL {@link ResultSet} row from {@code ug_user_document} to a {@link UserDocument} model.
     *
     * @param rs     the active database result set
     * @param rowNum index of current row
     * @return constructed {@link UserDocument} entity
     * @throws SQLException if column extraction fails
     */
    private UserDocument mapRow(ResultSet rs, int rowNum) throws SQLException {
        String userUuid = null;
        try {
            userUuid = rs.getString("user_uuid");
        } catch (SQLException e) {
            log.trace("[USER-DOC-REPO] user_uuid column not present in result set: {}", e.getMessage());
        }

        Long userId = null;
        try {
            long uid = rs.getLong("user_id");
            if (!rs.wasNull()) {
                userId = uid;
            }
        } catch (SQLException e) {
            log.trace("[USER-DOC-REPO] user_id column not present in result set: {}", e.getMessage());
        }

        String referenceType = null;
        String referenceId = null;
        try {
            referenceType = rs.getString("reference_type");
            referenceId = rs.getString("reference_id");
        } catch (SQLException e) {
            log.trace("[USER-DOC-REPO] reference columns not present in result set: {}", e.getMessage());
        }

        return UserDocument.builder()
                .id(rs.getString("id"))
                .userId(userId)
                .userUuid(userUuid)
                .tenantId(rs.getString("tenant_id"))
                .documentType(rs.getString("document_type"))
                .documentUid(rs.getString("document_uid"))
                .filestoreId(rs.getString("filestore_id"))
                .fileName(rs.getString("file_name"))
                .isVerified(rs.getBoolean("is_verified"))
                .active(rs.getBoolean("active"))
                .referenceType(referenceType)
                .referenceId(referenceId)
                .createdBy(rs.getLong("createdby"))
                .createdDate(rs.getTimestamp("createddate"))
                .lastModifiedBy(rs.getLong("lastmodifiedby"))
                .lastModifiedDate(rs.getTimestamp("lastmodifieddate"))
                .additionalDetails(deserializeJson(rs.getString("additional_details")))
                .build();
    }

    /**
     * Serializes an arbitrary object to a JSON string representation for JSONB column storage.
     *
     * @param obj object to serialize
     * @return JSON string or null if object is null or serialization fails
     */
    private String serializeJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("[USER-DOC-REPO] Could not serialize additional details: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Deserializes a raw JSON string from a JSONB database column into an arbitrary object hierarchy.
     *
     * @param json raw JSON string
     * @return deserialized Object or null if string is blank or invalid
     */
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
