package org.egov.user.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.user.domain.model.asset.UserIncome;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.repository.builder.UserIncomeQueryBuilder;
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
}
