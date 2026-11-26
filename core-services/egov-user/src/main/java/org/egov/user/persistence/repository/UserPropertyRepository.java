package org.egov.user.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.model.asset.PropertyOwner;
import org.egov.user.domain.model.asset.RuralPropertyDetail;
import org.egov.user.domain.model.asset.UrbanPropertyDetail;
import org.egov.user.domain.model.asset.UserProperty;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.repository.builder.UserPropertyQueryBuilder;
import org.egov.user.web.contract.asset.RuralPropertySearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository performing transactional persistence for citizen property declarations across
 * 3NF tables: {@code ug_user_property}, {@code ug_user_urban_property},
 * {@code ug_user_rural_property}, {@code ug_user_property_owner}, and their audit tables.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Repository
@Slf4j
public class UserPropertyRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ObjectMapper objectMapper;
    private final UserPropertyQueryBuilder queryBuilder;

    /**
     * Constructs a {@code UserPropertyRepository} with required JDBC operations template, JSON mapper, and query builder.
     *
     * @param namedParameterJdbcTemplate named parameter JDBC operations template
     * @param objectMapper Jackson JSON mapper for serialization
     * @param queryBuilder SQL query builder for property persistence
     */
    @Autowired
    public UserPropertyRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate,
                                  ObjectMapper objectMapper,
                                  UserPropertyQueryBuilder queryBuilder) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.objectMapper = objectMapper;
        this.queryBuilder = queryBuilder;
    }

    /**
     * Persists base property record into {@code ug_user_property} and writes an audit row.
     * Enforces natural key deduplication and foreign property ownership validation.
     *
     * @param property property entity to persist
     */
    public void saveProperty(UserProperty property) {
        // Natural key deduplication: if id is blank, attempt to reuse existing property id for the citizen
        if (StringUtils.isBlank(property.getId())) {
            if (property.getUrbanPropertyDetail() != null && StringUtils.isNotBlank(property.getUrbanPropertyDetail().getUpyogPropertyId())) {
                String existingId = findUrbanPropertyId(property.getUserUuid(), property.getUrbanPropertyDetail().getUpyogPropertyId());
                if (existingId != null) {
                    property.setId(existingId);
                }
            } else if (property.getRuralPropertyDetail() != null && StringUtils.isNotBlank(property.getRuralPropertyDetail().getSurveyNumber())) {
                String existingId = findRuralPropertyId(property.getUserUuid(), property.getRuralPropertyDetail().getSurveyNumber());
                if (existingId != null) {
                    property.setId(existingId);
                }
            }
            if (StringUtils.isBlank(property.getId())) {
                property.setId(UUID.randomUUID().toString());
            }
        } else {
            // Ownership boundary check: verify client-supplied id does not belong to another citizen
            String existingOwner = findUserUuidByPropertyId(property.getId());
            if (existingOwner != null && !existingOwner.equals(property.getUserUuid())) {
                log.warn("[USER-PROP-REPO] Blocked attempt to overwrite foreign property ID {} owned by {} with data for {}",
                        property.getId(), existingOwner, property.getUserUuid());
                property.setId(UUID.randomUUID().toString());
            }
        }
        long now = System.currentTimeMillis();
        if (property.getCreatedTime() == null) {
            property.setCreatedTime(now);
        }
        property.setLastModifiedTime(now);
        if (StringUtils.isBlank(property.getStatus())) {
            property.setStatus(UserConstants.STATUS_ACTIVE_UPPERCASE);
        }
        if (property.getDeclarationConfirmed() == null) {
            property.setDeclarationConfirmed(false);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("id", property.getId());
        params.put("user_uuid", property.getUserUuid());
        params.put("tenant_id", property.getTenantId());
        params.put("locality_type", property.getLocalityType());
        params.put("ownership_type", property.getOwnershipType());
        params.put("declaration_confirmed", property.getDeclarationConfirmed());
        params.put("document_id", property.getDocumentId());
        params.put("status", property.getStatus());
        params.put("additional_details", serializeJson(property.getAdditionalDetails()));
        params.put("created_by", property.getCreatedBy());
        params.put("created_time", property.getCreatedTime());
        params.put("last_modified_by", property.getLastModifiedBy());
        params.put("last_modified_time", property.getLastModifiedTime());

        namedParameterJdbcTemplate.update(queryBuilder.getInsertPropertyQuery(), params);

        // Audit entry
        params.put("audit_id", UUID.randomUUID().toString());
        namedParameterJdbcTemplate.update(queryBuilder.getInsertPropertyAuditQuery(), params);
        log.info("[USER-PROP-REPO] Persisted property id: {} for user: {}", property.getId(), property.getUserUuid());
    }

    /**
     * Persists urban property specialization in {@code ug_user_urban_property}.
     *
     * @param detail urban property attributes
     */
    public void saveUrbanProperty(UrbanPropertyDetail detail) {
        long now = System.currentTimeMillis();
        if (detail.getCreatedTime() == null) {
            detail.setCreatedTime(now);
        }
        detail.setLastModifiedTime(now);

        Map<String, Object> params = new HashMap<>();
        params.put("property_record_id", detail.resolvePropertyRecordId());
        params.put("tenant_id", detail.getTenantId());
        params.put("upyog_property_id", detail.getUpyogPropertyId());
        params.put("property_tax_amount", detail.getPropertyTaxAmount());
        params.put("urban_address", detail.getUrbanAddress());
        params.put("created_by", detail.getCreatedBy());
        params.put("created_time", detail.getCreatedTime());
        params.put("last_modified_by", detail.getLastModifiedBy());
        params.put("last_modified_time", detail.getLastModifiedTime());
        params.put("user_uuid", detail.getUserUuid());

        namedParameterJdbcTemplate.update(queryBuilder.getInsertUrbanPropertyQuery(), params);
        log.info("[USER-PROP-REPO] Persisted urban property record id: {}", detail.resolvePropertyRecordId());
    }

    /**
     * Persists rural property specialization in {@code ug_user_rural_property}.
     *
     * @param detail rural property attributes
     */
    public void saveRuralProperty(RuralPropertyDetail detail) {
        long now = System.currentTimeMillis();
        if (detail.getCreatedTime() == null) {
            detail.setCreatedTime(now);
        }
        detail.setLastModifiedTime(now);

        Map<String, Object> params = new HashMap<>();
        params.put("property_record_id", detail.resolvePropertyRecordId());
        params.put("tenant_id", detail.getTenantId());
        params.put("property_id", detail.getPropertyId());
        params.put("gram_panchayat", detail.getGramPanchayat());
        params.put("village_name", detail.getVillageName());
        params.put("ward_number", detail.getWardNumber());
        params.put("property_address", detail.getPropertyAddress());
        params.put("property_type", detail.getPropertyType());
        params.put("property_usage", detail.getPropertyUsage());
        params.put("survey_number", detail.getSurveyNumber());
        params.put("created_by", detail.getCreatedBy());
        params.put("created_time", detail.getCreatedTime());
        params.put("last_modified_by", detail.getLastModifiedBy());
        params.put("last_modified_time", detail.getLastModifiedTime());
        params.put("user_uuid", detail.getUserUuid());

        namedParameterJdbcTemplate.update(queryBuilder.getInsertRuralPropertyQuery(), params);
        log.info("[USER-PROP-REPO] Persisted rural property record id: {}", detail.resolvePropertyRecordId());
    }

    /**
     * Persists co-owner record in {@code ug_user_property_owner} and writes an audit row.
     *
     * @param owner owner specification (strictly 3NF)
     */
    public void savePropertyOwner(PropertyOwner owner) {
        if (StringUtils.isBlank(owner.getId())) {
            if (StringUtils.isNotBlank(owner.getPropertyId()) && StringUtils.isNotBlank(owner.getUserUuid())) {
                try {
                    Map<String, Object> lookupParams = new HashMap<>();
                    lookupParams.put("property_id", owner.getPropertyId());
                    lookupParams.put("user_uuid", owner.getUserUuid());
                    List<String> existingIds = namedParameterJdbcTemplate.query(
                            UserPropertyQueryBuilder.SELECT_PROPERTY_OWNER_ID_BY_PROPERTY_AND_USER_SQL,
                            lookupParams,
                            (rs, rowNum) -> rs.getString("id")
                    );
                    if (existingIds != null && !existingIds.isEmpty()) {
                        owner.setId(existingIds.get(0));
                    }
                } catch (Exception e) {
                    log.debug("[USER-PROP-REPO] Unable to lookup existing property owner ID: {}", e.getMessage());
                }
            }
            if (StringUtils.isBlank(owner.getId())) {
                owner.setId(UUID.randomUUID().toString());
            }
        }
        long now = System.currentTimeMillis();
        if (owner.getCreatedTime() == null) {
            owner.setCreatedTime(now);
        }
        owner.setLastModifiedTime(now);
        if (owner.getActive() == null) {
            owner.setActive(true);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("id", owner.getId());
        params.put("property_id", owner.getPropertyId());
        params.put("tenant_id", owner.getTenantId());
        params.put("owner_type", owner.getOwnerType());
        params.put("user_uuid", owner.getUserUuid());
        params.put("active", owner.getActive());
        params.put("additional_details", serializeJson(owner.getAdditionalDetails()));
        params.put("created_by", owner.getCreatedBy());
        params.put("created_time", owner.getCreatedTime());
        params.put("last_modified_by", owner.getLastModifiedBy());
        params.put("last_modified_time", owner.getLastModifiedTime());

        namedParameterJdbcTemplate.update(queryBuilder.getInsertPropertyOwnerQuery(), params);

        // Audit entry
        params.put("audit_id", UUID.randomUUID().toString());
        namedParameterJdbcTemplate.update(queryBuilder.getInsertPropertyOwnerAuditQuery(), params);
        log.info("[USER-PROP-REPO] Persisted property owner id: {} for property: {}", owner.getId(), owner.getPropertyId());
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
            log.warn("[USER-PROP-REPO] Failed to serialize additionalDetails: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Looks up existing property owner user_uuid to enforce ownership boundary.
     *
     * @param propertyId property primary key ID
     * @return owner userUuid if record exists, or null
     */
    public String findUserUuidByPropertyId(String propertyId) {
        if (StringUtils.isBlank(propertyId)) {
            return null;
        }
        try {
            Map<String, Object> params = Collections.singletonMap("id", propertyId);
            List<String> list = namedParameterJdbcTemplate.query(
                    UserPropertyQueryBuilder.SELECT_PROPERTY_USER_UUID_BY_ID_SQL,
                    params,
                    (rs, rowNum) -> rs.getString("user_uuid")
            );
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        } catch (Exception e) {
            log.debug("[USER-PROP-REPO] Failed to lookup property owner UUID: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Finds existing active property ID for a citizen by UPYOG property ID (natural key deduplication).
     *
     * @param userUuid citizen user UUID
     * @param upyogPropertyId municipal property assessment ID
     * @return existing property ID or null
     */
    public String findUrbanPropertyId(String userUuid, String upyogPropertyId) {
        if (StringUtils.isBlank(userUuid) || StringUtils.isBlank(upyogPropertyId)) {
            return null;
        }
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("user_uuid", userUuid);
            params.put("upyog_property_id", upyogPropertyId.trim());
            List<String> list = namedParameterJdbcTemplate.query(
                    UserPropertyQueryBuilder.SELECT_URBAN_PROPERTY_RECORD_ID_SQL,
                    params,
                    (rs, rowNum) -> rs.getString("id")
            );
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        } catch (Exception e) {
            log.debug("[USER-PROP-REPO] Failed to lookup existing urban property by upyogPropertyId: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Finds all active citizen profiles linked to an urban property by upyogPropertyId and tenantId.
     *
     * @param tenantId tenant identifier
     * @param upyogPropertyId municipal property assessment ID
     * @return list of maps containing userUuid, mobileNumber, propertyRecordId, and ownershipType
     */
    public List<Map<String, Object>> findActiveLinkedUsersForUrbanProperty(String tenantId, String upyogPropertyId) {
        // Fast-fail if query parameters are missing or blank
        if (StringUtils.isBlank(tenantId) || StringUtils.isBlank(upyogPropertyId)) {
            return Collections.emptyList();
        }
        try {
            // Bind query parameters to avoid SQL injection
            Map<String, Object> params = new HashMap<>();
            params.put(UserConstants.SQL_PARAM_TENANT_ID, tenantId.trim());
            params.put(UserConstants.SQL_PARAM_UPYOG_PROPERTY_ID, upyogPropertyId.trim());

            // Query active linkages joining ug_user_property, ug_user_urban_property, and eg_user
            return namedParameterJdbcTemplate.query(
                    UserPropertyQueryBuilder.SELECT_LINKED_USERS_BY_UPYOG_PROPERTY_ID_SQL,
                    params,
                    (rs, rowNum) -> {
                        Map<String, Object> row = new HashMap<>();
                        row.put(UserConstants.JSON_KEY_USER_UUID, rs.getString(UserConstants.COL_USER_UUID));
                        row.put(UserConstants.PT_JSON_MOBILE_NUMBER, rs.getString(UserConstants.COL_MOBILENUMBER));
                        row.put(UserConstants.JSON_KEY_PROPERTY_RECORD_ID, rs.getString(UserConstants.COL_PROPERTY_RECORD_ID));
                        row.put(UserConstants.JSON_KEY_OWNERSHIP_TYPE, rs.getString(UserConstants.COL_OWNERSHIP_TYPE));
                        return row;
                    }
            );
        } catch (Exception e) {
            log.error("[USER-PROP-REPO] Error querying active linked users for urban property {}: {}", upyogPropertyId, e.getMessage());
            throw new CustomException(UserConstants.ERROR_PROPERTY_SERVICE_ERROR,
                    UserConstants.MSG_PROPERTY_LINKAGE_QUERY_FAILED_PREFIX + upyogPropertyId);
        }
    }

    /**
     * Finds existing active rural property ID for a citizen by survey/khasra number (natural key deduplication).
     *
     * @param userUuid citizen user UUID
     * @param surveyNumber khasra or survey number
     * @return existing property ID or null
     */
    public String findRuralPropertyId(String userUuid, String surveyNumber) {
        if (StringUtils.isBlank(userUuid) || StringUtils.isBlank(surveyNumber)) {
            return null;
        }
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("user_uuid", userUuid);
            params.put("survey_number", surveyNumber.trim());
            List<String> list = namedParameterJdbcTemplate.query(
                    UserPropertyQueryBuilder.SELECT_RURAL_PROPERTY_RECORD_ID_SQL,
                    params,
                    (rs, rowNum) -> rs.getString("id")
            );
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        } catch (Exception e) {
            log.debug("[USER-PROP-REPO] Failed to lookup existing rural property by surveyNumber: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Looks up existing active rural property record ID by citizen user UUID and rural property ID.
     *
     * @param userUuid citizen user UUID
     * @param propertyId generated or assigned rural property ID
     * @return existing property record ID or null
     */
    public String findRuralPropertyIdByPropertyId(String userUuid, String propertyId) {
        if (StringUtils.isBlank(userUuid) || StringUtils.isBlank(propertyId)) {
            return null;
        }
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("user_uuid", userUuid);
            params.put("property_id", propertyId.trim());
            List<String> list = namedParameterJdbcTemplate.query(
                    UserPropertyQueryBuilder.SELECT_RURAL_PROPERTY_RECORD_ID_BY_PROPERTY_ID_SQL,
                    params,
                    (rs, rowNum) -> rs.getString("id")
            );
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        } catch (Exception e) {
            log.debug("[USER-PROP-REPO] Failed to lookup existing rural property by propertyId: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Searches rural property assets matching the supplied criteria, hydrating child rural specializations,
     * attached documents, and co-owners.
     *
     * @param criteria query search criteria
     * @return list of matching {@link UserProperty} domain instances
     */
    public List<UserProperty> searchRuralProperties(RuralPropertySearchCriteria criteria) {
        Map<String, Object> params = new HashMap<>();
        String sql = queryBuilder.buildRuralPropertySearchQuery(criteria, params);
        log.debug("[USER-PROP-REPO] Executing rural property search SQL: {} with params: {}", sql, params);

        List<UserProperty> properties = namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> {
            RuralPropertyDetail rural = RuralPropertyDetail.builder()
                    .propertyRecordId(rs.getString("r_property_record_id"))
                    .id(rs.getString("r_property_record_id"))
                    .tenantId(rs.getString("r_tenant_id"))
                    .gramPanchayat(rs.getString("r_gram_panchayat"))
                    .villageName(rs.getString("r_village_name"))
                    .wardNumber(rs.getString("r_ward_number"))
                    .propertyAddress(rs.getString("r_property_address"))
                    .propertyType(rs.getString("r_property_type"))
                    .propertyUsage(rs.getString("r_property_usage"))
                    .surveyNumber(rs.getString("r_survey_number"))
                    .createdBy(rs.getString("r_created_by"))
                    .createdTime(rs.getLong("r_created_time"))
                    .lastModifiedBy(rs.getString("r_last_modified_by"))
                    .lastModifiedTime(rs.getLong("r_last_modified_time"))
                    .build();

            UserDocument doc = null;
            String docId = rs.getString("d_id");
            if (StringUtils.isNotBlank(docId)) {
                doc = UserDocument.builder()
                        .id(docId)
                        .documentType(rs.getString("d_document_type"))
                        .filestoreId(rs.getString("d_filestore_id"))
                        .documentUid(rs.getString("d_document_uid"))
                        .fileName(rs.getString("d_file_name"))
                        .isVerified(rs.getObject("d_is_verified") != null ? rs.getBoolean("d_is_verified") : null)
                        .build();
            }

            Object additionalDetails = null;
            String jsonStr = rs.getString("p_additional_details");
            if (StringUtils.isNotBlank(jsonStr)) {
                try {
                    additionalDetails = objectMapper.readValue(jsonStr, Object.class);
                } catch (Exception e) {
                    log.debug("[USER-PROP-REPO] Unable to deserialize additional_details: {}", e.getMessage());
                }
            }

            return UserProperty.builder()
                    .id(rs.getString("p_id"))
                    .userUuid(rs.getString("p_user_uuid"))
                    .tenantId(rs.getString("p_tenant_id"))
                    .localityType(rs.getString("p_locality_type"))
                    .ownershipType(rs.getString("p_ownership_type"))
                    .declarationConfirmed(rs.getBoolean("p_declaration_confirmed"))
                    .documentId(rs.getString("p_document_id"))
                    .document(doc)
                    .status(rs.getString("p_status"))
                    .additionalDetails(additionalDetails)
                    .ruralPropertyDetail(rural)
                    .owners(new ArrayList<>())
                    .createdBy(rs.getString("p_created_by"))
                    .createdTime(rs.getLong("p_created_time"))
                    .lastModifiedBy(rs.getString("p_last_modified_by"))
                    .lastModifiedTime(rs.getLong("p_last_modified_time"))
                    .build();
        });

        if (properties.isEmpty()) {
            return Collections.emptyList();
        }

        // Hydrate co-owners for all retrieved properties
        List<String> propertyIds = properties.stream().map(UserProperty::getId).collect(Collectors.toList());
        Map<String, List<PropertyOwner>> ownersByProperty = fetchPropertyOwners(propertyIds);

        for (UserProperty prop : properties) {
            if (ownersByProperty.containsKey(prop.getId())) {
                prop.setOwners(ownersByProperty.get(prop.getId()));
            }
        }

        return properties;
    }

    /**
     * Retrieves co-owners for a collection of property primary key IDs.
     *
     * @param propertyIds list of property primary key IDs
     * @return map grouping property ID to its active co-owners
     */
    private Map<String, List<PropertyOwner>> fetchPropertyOwners(List<String> propertyIds) {
        if (propertyIds == null || propertyIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> params = Collections.singletonMap("property_ids", propertyIds);
            List<PropertyOwner> owners = namedParameterJdbcTemplate.query(
                    queryBuilder.getSelectPropertyOwnersByPropertyIdsQuery(),
                    params,
                    (rs, rowNum) -> {
                        Object ownerAdditional = null;
                        String addJson = rs.getString("additional_details");
                        if (StringUtils.isNotBlank(addJson)) {
                            try {
                                ownerAdditional = objectMapper.readValue(addJson, Object.class);
                            } catch (Exception e) {
                                log.debug("[USER-PROP-REPO] Unable to deserialize property owner additional_details: {}", e.getMessage());
                            }
                        }
                        return PropertyOwner.builder()
                                .id(rs.getString("id"))
                                .propertyId(rs.getString("property_id"))
                                .tenantId(rs.getString("tenant_id"))
                                .ownerType(rs.getString("owner_type"))
                                .userUuid(rs.getString("user_uuid"))
                                .active(rs.getBoolean("active"))
                                .additionalDetails(ownerAdditional)
                                .createdBy(rs.getString("created_by"))
                                .createdTime(rs.getLong("created_time"))
                                .lastModifiedBy(rs.getString("last_modified_by"))
                                .lastModifiedTime(rs.getLong("last_modified_time"))
                                .build();
                    }
            );

            Map<String, List<PropertyOwner>> result = new HashMap<>();
            for (PropertyOwner owner : owners) {
                result.computeIfAbsent(owner.getPropertyId(), k -> new ArrayList<>()).add(owner);
            }
            return result;
        } catch (Exception e) {
            log.warn("[USER-PROP-REPO] Failed to fetch property owners for IDs: {}", propertyIds, e);
            return Collections.emptyMap();
        }
    }

    /**
     * Looks up an individual rural property by its unique record ID.
     *
     * @param propertyId property primary key ID
     * @return populated {@link UserProperty} or null if not found
     */
    public UserProperty findRuralPropertyById(String propertyId) {
        if (StringUtils.isBlank(propertyId)) {
            return null;
        }
        RuralPropertySearchCriteria criteria = RuralPropertySearchCriteria.builder()
                .propertyId(propertyId.trim())
                .includeDeactivated(true)
                .build();
        List<UserProperty> list = searchRuralProperties(criteria);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    /**
     * Updates the status of a property (e.g. marking as {@code INACTIVE}), updates modification audit,
     * and logs the mutation in the historical audit table.
     *
     * @param propertyId property primary key ID
     * @param userUuid   citizen user UUID (enforces ownership guard)
     * @param status     new lifecycle status (e.g. {@code INACTIVE})
     * @param actorUuid  actor UUID performing the modification
     * @return true if property was found and updated, false otherwise
     */
    public boolean updatePropertyStatus(String propertyId, String userUuid, String status, String actorUuid) {
        if (StringUtils.isBlank(propertyId) || StringUtils.isBlank(userUuid)) {
            return false;
        }

        long now = System.currentTimeMillis();
        Map<String, Object> params = new HashMap<>();
        params.put("id", propertyId.trim());
        params.put("user_uuid", userUuid.trim());
        params.put("status", status != null ? status.trim().toUpperCase() : UserConstants.STATUS_INACTIVE_UPPERCASE);
        params.put("last_modified_by", actorUuid);
        params.put("last_modified_time", now);

        int rows = namedParameterJdbcTemplate.update(queryBuilder.getUpdatePropertyStatusQuery(), params);
        if (rows > 0) {
            log.info("[USER-PROP-REPO] Property ID {} updated to status {} for citizen {}", propertyId, status, userUuid);

            // If marking inactive, also soft-deactivate co-owners
            if (UserConstants.STATUS_INACTIVE_UPPERCASE.equalsIgnoreCase(status)) {
                Map<String, Object> ownerParams = new HashMap<>();
                ownerParams.put("property_id", propertyId.trim());
                ownerParams.put("last_modified_by", actorUuid);
                ownerParams.put("last_modified_time", now);
                try {
                    namedParameterJdbcTemplate.update(queryBuilder.getDeactivatePropertyOwnersQuery(), ownerParams);
                } catch (Exception e) {
                    log.warn("[USER-PROP-REPO] Failed to deactivate property owners for property ID {}: {}", propertyId, e.getMessage());
                }
            }

            // Write audit snapshot
            try {
                UserProperty existing = findRuralPropertyById(propertyId);
                if (existing != null) {
                    Map<String, Object> auditParams = new HashMap<>();
                    auditParams.put("audit_id", UUID.randomUUID().toString());
                    auditParams.put("id", existing.getId());
                    auditParams.put("user_uuid", existing.getUserUuid());
                    auditParams.put("tenant_id", existing.getTenantId());
                    auditParams.put("locality_type", existing.getLocalityType());
                    auditParams.put("ownership_type", existing.getOwnershipType());
                    auditParams.put("declaration_confirmed", existing.getDeclarationConfirmed());
                    auditParams.put("document_id", existing.getDocumentId());
                    auditParams.put("status", status != null ? status.trim().toUpperCase() : UserConstants.STATUS_INACTIVE_UPPERCASE);
                    auditParams.put("additional_details", serializeJson(existing.getAdditionalDetails()));
                    auditParams.put("created_by", existing.getCreatedBy());
                    auditParams.put("created_time", existing.getCreatedTime() != null ? existing.getCreatedTime() : now);
                    auditParams.put("last_modified_by", actorUuid);
                    auditParams.put("last_modified_time", now);

                    namedParameterJdbcTemplate.update(queryBuilder.getInsertPropertyAuditQuery(), auditParams);
                }
            } catch (Exception e) {
                log.warn("[USER-PROP-REPO] Failed to record audit log for status update of property ID {}: {}", propertyId, e.getMessage());
            }

            return true;
        }

        return false;
    }

    /**
     * Retrieves declared property assets (urban and rural) belonging to the specified citizen user UUID.
     * Hydrates child urban/rural details, attached verification documents, and active co-owners.
     *
     * @param userUuid citizen user UUID
     * @param active   optional status filter (true = ACTIVE, false = INACTIVE, null = ALL)
     * @return list of matching {@link UserProperty} domain instances
     */
    public List<UserProperty> findPropertiesByUserUuid(String userUuid, Boolean active) {
        if (StringUtils.isBlank(userUuid)) {
            return Collections.emptyList();
        }
        Map<String, Object> params = new HashMap<>();
        params.put("user_uuid", userUuid.trim());
        String sql = queryBuilder.buildPropertiesByUserQuery(active, params);
        log.debug("[USER-PROP-REPO] Executing properties search SQL: {} with params: {}", sql, params);

        List<UserProperty> properties = namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> mapRowToUserProperty(rs));

        if (properties.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> propertyIds = properties.stream().map(UserProperty::getId).collect(Collectors.toList());
        Map<String, List<PropertyOwner>> ownersByProperty = fetchPropertyOwners(propertyIds);

        for (UserProperty prop : properties) {
            if (ownersByProperty.containsKey(prop.getId())) {
                prop.setOwners(ownersByProperty.get(prop.getId()));
            }
        }

        return properties;
    }

    /**
     * Maps a database result row to a {@link UserProperty} domain entity with urban and rural details.
     *
     * @param rs JDBC result set
     * @return populated {@link UserProperty} entity
     * @throws java.sql.SQLException if SQL reading fails
     */
    private UserProperty mapRowToUserProperty(java.sql.ResultSet rs) throws java.sql.SQLException {
        RuralPropertyDetail rural = null;
        String rId = rs.getString("r_property_record_id");
        if (StringUtils.isNotBlank(rId)) {
            rural = RuralPropertyDetail.builder()
                    .propertyRecordId(rId)
                    .id(rId)
                    .propertyId(rs.getString("r_property_id"))
                    .tenantId(rs.getString("r_tenant_id"))
                    .gramPanchayat(rs.getString("r_gram_panchayat"))
                    .villageName(rs.getString("r_village_name"))
                    .wardNumber(rs.getString("r_ward_number"))
                    .propertyAddress(rs.getString("r_property_address"))
                    .propertyType(rs.getString("r_property_type"))
                    .propertyUsage(rs.getString("r_property_usage"))
                    .surveyNumber(rs.getString("r_survey_number"))
                    .createdBy(rs.getString("r_created_by"))
                    .createdTime(rs.getLong("r_created_time"))
                    .lastModifiedBy(rs.getString("r_last_modified_by"))
                    .lastModifiedTime(rs.getLong("r_last_modified_time"))
                    .build();
        }

        UrbanPropertyDetail urban = null;
        String uId = rs.getString("u_property_record_id");
        if (StringUtils.isNotBlank(uId)) {
            urban = UrbanPropertyDetail.builder()
                    .propertyRecordId(uId)
                    .tenantId(rs.getString("u_tenant_id"))
                    .upyogPropertyId(rs.getString("u_upyog_property_id"))
                    .propertyTaxAmount(rs.getBigDecimal("u_property_tax_amount"))
                    .urbanAddress(rs.getString("u_urban_address"))
                    .createdBy(rs.getString("u_created_by"))
                    .createdTime(rs.getLong("u_created_time"))
                    .lastModifiedBy(rs.getString("u_last_modified_by"))
                    .lastModifiedTime(rs.getLong("u_last_modified_time"))
                    .build();
        }

        UserDocument doc = null;
        String docId = rs.getString("d_id");
        if (StringUtils.isNotBlank(docId)) {
            doc = UserDocument.builder()
                    .id(docId)
                    .documentType(rs.getString("d_document_type"))
                    .filestoreId(rs.getString("d_filestore_id"))
                    .documentUid(rs.getString("d_document_uid"))
                    .fileName(rs.getString("d_file_name"))
                    .isVerified(rs.getObject("d_is_verified") != null ? rs.getBoolean("d_is_verified") : null)
                    .build();
        }

        Object additionalDetails = null;
        String jsonStr = rs.getString("p_additional_details");
        if (StringUtils.isNotBlank(jsonStr)) {
            try {
                additionalDetails = objectMapper.readValue(jsonStr, Object.class);
            } catch (Exception e) {
                log.debug("[USER-PROP-REPO] Unable to deserialize additional_details: {}", e.getMessage());
            }
        }

        return UserProperty.builder()
                .id(rs.getString("p_id"))
                .userUuid(rs.getString("p_user_uuid"))
                .tenantId(rs.getString("p_tenant_id"))
                .localityType(rs.getString("p_locality_type"))
                .ownershipType(rs.getString("p_ownership_type"))
                .declarationConfirmed(rs.getBoolean("p_declaration_confirmed"))
                .documentId(rs.getString("p_document_id"))
                .document(doc)
                .status(rs.getString("p_status"))
                .additionalDetails(additionalDetails)
                .urbanPropertyDetail(urban)
                .ruralPropertyDetail(rural)
                .owners(new ArrayList<>())
                .createdBy(rs.getString("p_created_by"))
                .createdTime(rs.getLong("p_created_time"))
                .lastModifiedBy(rs.getString("p_last_modified_by"))
                .lastModifiedTime(rs.getLong("p_last_modified_time"))
                .build();
    }

    /**
     * Batch retrieves citizen property assets across multiple citizen user UUIDs in a single query.
     * Prevents N+1 query overhead during bulk citizen profile searches.
     *
     * @param userUuids collection of citizen user UUIDs
     * @param active    optional status filter (true = ACTIVE, false = INACTIVE, null = ALL)
     * @return map associating each citizen user UUID to their corresponding list of properties
     */
    public Map<String, List<UserProperty>> findPropertiesByUserUuids(List<String> userUuids, Boolean active) {
        if (userUuids == null || userUuids.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> params = Collections.singletonMap("user_uuids", userUuids);
            String sql = queryBuilder.buildPropertiesByUserUuidsQuery(active);
            List<UserProperty> properties = namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> mapRowToUserProperty(rs));
            if (properties == null || properties.isEmpty()) {
                return Collections.emptyMap();
            }

            List<String> propertyIds = properties.stream().map(UserProperty::getId).collect(Collectors.toList());
            Map<String, List<PropertyOwner>> ownersByProperty = fetchPropertyOwners(propertyIds);

            for (UserProperty prop : properties) {
                if (ownersByProperty.containsKey(prop.getId())) {
                    prop.setOwners(ownersByProperty.get(prop.getId()));
                }
            }

            Map<String, List<UserProperty>> result = new HashMap<>();
            for (UserProperty prop : properties) {
                if (prop != null && StringUtils.isNotBlank(prop.getUserUuid())) {
                    result.computeIfAbsent(prop.getUserUuid(), k -> new ArrayList<>()).add(prop);
                }
            }
            return result;
        } catch (Exception e) {
            log.error("[USER-PROP-REPO] Failed to batch lookup properties by user UUIDs: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Overloaded convenience method retrieving active property assets for a citizen user UUID.
     *
     * @param userUuid citizen user UUID
     * @return list of active {@link UserProperty} domain instances
     */
    public List<UserProperty> findPropertiesByUserUuid(String userUuid) {
        return findPropertiesByUserUuid(userUuid, Boolean.TRUE);
    }
}
