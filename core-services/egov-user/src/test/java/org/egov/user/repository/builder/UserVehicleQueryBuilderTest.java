package org.egov.user.repository.builder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests verifying SQL query generation in {@link UserVehicleQueryBuilder}.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public class UserVehicleQueryBuilderTest {

    private UserVehicleQueryBuilder queryBuilder;

    @BeforeEach
    public void setUp() {
        queryBuilder = new UserVehicleQueryBuilder();
    }

    @Test
    @DisplayName("Should build vehicles by user query with active status filter")
    public void testBuildVehiclesByUserQuery_Active() {
        String sql = queryBuilder.buildVehiclesByUserQuery(true);
        assertNotNull(sql);
        assertTrue(sql.contains("WHERE v.user_uuid = :user_uuid"));
        assertTrue(sql.contains("AND v.status = 'ACTIVE'"));
        assertTrue(sql.contains("ORDER BY v.created_time DESC"));
    }

    @Test
    @DisplayName("Should build vehicles by user query with inactive status filter")
    public void testBuildVehiclesByUserQuery_Inactive() {
        String sql = queryBuilder.buildVehiclesByUserQuery(false);
        assertNotNull(sql);
        assertTrue(sql.contains("WHERE v.user_uuid = :user_uuid"));
        assertTrue(sql.contains("AND v.status = 'INACTIVE'"));
        assertTrue(sql.contains("ORDER BY v.created_time DESC"));
    }

    @Test
    @DisplayName("Should build vehicles by user query without status filter when active is null")
    public void testBuildVehiclesByUserQuery_AllStatus() {
        String sql = queryBuilder.buildVehiclesByUserQuery(null);
        assertNotNull(sql);
        assertTrue(sql.contains("WHERE v.user_uuid = :user_uuid"));
        assertFalse(sql.contains("AND v.status ="));
        assertTrue(sql.contains("ORDER BY v.created_time DESC"));
    }

    @Test
    @DisplayName("Should return valid static insert and audit SQL queries")
    public void testStaticQueries() {
        assertNotNull(queryBuilder.getInsertVehicleQuery());
        assertTrue(queryBuilder.getInsertVehicleQuery().contains("INSERT INTO ug_user_vehicle"));
        assertNotNull(queryBuilder.getInsertVehicleAuditQuery());
        assertTrue(queryBuilder.getInsertVehicleAuditQuery().contains("INSERT INTO ug_user_vehicle_audit"));
    }
}
