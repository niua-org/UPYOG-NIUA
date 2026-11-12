package org.egov.user.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.user.domain.model.asset.PropertyOwner;
import org.egov.user.domain.model.asset.RuralPropertyDetail;
import org.egov.user.domain.model.asset.UrbanPropertyDetail;
import org.egov.user.domain.model.asset.UserProperty;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.repository.builder.UserPropertyQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
     *
     * @param property property entity to persist
     */
    public void saveProperty(UserProperty property) {
        if (StringUtils.isBlank(property.getId())) {
            property.setId(UUID.randomUUID().toString());
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
            owner.setId(UUID.randomUUID().toString());
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
}
