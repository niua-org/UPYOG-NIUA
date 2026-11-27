package org.egov.user.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.user.domain.model.asset.RuralPropertyDetail;
import org.egov.user.domain.model.asset.UserProperty;
import org.egov.user.repository.builder.UserPropertyQueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests verifying persistence and retrieval operations in {@link UserPropertyRepository}.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class UserPropertyRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private UserPropertyQueryBuilder queryBuilder;
    private ObjectMapper objectMapper;
    private UserPropertyRepository propertyRepository;

    @BeforeEach
    void setUp() {
        queryBuilder = new UserPropertyQueryBuilder();
        objectMapper = new ObjectMapper();
        propertyRepository = new UserPropertyRepository(namedParameterJdbcTemplate, objectMapper, queryBuilder);
    }

    @Test
    @DisplayName("findPropertiesByUserUuid should return empty list when userUuid is blank")
    void testFindPropertiesByUserUuid_BlankUserUuid() {
        List<UserProperty> results = propertyRepository.findPropertiesByUserUuid("   ", true);
        assertNotNull(results);
        assertTrue(results.isEmpty());

        results = propertyRepository.findPropertiesByUserUuid(null);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("saveRuralProperty binds property_id correctly to named parameters")
    void testSaveRuralProperty_IncludesPropertyId() {
        RuralPropertyDetail detail = RuralPropertyDetail.builder()
                .id("rec-uuid-1")
                .propertyRecordId("rec-uuid-1")
                .propertyId("PG-RP-2026-11-16-000001")
                .tenantId("pg.citya")
                .gramPanchayat("Rampur")
                .villageName("Fatehpur")
                .surveyNumber("KHASRA-102")
                .build();

        propertyRepository.saveRuralProperty(detail);

        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(namedParameterJdbcTemplate).update(eq(queryBuilder.getInsertRuralPropertyQuery()), paramsCaptor.capture());

        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals("rec-uuid-1", params.get("property_record_id"));
        assertEquals("PG-RP-2026-11-16-000001", params.get("property_id"));
        assertEquals("pg.citya", params.get("tenant_id"));
        assertEquals("Rampur", params.get("gram_panchayat"));
    }

    @Test
    @DisplayName("findRuralPropertyIdByPropertyId returns record ID when found")
    void testFindRuralPropertyIdByPropertyId_Found() {
        when(namedParameterJdbcTemplate.query(
                eq(UserPropertyQueryBuilder.SELECT_RURAL_PROPERTY_RECORD_ID_BY_PROPERTY_ID_SQL),
                anyMap(),
                any(RowMapper.class)
        )).thenReturn(Collections.singletonList("rec-uuid-101"));

        String result = propertyRepository.findRuralPropertyIdByPropertyId("user-uuid-1", "PG-RP-2026-11-16-000001");
        assertEquals("rec-uuid-101", result);
    }

    @Test
    @DisplayName("findRuralPropertyIdByPropertyId returns null when arguments are blank")
    void testFindRuralPropertyIdByPropertyId_BlankArgs() {
        assertNull(propertyRepository.findRuralPropertyIdByPropertyId(null, "PG-RP-001"));
        assertNull(propertyRepository.findRuralPropertyIdByPropertyId("user-1", "  "));
        assertNull(propertyRepository.findRuralPropertyIdByPropertyId("", null));
    }

    @Test
    @DisplayName("findActiveLinkedUsersForUrbanProperty fails closed throwing CustomException on DB error")
    void testFindActiveLinkedUsersForUrbanProperty_DbError_ThrowsException() {
        when(namedParameterJdbcTemplate.query(
                eq(UserPropertyQueryBuilder.SELECT_LINKED_USERS_BY_UPYOG_PROPERTY_ID_SQL),
                anyMap(),
                any(RowMapper.class)
        )).thenThrow(new RuntimeException("Database down"));

        org.egov.tracer.model.CustomException ex = org.junit.jupiter.api.Assertions.assertThrows(
                org.egov.tracer.model.CustomException.class,
                () -> propertyRepository.findActiveLinkedUsersForUrbanProperty("pg.citya", "PB-PT-001")
        );
        assertEquals(org.egov.user.domain.service.utils.UserConstants.ERROR_PROPERTY_SERVICE_ERROR, ex.getCode());
    }
}
