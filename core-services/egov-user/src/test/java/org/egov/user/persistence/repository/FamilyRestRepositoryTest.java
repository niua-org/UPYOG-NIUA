package org.egov.user.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.user.config.UserConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Persistence repository for family rest repository test.
 *
 * Handles database interactions, entity mapping, and persistence operations.
 */
@ExtendWith(MockitoExtension.class)
class FamilyRestRepositoryTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private UserConfiguration userConfiguration;

    private ObjectMapper objectMapper = new ObjectMapper();

    private FamilyRestRepository familyRestRepository;

    private RequestInfo requestInfo;

    @BeforeEach
    void setUp() {
        familyRestRepository = new FamilyRestRepository(restTemplate, userConfiguration, objectMapper);
        requestInfo = RequestInfo.builder().apiId("rainmaker").build();
    }

    /**
     * Tests the functionality of testgetactivefamilyid success.
     */
    @Test
    @DisplayName("getActiveFamilyIdByUserUuid: Successfully resolves active familyId using status=ACTIVE")
    void testGetActiveFamilyId_Success() {
        when(userConfiguration.getFamilyServiceHost()).thenReturn("http://upyog-family-service.egov:8080");
        when(userConfiguration.getFamilyServiceSearchEndpoint()).thenReturn("/family/v1/family/_search");

        String jsonResponse = "{"
                + "\"Families\": ["
                + "  {\"familyId\": \"FAM-2026-OLD\", \"status\": \"EXITED\"},"
                + "  {\"familyId\": \"FAM-2026-ACTIVE\", \"status\": \"ACTIVE\"}"
                + "]"
                + "}";

        when(restTemplate.postForObject(contains("status=ACTIVE"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(jsonResponse);

        String result = familyRestRepository.getActiveFamilyIdByUserUuid("user-uuid-1", "pg", requestInfo);

        assertEquals("FAM-2026-ACTIVE", result);
        verify(restTemplate).postForObject(contains("memberUserUuid=user-uuid-1"), any(HttpEntity.class), eq(String.class));
    }

    /**
     * Tests the functionality of testgetactivefamilyid missinghost failsfast.
     */
    @Test
    @DisplayName("getActiveFamilyIdByUserUuid: Returns null when host config is blank (fails fast without localhost)")
    void testGetActiveFamilyId_MissingHost_FailsFast() {
        when(userConfiguration.getFamilyServiceHost()).thenReturn(null);

        String result = familyRestRepository.getActiveFamilyIdByUserUuid("user-uuid-1", "pg", requestInfo);

        assertNull(result);
        verify(restTemplate, never()).postForObject(any(String.class), any(), any());
    }

    /**
     * Tests the functionality of testgetactivefamilyidsbyuseruuids bulkbatchsuccess.
     */
    @Test
    @DisplayName("getActiveFamilyIdsByUserUuids: Performs single bulk batch request and maps active member UUIDs")
    void testGetActiveFamilyIdsByUserUuids_BulkBatchSuccess() {
        when(userConfiguration.getFamilyServiceHost()).thenReturn("http://upyog-family-service.egov:8080");
        when(userConfiguration.getFamilyServiceSearchEndpoint()).thenReturn("/family/v1/family/_search");

        String jsonResponse = "{"
                + "\"Families\": ["
                + "  {"
                + "    \"familyId\": \"FAM-2026-001\","
                + "    \"status\": \"ACTIVE\","
                + "    \"members\": ["
                + "      {\"userUuid\": \"user-1\", \"active\": true},"
                + "      {\"userUuid\": \"user-2\", \"active\": true}"
                + "    ]"
                + "  },"
                + "  {"
                + "    \"familyId\": \"FAM-2026-002\","
                + "    \"status\": \"ACTIVE\","
                + "    \"members\": ["
                + "      {\"userUuid\": \"user-3\", \"active\": true}"
                + "    ]"
                + "  }"
                + "]"
                + "}";

        when(restTemplate.postForObject(contains("memberUserUuids="), any(HttpEntity.class), eq(String.class)))
                .thenReturn(jsonResponse);

        List<String> userUuids = Arrays.asList("user-1", "user-2", "user-3");
        Map<String, String> resultMap = familyRestRepository.getActiveFamilyIdsByUserUuids(userUuids, "pg", requestInfo);

        assertNotNull(resultMap);
        assertEquals(3, resultMap.size());
        assertEquals("FAM-2026-001", resultMap.get("user-1"));
        assertEquals("FAM-2026-001", resultMap.get("user-2"));
        assertEquals("FAM-2026-002", resultMap.get("user-3"));

        // Single batch HTTP request executed
        verify(restTemplate).postForObject(any(String.class), any(HttpEntity.class), eq(String.class));
    }

    /**
     * Tests the functionality of testgetactivefamilyidsbyuseruuids emptyormissingconfig.
     */
    @Test
    @DisplayName("getActiveFamilyIdsByUserUuids: Returns empty map when list is empty or host is missing")
    void testGetActiveFamilyIdsByUserUuids_EmptyOrMissingConfig() {
        Map<String, String> emptyListResult = familyRestRepository.getActiveFamilyIdsByUserUuids(Collections.emptyList(), "pg", requestInfo);
        assertTrue(emptyListResult.isEmpty());

        when(userConfiguration.getFamilyServiceHost()).thenReturn(null);
        Map<String, String> missingHostResult = familyRestRepository.getActiveFamilyIdsByUserUuids(Collections.singletonList("u1"), "pg", requestInfo);
        assertTrue(missingHostResult.isEmpty());
        verify(restTemplate, never()).postForObject(any(String.class), any(), any());
    }
}
