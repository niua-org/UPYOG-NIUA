package org.upyog.family.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.upyog.family.persistence.builder.StateFamilySyncQueryBuilder;
import org.upyog.family.web.contract.state.StateFamily;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository component handling staging and retrieval of State Family sync payloads (UC2).
 * Implements a dual-layer strategy: fast in-memory ConcurrentHashMap cache with persistent PostgreSQL backing.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Repository
@Slf4j
public class StateFamilySyncRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final org.upyog.family.persistence.builder.StateFamilySyncQueryBuilder queryBuilder;

    // Dual-layer cache for fast sub-millisecond retrieval
    private final Map<String, StateFamily> cacheByReferenceId = new ConcurrentHashMap<>();
    private final Map<String, String> referenceIdByStateFamilyId = new ConcurrentHashMap<>();

    @Autowired
    public StateFamilySyncRepository(JdbcTemplate jdbcTemplate,
                                     ObjectMapper objectMapper,
                                     org.upyog.family.persistence.builder.StateFamilySyncQueryBuilder queryBuilder) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.queryBuilder = queryBuilder;
    }

    /**
     * Staging save of StateFamily payload with reference identifier.
     *
     * @param referenceId generated unique reference UUID
     * @param stateFamily domain payload from state registry
     */
    public void save(String referenceId, StateFamily stateFamily) {
        long now = System.currentTimeMillis();
        cacheByReferenceId.put(referenceId, stateFamily);
        if (StringUtils.isNotBlank(stateFamily.getStateFamilyId())) {
            referenceIdByStateFamilyId.put(stateFamily.getStateFamilyId(), referenceId);
        }

        try {
            String payloadJson = objectMapper.writeValueAsString(stateFamily);
            jdbcTemplate.update(
                    StateFamilySyncQueryBuilder.INSERT_SYNC_SQL,
                    referenceId,
                    stateFamily.getStateCode(),
                    stateFamily.getStateFamilyId(),
                    stateFamily.getTenantId(),
                    stateFamily.getFamilyName(),
                    "STAGED",
                    payloadJson,
                    now,
                    now
            );
            log.info("[STATE-SYNC-REPO] Persisted state family sync to DB: refId={}, stateFamilyId={}",
                    referenceId, stateFamily.getStateFamilyId());
        } catch (Exception e) {
            log.warn("[STATE-SYNC-REPO] Could not persist state family sync to DB (cache intact): {}", e.getMessage());
        }
    }

    /**
     * Retrieves StateFamily by reference ID from cache or PostgreSQL.
     *
     * @param referenceId the unique reference UUID
     * @return StateFamily or null if not found
     */
    public StateFamily findByReferenceId(String referenceId) {
        if (StringUtils.isBlank(referenceId)) {
            return null;
        }
        if (cacheByReferenceId.containsKey(referenceId)) {
            return cacheByReferenceId.get(referenceId);
        }

        try {
            String json = jdbcTemplate.queryForObject(StateFamilySyncQueryBuilder.SELECT_BY_REF_SQL, String.class, referenceId);
            if (StringUtils.isNotBlank(json)) {
                StateFamily family = objectMapper.readValue(json, StateFamily.class);
                cacheByReferenceId.put(referenceId, family);
                return family;
            }
        } catch (Exception e) {
            log.debug("[STATE-SYNC-REPO] Record not found in DB for refId: {}", referenceId);
        }
        return null;
    }

    /**
     * Retrieves StateFamily by State Family ID.
     *
     * @param stateFamilyId external state household identifier
     * @return StateFamily or null if not found
     */
    public StateFamily findByStateFamilyId(String stateFamilyId) {
        if (StringUtils.isBlank(stateFamilyId)) {
            return null;
        }
        String refId = referenceIdByStateFamilyId.get(stateFamilyId);
        if (refId != null && cacheByReferenceId.containsKey(refId)) {
            return cacheByReferenceId.get(refId);
        }

        try {
            return jdbcTemplate.query(StateFamilySyncQueryBuilder.SELECT_BY_STATE_FAMILY_ID_SQL, rs -> {
                if (rs.next()) {
                    String ref = rs.getString("reference_id");
                    String json = rs.getString("payload");
                    try {
                        StateFamily family = objectMapper.readValue(json, StateFamily.class);
                        cacheByReferenceId.put(ref, family);
                        referenceIdByStateFamilyId.put(stateFamilyId, ref);
                        return family;
                    } catch (Exception ex) {
                        log.error("[STATE-SYNC-REPO] Error deserializing state family: {}", ex.getMessage());
                    }
                }
                return null;
            }, stateFamilyId);
        } catch (Exception e) {
            log.debug("[STATE-SYNC-REPO] State family not found in DB for: {}", stateFamilyId);
        }
        return null;
    }
}
