package org.egov.user.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.user.domain.model.asset.UserVehicle;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.repository.builder.UserVehicleQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Repository performing transactional persistence for citizen vehicle declarations in
 * {@code ug_user_vehicle} and historical auditing in {@code ug_user_vehicle_audit}.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Repository
@Slf4j
public class UserVehicleRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ObjectMapper objectMapper;
    private final UserVehicleQueryBuilder queryBuilder;

    /**
     * Constructs a {@code UserVehicleRepository} with required JDBC operations template, JSON mapper, and query builder.
     *
     * @param namedParameterJdbcTemplate named parameter JDBC operations template
     * @param objectMapper Jackson JSON mapper for serialization
     * @param queryBuilder SQL query builder for vehicle declarations
     */
    @Autowired
    public UserVehicleRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate,
                                 ObjectMapper objectMapper,
                                 UserVehicleQueryBuilder queryBuilder) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.objectMapper = objectMapper;
        this.queryBuilder = queryBuilder;
    }

    /**
     * Persists declared vehicle record into {@code ug_user_vehicle} and writes an audit row.
     * Enforces natural key deduplication on vehicle registration number and foreign record ownership boundary.
     *
     * @param vehicle vehicle declaration entity
     */
    public void saveVehicle(UserVehicle vehicle) {
        // Natural key deduplication: if id is blank, lookup existing active vehicle by (user_uuid, vehicle_number)
        if (StringUtils.isBlank(vehicle.getId())) {
            if (StringUtils.isNotBlank(vehicle.getVehicleNumber())) {
                String existingId = findVehicleIdByUserAndNumber(vehicle.getUserUuid(), vehicle.getVehicleNumber());
                if (existingId != null) {
                    vehicle.setId(existingId);
                }
            }
            if (StringUtils.isBlank(vehicle.getId())) {
                vehicle.setId(UUID.randomUUID().toString());
            }
        } else {
            // Ownership boundary check: verify client-supplied id does not belong to another citizen
            String existingOwner = findUserUuidByVehicleId(vehicle.getId());
            if (existingOwner != null && !existingOwner.equals(vehicle.getUserUuid())) {
                log.warn("[USER-VEHICLE-REPO] Blocked attempt to overwrite foreign vehicle ID {} owned by {} with data for {}",
                        vehicle.getId(), existingOwner, vehicle.getUserUuid());
                vehicle.setId(UUID.randomUUID().toString());
            }
        }
        long now = System.currentTimeMillis();
        if (vehicle.getCreatedTime() == null) {
            vehicle.setCreatedTime(now);
        }
        vehicle.setLastModifiedTime(now);
        if (StringUtils.isBlank(vehicle.getStatus())) {
            vehicle.setStatus(UserConstants.STATUS_ACTIVE_UPPERCASE);
        }
        if (vehicle.getIsVerified() == null) {
            vehicle.setIsVerified(false);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("id", vehicle.getId());
        params.put("user_uuid", vehicle.getUserUuid());
        params.put("tenant_id", vehicle.getTenantId());
        params.put("vehicle_number", vehicle.getVehicleNumber());
        params.put("purchase_date", parsePurchaseDate(vehicle.getPurchaseDate()));
        params.put("is_verified", vehicle.getIsVerified());
        params.put("validation_source", vehicle.getValidationSource());
        params.put("rc_doc_id", vehicle.getRcDocId());
        params.put("vehicle_type", vehicle.getVehicleType());
        params.put("maker_model", vehicle.getMakerModel());
        params.put("fuel_type", vehicle.getFuelType());
        params.put("category", vehicle.getCategory());
        params.put("status", vehicle.getStatus());
        params.put("additional_details", serializeJson(vehicle.getAdditionalDetails()));
        params.put("created_by", vehicle.getCreatedBy());
        params.put("created_time", vehicle.getCreatedTime());
        params.put("last_modified_by", vehicle.getLastModifiedBy());
        params.put("last_modified_time", vehicle.getLastModifiedTime());

        namedParameterJdbcTemplate.update(queryBuilder.getInsertVehicleQuery(), params);

        // Audit entry
        params.put("audit_id", UUID.randomUUID().toString());
        namedParameterJdbcTemplate.update(queryBuilder.getInsertVehicleAuditQuery(), params);
        log.info("[USER-VEHICLE-REPO] Persisted vehicle id: {} for user: {}", vehicle.getId(), vehicle.getUserUuid());
    }

    /**
     * Parses a flexible vehicle purchase date string (epoch timestamp, ISO yyyy-MM-dd, or dd-MM-yyyy) into a SQL Date.
     *
     * @param dateStr input date string
     * @return parsed {@link Date} object or null if input is empty or cannot be parsed
     */
    private Date parsePurchaseDate(String dateStr) {
        if (StringUtils.isBlank(dateStr)) return null;
        try {
            if (dateStr.matches("^\\d+$")) {
                long epoch = Long.parseLong(dateStr);
                return new Date(epoch);
            }
            if (dateStr.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                return Date.valueOf(dateStr);
            }
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            return new Date(sdf.parse(dateStr).getTime());
        } catch (Exception e) {
            log.warn("[USER-VEHICLE-REPO] Unable to parse purchase date '\''{}'\'': {}", dateStr, e.getMessage());
            return null;
        }
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
            log.warn("[USER-VEHICLE-REPO] Failed to serialize additionalDetails: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Looks up existing active vehicle ID by citizen user UUID and vehicle registration number.
     *
     * @param userUuid citizen user UUID
     * @param vehicleNumber registration plate number
     * @return existing vehicle record ID or null
     */
    public String findVehicleIdByUserAndNumber(String userUuid, String vehicleNumber) {
        if (StringUtils.isBlank(userUuid) || StringUtils.isBlank(vehicleNumber)) {
            return null;
        }
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("user_uuid", userUuid);
            params.put("vehicle_number", vehicleNumber.trim());
            List<String> list = namedParameterJdbcTemplate.query(
                    UserVehicleQueryBuilder.SELECT_ACTIVE_VEHICLE_ID_BY_USER_AND_NUMBER_SQL,
                    params,
                    (rs, rowNum) -> rs.getString("id")
            );
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        } catch (Exception e) {
            log.debug("[USER-VEHICLE-REPO] Failed to lookup existing vehicle by number: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Looks up user_uuid of existing vehicle record by primary key ID.
     *
     * @param vehicleId vehicle primary key ID
     * @return userUuid owning the vehicle or null
     */
    public String findUserUuidByVehicleId(String vehicleId) {
        if (StringUtils.isBlank(vehicleId)) {
            return null;
        }
        try {
            Map<String, Object> params = Collections.singletonMap("id", vehicleId);
            List<String> list = namedParameterJdbcTemplate.query(
                    UserVehicleQueryBuilder.SELECT_VEHICLE_USER_UUID_BY_ID_SQL,
                    params,
                    (rs, rowNum) -> rs.getString("user_uuid")
            );
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        } catch (Exception e) {
            log.debug("[USER-VEHICLE-REPO] Failed to lookup vehicle owner userUuid: {}", e.getMessage());
            return null;
        }
    }
}
