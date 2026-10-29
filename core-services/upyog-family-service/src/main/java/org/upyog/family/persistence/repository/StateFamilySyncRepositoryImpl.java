package org.upyog.family.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.upyog.family.persistence.builder.StateFamilySyncQueryBuilder;
import org.upyog.family.util.FamilyConstants;
import org.upyog.family.web.contract.state.StateFamily;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link StateFamilySyncRepository} handling staging and retrieval of State Family sync payloads (UC2).
 * Implements a dual-layer strategy: fast in-memory ConcurrentHashMap cache with persistent PostgreSQL backing.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Repository
@Slf4j
public class StateFamilySyncRepositoryImpl implements StateFamilySyncRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final StateFamilySyncQueryBuilder queryBuilder;

    // Dual-layer cache for fast sub-millisecond retrieval
    private final Map<String, StateFamily> cacheByReferenceId = new ConcurrentHashMap<>();
    private final Map<String, String> referenceIdByStateFamilyId = new ConcurrentHashMap<>();

    @Autowired
    public StateFamilySyncRepositoryImpl(JdbcTemplate jdbcTemplate,
                                         ObjectMapper objectMapper,
                                         StateFamilySyncQueryBuilder queryBuilder) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.queryBuilder = queryBuilder;
    }

    @Override
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
                    FamilyConstants.STATUS_STAGED,
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

    @Override
    public StateFamily findByReferenceId(String referenceId) {
        if (StringUtils.isBlank(referenceId)) {
            return null;
        }

        // 1. In-memory Cache Lookup
        StateFamily cached = cacheByReferenceId.get(referenceId);
        if (cached != null) {
            return cached;
        }

        // 2. Persistent Database Fallback
        try {
            String payloadJson = jdbcTemplate.queryForObject(
                    StateFamilySyncQueryBuilder.SELECT_BY_REF_SQL,
                    String.class,
                    referenceId
            );
            if (StringUtils.isNotBlank(payloadJson)) {
                StateFamily loaded = objectMapper.readValue(payloadJson, StateFamily.class);
                cacheByReferenceId.put(referenceId, loaded);
                if (StringUtils.isNotBlank(loaded.getStateFamilyId())) {
                    referenceIdByStateFamilyId.put(loaded.getStateFamilyId(), referenceId);
                }
                return loaded;
            }
        } catch (Exception e) {
            log.debug("[STATE-SYNC-REPO] DB lookup miss for referenceId: {}", referenceId);
        }

        return null;
    }

    @Override
    public StateFamily findByStateFamilyId(String stateFamilyId) {
        if (StringUtils.isBlank(stateFamilyId)) {
            return null;
        }

        // 1. Cache lookup via index mapping
        String refId = referenceIdByStateFamilyId.get(stateFamilyId);
        if (refId != null) {
            StateFamily cached = cacheByReferenceId.get(refId);
            if (cached != null) {
                return cached;
            }
        }

        // 2. Persistent Database Fallback
        try {
            String payloadJson = jdbcTemplate.queryForObject(
                    StateFamilySyncQueryBuilder.SELECT_BY_STATE_FAMILY_ID_SQL,
                    String.class,
                    stateFamilyId
            );
            if (StringUtils.isNotBlank(payloadJson)) {
                StateFamily loaded = objectMapper.readValue(payloadJson, StateFamily.class);
                if (refId != null) {
                    cacheByReferenceId.put(refId, loaded);
                }
                return loaded;
            }
        } catch (Exception e) {
            log.debug("[STATE-SYNC-REPO] DB lookup miss for stateFamilyId: {}", stateFamilyId);
        }

        return null;
    }
}
