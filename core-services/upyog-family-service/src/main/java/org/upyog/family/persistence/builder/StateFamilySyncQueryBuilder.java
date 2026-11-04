package org.upyog.family.persistence.builder;

import org.springframework.stereotype.Component;

/**
 * SQL Query Builder component for State Family Sync persistence targeting {@code ug_state_family_sync}.
 * Follows the standard UPYOG QueryBuilder architectural pattern.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
public class StateFamilySyncQueryBuilder {

    public static final String INSERT_SYNC_SQL =
            "INSERT INTO ug_state_family_sync (reference_id, state_code, state_family_id, tenant_id, "
            + "family_name, status, payload, created_time, last_modified_time) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?)";

    public static final String SELECT_BY_REF_SQL =
            "SELECT payload FROM ug_state_family_sync WHERE reference_id = ?";

    public static final String SELECT_BY_STATE_FAMILY_ID_SQL =
            "SELECT reference_id, payload FROM ug_state_family_sync WHERE state_family_id = ? "
            + "ORDER BY created_time DESC LIMIT 1";

}

