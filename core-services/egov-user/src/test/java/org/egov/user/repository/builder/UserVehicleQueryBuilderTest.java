package org.egov.user.repository.builder;

import org.egov.user.web.contract.asset.VehicleSearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @DisplayName("Should build criteria search query with all filter parameters")
    public void testBuildSearchQuery_WithFullCriteria() {
        Map<String, Object> params = new HashMap<>();
        VehicleSearchCriteria criteria = VehicleSearchCriteria.builder()
                .userUuid("user-uuid-001")
                .vehicleId("veh-id-001")
                .vehicleNumber("DL01AB1234")
                .tenantId("pg.citya")
                .vehicleType("4_WHEELER")
                .fuelType("ELECTRIC")
                .category("LMV")
                .status("ACTIVE")
                .limit(20)
                .offset(0)
                .build();

        String sql = queryBuilder.buildSearchQuery(criteria, params);

        assertNotNull(sql);
        assertTrue(sql.contains("v.user_uuid = :user_uuid"));
        assertTrue(sql.contains("v.id = :id"));
        assertTrue(sql.contains("UPPER(v.vehicle_number) = UPPER(:vehicle_number)"));
        assertTrue(sql.contains("v.tenant_id = :tenant_id"));
        assertTrue(sql.contains("UPPER(v.vehicle_type) = UPPER(:vehicle_type)"));
        assertTrue(sql.contains("UPPER(v.fuel_type) = UPPER(:fuel_type)"));
        assertTrue(sql.contains("UPPER(v.category) = UPPER(:category)"));
        assertTrue(sql.contains("v.status = :status"));
        assertTrue(sql.contains("LIMIT :limit"));
        assertTrue(sql.contains("OFFSET :offset"));

        assertEquals("user-uuid-001", params.get("user_uuid"));
        assertEquals("veh-id-001", params.get("id"));
        assertEquals("DL01AB1234", params.get("vehicle_number"));
        assertEquals("pg.citya", params.get("tenant_id"));
        assertEquals("4_WHEELER", params.get("vehicle_type"));
        assertEquals("ELECTRIC", params.get("fuel_type"));
        assertEquals("LMV", params.get("category"));
        assertEquals("ACTIVE", params.get("status"));
        assertEquals(20, params.get("limit"));
        assertEquals(0, params.get("offset"));
    }

    @Test
    @DisplayName("Should build default active status search query when criteria is null")
    public void testBuildSearchQuery_NullCriteria() {
        Map<String, Object> params = new HashMap<>();
        String sql = queryBuilder.buildSearchQuery(null, params);

        assertNotNull(sql);
        assertTrue(sql.contains("v.status = 'ACTIVE'"));
        assertTrue(sql.contains("ORDER BY v.created_time DESC"));
        assertTrue(sql.contains("LIMIT :limit OFFSET :offset"));
        assertEquals(UserVehicleQueryBuilder.DEFAULT_SEARCH_LIMIT, params.get("limit"));
        assertEquals(UserVehicleQueryBuilder.DEFAULT_SEARCH_OFFSET, params.get("offset"));
    }

    @Test
    @DisplayName("Should cap vehicle search limit to MAX_SEARCH_LIMIT when requested limit exceeds cap")
    public void testBuildSearchQuery_CappedLimit() {
        Map<String, Object> params = new HashMap<>();
        VehicleSearchCriteria criteria = VehicleSearchCriteria.builder()
                .userUuid("user-uuid-001")
                .limit(200)
                .offset(15)
                .build();

        String sql = queryBuilder.buildSearchQuery(criteria, params);

        assertTrue(sql.contains("LIMIT :limit OFFSET :offset"));
        assertEquals(UserVehicleQueryBuilder.MAX_SEARCH_LIMIT, params.get("limit"));
        assertEquals(15, params.get("offset"));
    }

    @Test
    @DisplayName("Should return valid static insert, audit, and by-id SQL queries")
    public void testStaticQueries() {
        assertNotNull(queryBuilder.getInsertVehicleQuery());
        assertTrue(queryBuilder.getInsertVehicleQuery().contains("INSERT INTO ug_user_vehicle"));
        assertNotNull(queryBuilder.getInsertVehicleAuditQuery());
        assertTrue(queryBuilder.getInsertVehicleAuditQuery().contains("INSERT INTO ug_user_vehicle_audit"));
        assertNotNull(UserVehicleQueryBuilder.SELECT_VEHICLE_BY_ID_SQL);
        assertTrue(UserVehicleQueryBuilder.SELECT_VEHICLE_BY_ID_SQL.contains("WHERE v.id = :id"));
    }
}
