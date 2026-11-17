package org.egov.user.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.model.asset.UserIncome;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.repository.builder.UserIncomeQueryBuilder;
import org.egov.user.web.contract.asset.IncomeSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Repository performing transactional persistence for citizen income declarations in
 * {@code ug_user_income} and historical auditing in {@code ug_user_income_audit}.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Repository
@Slf4j
public class UserIncomeRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ObjectMapper objectMapper;
    private final UserIncomeQueryBuilder queryBuilder;

    /**
     * Constructs a {@code UserIncomeRepository} with required JDBC operations template, JSON mapper, and query builder.
     *
     * @param namedParameterJdbcTemplate named parameter JDBC operations template
     * @param objectMapper Jackson JSON mapper for serialization
     * @param queryBuilder SQL query builder for income declarations
     */
    @Autowired
    public UserIncomeRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate,
                                ObjectMapper objectMapper,
                                UserIncomeQueryBuilder queryBuilder) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.objectMapper = objectMapper;
        this.queryBuilder = queryBuilder;
    }

    /**
     * Persists citizen income record into {@code ug_user_income} and writes an audit row.
     * Enforces natural key deduplication and foreign income record ownership validation.
     *
     * @param income income declaration entity
     */
    public void saveIncome(UserIncome income) {
        // Natural key deduplication: if id is blank, reuse existing active income record ID for this citizen
        if (StringUtils.isBlank(income.getId())) {
            String existingId = findActiveIncomeIdByUserUuid(income.getUserUuid());
            if (existingId != null) {
                income.setId(existingId);
            } else {
                income.setId(UUID.randomUUID().toString());
            }
        } else {
            // Ownership boundary check: verify client-supplied id does not belong to another citizen
            String existingOwner = findUserUuidByIncomeId(income.getId());
            if (existingOwner != null && !existingOwner.equals(income.getUserUuid())) {
                log.warn("[USER-INCOME-REPO] Blocked attempt to overwrite foreign income ID {} owned by {} with data for {}",
                        income.getId(), existingOwner, income.getUserUuid());
                income.setId(UUID.randomUUID().toString());
            }
        }
        long now = System.currentTimeMillis();
        if (income.getCreatedTime() == null) {
            income.setCreatedTime(now);
        }
        income.setLastModifiedTime(now);
        if (StringUtils.isBlank(income.getStatus())) {
            income.setStatus(UserConstants.STATUS_ACTIVE_UPPERCASE);
        }
        if (income.getIsVerified() == null) {
            income.setIsVerified(false);
        }
        if (income.getOtpVerified() == null) {
            income.setOtpVerified(false);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("id", income.getId());
        params.put("user_uuid", income.getUserUuid());
        params.put("tenant_id", income.getTenantId());
        params.put("occupation", income.getOccupation());
        params.put("annual_income", income.getAnnualIncome());
        params.put("identifier_type", income.getIdentifierType());
        params.put("identifier_value", income.getIdentifierValue());
        params.put("is_verified", income.getIsVerified());
        params.put("verification_source", income.getVerificationSource());
        params.put("verification_date", income.getVerificationDate());
        params.put("otp_verified", income.getOtpVerified());
        params.put("salary_proof_doc_id", income.getSalaryProofDocId());
        params.put("tax_proof_doc_id", income.getTaxProofDocId());
        params.put("status", income.getStatus());
        params.put("additional_details", serializeJson(income.getAdditionalDetails()));
        params.put("created_by", income.getCreatedBy());
        params.put("created_time", income.getCreatedTime());
        params.put("last_modified_by", income.getLastModifiedBy());
        params.put("last_modified_time", income.getLastModifiedTime());

        namedParameterJdbcTemplate.update(queryBuilder.getInsertIncomeQuery(), params);

        // Audit entry
        params.put("audit_id", UUID.randomUUID().toString());
        namedParameterJdbcTemplate.update(queryBuilder.getInsertIncomeAuditQuery(), params);
        log.info("[USER-INCOME-REPO] Persisted income id: {} for user: {}", income.getId(), income.getUserUuid());
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
            log.warn("[USER-INCOME-REPO] Failed to serialize additionalDetails: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Looks up existing active income record ID for a citizen to enable idempotent re-sync.
     *
     * @param userUuid citizen user UUID
     * @return active income record ID or null
     */
    public String findActiveIncomeIdByUserUuid(String userUuid) {
        if (StringUtils.isBlank(userUuid)) {
            return null;
        }
        try {
            Map<String, Object> params = Collections.singletonMap("user_uuid", userUuid);
            List<String> list = namedParameterJdbcTemplate.query(
                    UserIncomeQueryBuilder.SELECT_ACTIVE_INCOME_ID_BY_USER_SQL,
                    params,
                    (rs, rowNum) -> rs.getString("id")
            );
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        } catch (Exception e) {
            log.debug("[USER-INCOME-REPO] Failed to lookup active income ID: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Looks up user_uuid of existing income record by primary key ID.
     *
     * @param incomeId income record ID
     * @return userUuid owning the income record or null
     */
    public String findUserUuidByIncomeId(String incomeId) {
        if (StringUtils.isBlank(incomeId)) {
            return null;
        }
        try {
            Map<String, Object> params = Collections.singletonMap("id", incomeId);
            List<String> list = namedParameterJdbcTemplate.query(
                    UserIncomeQueryBuilder.SELECT_INCOME_USER_UUID_BY_ID_SQL,
                    params,
                    (rs, rowNum) -> rs.getString("user_uuid")
            );
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        } catch (Exception e) {
            log.debug("[USER-INCOME-REPO] Failed to lookup income owner userUuid: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves a single citizen income declaration by primary key identifier.
     * Hydrates child salary and tax proof verification documents if present.
     *
     * @param id income record primary key identifier
     * @return matched {@link UserIncome} entity or null if not found
     */
    public UserIncome findIncomeById(String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        try {
            Map<String, Object> params = Collections.singletonMap("id", id.trim());
            List<UserIncome> list = namedParameterJdbcTemplate.query(
                    UserIncomeQueryBuilder.SELECT_INCOME_BY_ID_SQL,
                    params,
                    (rs, rowNum) -> mapRowToUserIncome(rs)
            );
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        } catch (Exception e) {
            log.error("[USER-INCOME-REPO] Failed to lookup income by ID {}: {}", id, e.getMessage());
            return null;
        }
    }

    /**
     * Executes a dynamic search for citizen income declarations based on criteria filters.
     *
     * @param criteria search criteria filters
     * @return list of matched {@link UserIncome} entities
     */
    public List<UserIncome> searchIncome(IncomeSearchCriteria criteria) {
        try {
            Map<String, Object> params = new HashMap<>();
            String sql = queryBuilder.buildSearchQuery(criteria, params);
            return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> mapRowToUserIncome(rs));
        } catch (Exception e) {
            log.error("[USER-INCOME-REPO] Failed to search income declarations: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Updates the status of an income declaration (used for soft-deactivation).
     *
     * @param incomeId   income record primary key ID
     * @param userUuid   citizen user UUID owning the record
     * @param status     new lifecycle status (e.g. INACTIVE)
     * @param actorUuid  audit actor UUID
     * @return true if updated successfully, false otherwise
     */
    public boolean updateIncomeStatus(String incomeId, String userUuid, String status, String actorUuid) {
        if (StringUtils.isBlank(incomeId) || StringUtils.isBlank(userUuid)) {
            return false;
        }
        UserIncome existing = findIncomeById(incomeId);
        if (existing == null || !userUuid.equals(existing.getUserUuid())) {
            return false;
        }
        existing.setStatus(status != null ? status.trim().toUpperCase() : UserConstants.STATUS_INACTIVE_UPPERCASE);
        existing.setLastModifiedBy(actorUuid);
        saveIncome(existing);
        return true;
    }

    /**
     * Retrieves declared citizen income record for the specified citizen user UUID.
     * Hydrates child salary and tax proof verification documents.
     *
     * @param userUuid citizen user UUID
     * @param active   optional status filter (true = ACTIVE, false = INACTIVE, null = ALL)
     * @return {@link UserIncome} domain instance or null if not found
     */
    public UserIncome findActiveIncomeByUserUuid(String userUuid, Boolean active) {
        if (StringUtils.isBlank(userUuid)) {
            return null;
        }
        try {
            Map<String, Object> params = Collections.singletonMap("user_uuid", userUuid.trim());
            String sql = queryBuilder.buildIncomeByUserQuery(active);
            List<UserIncome> list = namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> mapRowToUserIncome(rs));
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        } catch (Exception e) {
            log.error("[USER-INCOME-REPO] Failed to lookup income by user UUID {}: {}", userUuid, e.getMessage());
            return null;
        }
    }

    /**
     * Batch retrieves active citizen income declarations for multiple user UUIDs in a single query.
     * Prevents N+1 query overhead during bulk citizen profile searches.
     *
     * @param userUuids collection of citizen user UUIDs
     * @param active    optional status filter (true = ACTIVE, false = INACTIVE, null = ALL)
     * @return map associating each citizen user UUID to their corresponding active income declaration
     */
    public Map<String, UserIncome> findActiveIncomesByUserUuids(List<String> userUuids, Boolean active) {
        if (userUuids == null || userUuids.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> params = Collections.singletonMap("user_uuids", userUuids);
            String sql = queryBuilder.buildIncomeByUserUuidsQuery(active);
            List<UserIncome> list = namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> mapRowToUserIncome(rs));
            Map<String, UserIncome> result = new HashMap<>();
            if (list != null) {
                for (UserIncome inc : list) {
                    if (inc != null && StringUtils.isNotBlank(inc.getUserUuid())) {
                        result.putIfAbsent(inc.getUserUuid(), inc);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.error("[USER-INCOME-REPO] Failed to batch lookup income by user UUIDs: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Maps a database result row to a {@link UserIncome} entity with attached documents.
     *
     * @param rs JDBC result set
     * @return populated {@link UserIncome} entity
     * @throws java.sql.SQLException if SQL reading fails
     */
    private UserIncome mapRowToUserIncome(java.sql.ResultSet rs) throws java.sql.SQLException {
        UserDocument salaryDoc = null;
        String sdId = rs.getString("sd_id");
        if (StringUtils.isNotBlank(sdId)) {
            salaryDoc = UserDocument.builder()
                    .id(sdId)
                    .documentType(rs.getString("sd_document_type"))
                    .filestoreId(rs.getString("sd_filestore_id"))
                    .documentUid(rs.getString("sd_document_uid"))
                    .fileName(rs.getString("sd_file_name"))
                    .isVerified(rs.getObject("sd_is_verified") != null ? rs.getBoolean("sd_is_verified") : null)
                    .build();
        }

        UserDocument taxDoc = null;
        String tdId = rs.getString("td_id");
        if (StringUtils.isNotBlank(tdId)) {
            taxDoc = UserDocument.builder()
                    .id(tdId)
                    .documentType(rs.getString("td_document_type"))
                    .filestoreId(rs.getString("td_filestore_id"))
                    .documentUid(rs.getString("td_document_uid"))
                    .fileName(rs.getString("td_file_name"))
                    .isVerified(rs.getObject("td_is_verified") != null ? rs.getBoolean("td_is_verified") : null)
                    .build();
        }

        Object additionalDetails = null;
        String jsonStr = rs.getString("i_additional_details");
        if (StringUtils.isNotBlank(jsonStr)) {
            try {
                additionalDetails = objectMapper.readValue(jsonStr, Object.class);
            } catch (Exception e) {
                log.debug("[USER-INCOME-REPO] Unable to deserialize additional_details: {}", e.getMessage());
            }
        }

        return UserIncome.builder()
                .id(rs.getString("i_id"))
                .userUuid(rs.getString("i_user_uuid"))
                .tenantId(rs.getString("i_tenant_id"))
                .occupation(rs.getString("i_occupation"))
                .annualIncome(rs.getBigDecimal("i_annual_income"))
                .identifierType(rs.getString("i_identifier_type"))
                .identifierValue(rs.getString("i_identifier_value"))
                .isVerified(rs.getObject("i_is_verified") != null ? rs.getBoolean("i_is_verified") : null)
                .verificationSource(rs.getString("i_verification_source"))
                .verificationDate(rs.getObject("i_verification_date") != null ? rs.getLong("i_verification_date") : null)
                .otpVerified(rs.getObject("i_otp_verified") != null ? rs.getBoolean("i_otp_verified") : null)
                .salaryProofDocId(rs.getString("i_salary_proof_doc_id"))
                .salaryProofDocument(salaryDoc)
                .taxProofDocId(rs.getString("i_tax_proof_doc_id"))
                .taxProofDocument(taxDoc)
                .status(rs.getString("i_status"))
                .additionalDetails(additionalDetails)
                .createdBy(rs.getString("i_created_by"))
                .createdTime(rs.getLong("i_created_time"))
                .lastModifiedBy(rs.getString("i_last_modified_by"))
                .lastModifiedTime(rs.getObject("i_last_modified_time") != null ? rs.getLong("i_last_modified_time") : null)
                .build();
    }

    /**
     * Overloaded convenience method retrieving active income declaration for a citizen user UUID.
     *
     * @param userUuid citizen user UUID
     * @return active {@link UserIncome} domain instance or null
     */
    public UserIncome findActiveIncomeByUserUuid(String userUuid) {
        return findActiveIncomeByUserUuid(userUuid, Boolean.TRUE);
    }
}
