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
     * Persists a new {@link UserDocument} record in the database using idempotent upsert.
     * Enforces natural key deduplication and foreign document ownership validation.
     *
     * @param document the document entity to insert or update
     * @return the persisted {@link UserDocument}
     */
    public UserDocument create(UserDocument document) {
        // Natural key deduplication: if id is blank, lookup existing document for this citizen
        if (StringUtils.isBlank(document.getId())) {
            String existingId = findDocumentId(document.getUserUuid(), document.getReferenceType(), document.getReferenceId(), document.getFilestoreId());
            if (existingId == null && StringUtils.isNotBlank(document.getFilestoreId())) {
                existingId = findDocumentIdByFilestoreId(document.getUserUuid(), document.getFilestoreId());
            }
            if (existingId != null) {
                document.setId(existingId);
            } else {
                document.setId(UUID.randomUUID().toString());
            }
        } else {
            // Ownership boundary check: verify client-supplied id does not belong to another citizen
            String existingOwner = findUserUuidByDocumentId(document.getId());
            if (existingOwner != null && !existingOwner.equals(document.getUserUuid())) {
                log.warn("[USER-DOC-REPO] Blocked attempt to overwrite foreign document ID {} owned by {} with data for {}",
                        document.getId(), existingOwner, document.getUserUuid());
                document.setId(UUID.randomUUID().toString());
            }
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
        Long userId = document.getUserId();

        // Fix for PR Comment #8: Populate both user_id (bigint) and user_uuid (varchar) for rolling deployment safety
        if (userId == null && StringUtils.isNotBlank(userUuid)) {
            userId = resolveUserIdByUuid(userUuid);
            document.setUserId(userId);
        } else if (StringUtils.isBlank(userUuid) && userId != null) {
            userUuid = resolveUserUuidById(userId);
            document.setUserUuid(userUuid);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("id", document.getId());
        params.put("user_id", userId);
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

        String userUuid = document.getUserUuid();
        Long userId = document.getUserId();

        // Fix for PR Comment #8: Ensure both user_id and user_uuid are supplied during rolling deployment updates
        if (userId == null && StringUtils.isNotBlank(userUuid)) {
            userId = resolveUserIdByUuid(userUuid);
            document.setUserId(userId);
        } else if (StringUtils.isBlank(userUuid) && userId != null) {
            userUuid = resolveUserUuidById(userId);
            document.setUserUuid(userUuid);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("id", document.getId());
        params.put("user_id", userId);
        params.put("user_uuid", userUuid);
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

    /**
     * Looks up existing active document ID by reference and filestore ID for natural key deduplication.
     *
     * @param userUuid citizen user UUID
     * @param referenceType entity category (e.g. CITIZEN_PROFILE, PROPERTY, INCOME_SALARY, VEHICLE_RC)
     * @param referenceId associated entity record ID
     * @param filestoreId file storage system ID
     * @return document ID or null
     */
    public String findDocumentId(String userUuid, String referenceType, String referenceId, String filestoreId) {
        if (StringUtils.isBlank(userUuid) || StringUtils.isBlank(filestoreId) || StringUtils.isBlank(referenceType) || StringUtils.isBlank(referenceId)) {
            return null;
        }
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("user_uuid", userUuid);
            params.put("reference_type", referenceType);
            params.put("reference_id", referenceId);
            params.put("filestore_id", filestoreId);
            List<String> list = namedParameterJdbcTemplate.query(
                    UserDocumentQueryBuilder.SELECT_DOCUMENT_ID_BY_USER_AND_REF_SQL,
                    params,
                    (rs, rowNum) -> rs.getString("id")
            );
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        } catch (Exception e) {
            log.debug("[USER-DOC-REPO] Failed to lookup existing document by reference: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Looks up existing active document ID by citizen UUID and filestore ID.
     *
     * @param userUuid citizen user UUID
     * @param filestoreId file storage system ID
     * @return document ID or null
     */
    public String findDocumentIdByFilestoreId(String userUuid, String filestoreId) {
        if (StringUtils.isBlank(userUuid) || StringUtils.isBlank(filestoreId)) {
            return null;
        }
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("user_uuid", userUuid);
            params.put("filestore_id", filestoreId);
            List<String> list = namedParameterJdbcTemplate.query(
                    UserDocumentQueryBuilder.SELECT_DOCUMENT_ID_BY_USER_AND_FILESTORE_SQL,
                    params,
                    (rs, rowNum) -> rs.getString("id")
            );
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        } catch (Exception e) {
            log.debug("[USER-DOC-REPO] Failed to lookup existing document by filestoreId: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Looks up user_uuid of existing document record by primary key ID.
     *
     * @param documentId document primary key ID
     * @return userUuid owning the document or null
     */
    public String findUserUuidByDocumentId(String documentId) {
        if (StringUtils.isBlank(documentId)) {
            return null;
        }
        try {
            Map<String, Object> params = Collections.singletonMap("id", documentId);
            List<String> list = namedParameterJdbcTemplate.query(
                    UserDocumentQueryBuilder.SELECT_DOCUMENT_USER_UUID_BY_ID_SQL,
                    params,
                    (rs, rowNum) -> rs.getString("user_uuid")
            );
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        } catch (Exception e) {
            log.debug("[USER-DOC-REPO] Failed to lookup document owner userUuid: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Resolves the numeric eg_user ID for a given user UUID.
     * Fixes PR Comment #8 by facilitating dual-write on rolling deployments.
     *
     * @param userUuid the citizen or employee UUID
     * @return numeric user ID or null if not found
     */
    public Long resolveUserIdByUuid(String userUuid) {
        if (StringUtils.isBlank(userUuid)) return null;
        try {
            List<Long> ids = namedParameterJdbcTemplate.query(
                    "SELECT id FROM eg_user WHERE uuid = :uuid LIMIT 1",
                    Collections.singletonMap("uuid", userUuid),
                    (rs, rowNum) -> rs.getLong("id")
            );
            return CollectionUtils.isEmpty(ids) ? null : ids.get(0);
        } catch (Exception e) {
            log.trace("[USER-DOC-REPO] Could not resolve user_id for uuid {}: {}", userUuid, e.getMessage());
            return null;
        }
    }

    /**
     * Resolves the citizen or employee UUID for a given numeric eg_user ID.
     * Fixes PR Comment #8 by facilitating dual-write on rolling deployments.
     *
     * @param userId the numeric user ID
     * @return user UUID string or null if not found
     */
    public String resolveUserUuidById(Long userId) {
        if (userId == null) return null;
        try {
            List<String> uuids = namedParameterJdbcTemplate.query(
                    "SELECT uuid FROM eg_user WHERE id = :id LIMIT 1",
                    Collections.singletonMap("id", userId),
                    (rs, rowNum) -> rs.getString("uuid")
            );
            return CollectionUtils.isEmpty(uuids) ? null : uuids.get(0);
        } catch (Exception e) {
            log.trace("[USER-DOC-REPO] Could not resolve user_uuid for id {}: {}", userId, e.getMessage());
            return null;
        }
    }
}
