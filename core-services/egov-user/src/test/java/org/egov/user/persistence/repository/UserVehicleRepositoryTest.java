package org.egov.user.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.user.domain.model.asset.UserVehicle;
import org.egov.user.repository.builder.UserVehicleQueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserVehicleRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private UserVehicleQueryBuilder queryBuilder;
    private ObjectMapper objectMapper;
    private UserVehicleRepository vehicleRepository;

    @BeforeEach
    void setUp() {
        queryBuilder = new UserVehicleQueryBuilder();
        objectMapper = new ObjectMapper();
        vehicleRepository = new UserVehicleRepository(namedParameterJdbcTemplate, objectMapper, queryBuilder);
    }

    @Test
    @DisplayName("saveVehicle should bind fuel_type and category parameters into SQL insert and audit statements")
    void testSaveVehicle_BindsFuelTypeAndCategory() {
        UserVehicle vehicle = UserVehicle.builder()
                .id("veh-uuid-1")
                .userUuid("user-uuid-1")
                .tenantId("pg.citya")
                .vehicleNumber("PB02AB1234")
                .vehicleType("4_WHEELER")
                .makerModel("Tata Nexon EV")
                .fuelType("ELECTRIC")
                .category("LMV")
                .build();

        vehicleRepository.saveVehicle(vehicle);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        verify(namedParameterJdbcTemplate, times(2)).update(sqlCaptor.capture(), paramsCaptor.capture());

        String insertSql = sqlCaptor.getAllValues().get(0);
        assertTrue(insertSql.contains("fuel_type"));
        assertTrue(insertSql.contains("category"));
        assertTrue(insertSql.contains(":fuel_type"));
        assertTrue(insertSql.contains(":category"));
        assertTrue(insertSql.contains("fuel_type = EXCLUDED.fuel_type"));
        assertTrue(insertSql.contains("category = EXCLUDED.category"));

        String auditSql = sqlCaptor.getAllValues().get(1);
        assertTrue(auditSql.contains("fuel_type"));
        assertTrue(auditSql.contains("category"));

        Map<String, Object> capturedParams = paramsCaptor.getAllValues().get(0);
        assertEquals("ELECTRIC", capturedParams.get("fuel_type"));
        assertEquals("LMV", capturedParams.get("category"));
        assertEquals("PB02AB1234", capturedParams.get("vehicle_number"));
        assertEquals("Tata Nexon EV", capturedParams.get("maker_model"));
    }
}
