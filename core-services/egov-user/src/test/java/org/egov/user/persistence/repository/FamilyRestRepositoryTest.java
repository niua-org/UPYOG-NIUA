package org.egov.user.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.config.UserConfiguration;
import org.egov.user.web.contract.profile.FamilyHofStatus;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
    @DisplayName("getActiveFamilyIdByUserUuid: Throws CustomException when host config is blank")
    void testGetActiveFamilyId_MissingHost_FailsFast() {
        when(userConfiguration.getFamilyServiceHost()).thenReturn(null);

        assertThrows(CustomException.class, () ->
                familyRestRepository.getActiveFamilyIdByUserUuid("user-uuid-1", "pg", requestInfo));
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
    @DisplayName("getActiveFamilyIdsByUserUuids: Returns empty map when list is empty, throws when host is missing")
    void testGetActiveFamilyIdsByUserUuids_EmptyOrMissingConfig() {
        Map<String, String> emptyListResult = familyRestRepository.getActiveFamilyIdsByUserUuids(Collections.emptyList(), "pg", requestInfo);
        assertTrue(emptyListResult.isEmpty());

        when(userConfiguration.getFamilyServiceHost()).thenReturn(null);
        assertThrows(CustomException.class, () ->
                familyRestRepository.getActiveFamilyIdsByUserUuids(Collections.singletonList("u1"), "pg", requestInfo));
        verify(restTemplate, never()).postForObject(any(String.class), any(), any());
    }

    @Test
    @DisplayName("getActiveFamilyIdByUserUuid: Skips REST call when family concept is disabled for tenant")
    void testGetActiveFamilyId_FamilyDisabled_SkipsRestCall() {
        org.egov.user.domain.service.config.FamilyConfigurationService configService =
                mock(org.egov.user.domain.service.config.FamilyConfigurationService.class);
        org.egov.user.web.contract.config.FamilyConfig disabledConfig = org.egov.user.web.contract.config.FamilyConfig.builder()
                .isFamilySupported(false)
                .familyType("NONE")
                .build();
        when(configService.getActiveFamilyConfiguration(any(), eq("ul"))).thenReturn(disabledConfig);

        FamilyRestRepository repoWithConfig = new FamilyRestRepository(restTemplate, userConfiguration, objectMapper, configService);
        String result = repoWithConfig.getActiveFamilyIdByUserUuid("u1", "ul", requestInfo);

        assertNull(result);
        verify(restTemplate, never()).postForObject(any(String.class), any(), any());
    }

    @Test
    @DisplayName("checkUserHofStatus: Returns isHof=true when user matches headUserUuid")
    void testCheckUserHofStatus_MatchesHeadUserUuid() {
        when(userConfiguration.getFamilyServiceHost()).thenReturn("http://upyog-family-service:8080");
        when(userConfiguration.getFamilyServiceSearchEndpoint()).thenReturn("/family/v1/family/_search");

        String jsonResponse = "{\"Families\":[{" +
                "\"familyId\":\"FAM-2026-001\"," +
                "\"status\":\"ACTIVE\"," +
                "\"headUserUuid\":\"user-hof-1\"," +
                "\"members\":[{\"userUuid\":\"user-hof-1\",\"isHead\":true,\"active\":true}]" +
                "}]}";

        when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(jsonResponse);

        FamilyHofStatus status = familyRestRepository.checkUserHofStatus("user-hof-1", "pg.citya", requestInfo);

        assertNotNull(status);
        assertTrue(status.isFamilyFound());
        assertTrue(status.isHof());
        assertEquals("FAM-2026-001", status.getFamilyId());
        assertEquals("user-hof-1", status.getHeadUserUuid());
    }

    @Test
    @DisplayName("checkUserHofStatus: Returns isHof=true when member has isHead=true")
    void testCheckUserHofStatus_MatchesMemberIsHead() {
        when(userConfiguration.getFamilyServiceHost()).thenReturn("http://upyog-family-service:8080");
        when(userConfiguration.getFamilyServiceSearchEndpoint()).thenReturn("/family/v1/family/_search");

        String jsonResponse = "{\"Families\":[{" +
                "\"familyId\":\"FAM-2026-001\"," +
                "\"status\":\"ACTIVE\"," +
                "\"headUserUuid\":\"other-head\"," +
                "\"members\":[{\"userUuid\":\"user-hof-2\",\"isHead\":true,\"active\":true}]" +
                "}]}";

        when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(jsonResponse);

        FamilyHofStatus status = familyRestRepository.checkUserHofStatus("user-hof-2", "pg.citya", requestInfo);

        assertNotNull(status);
        assertTrue(status.isFamilyFound());
        assertTrue(status.isHof());
        assertEquals("FAM-2026-001", status.getFamilyId());
    }

    @Test
    @DisplayName("checkUserHofStatus: Returns isHof=false when member is not head")
    void testCheckUserHofStatus_NonHeadMember() {
        when(userConfiguration.getFamilyServiceHost()).thenReturn("http://upyog-family-service:8080");
        when(userConfiguration.getFamilyServiceSearchEndpoint()).thenReturn("/family/v1/family/_search");

        String jsonResponse = "{\"Families\":[{" +
                "\"familyId\":\"FAM-2026-001\"," +
                "\"status\":\"ACTIVE\"," +
                "\"headUserUuid\":\"other-head\"," +
                "\"members\":[{\"userUuid\":\"user-member-1\",\"isHead\":false,\"active\":true}]" +
                "}]}";

        when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(jsonResponse);

        FamilyHofStatus status = familyRestRepository.checkUserHofStatus("user-member-1", "pg.citya", requestInfo);

        assertNotNull(status);
        assertTrue(status.isFamilyFound());
        assertFalse(status.isHof());
        assertEquals("FAM-2026-001", status.getFamilyId());
    }

    @Test
    @DisplayName("checkUserHofStatus: Returns familyFound=false when no family returned")
    void testCheckUserHofStatus_NoFamily() {
        when(userConfiguration.getFamilyServiceHost()).thenReturn("http://upyog-family-service:8080");
        when(userConfiguration.getFamilyServiceSearchEndpoint()).thenReturn("/family/v1/family/_search");

        when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn("{\"Families\":[]}");

        FamilyHofStatus status = familyRestRepository.checkUserHofStatus("user-isolated", "pg.citya", requestInfo);

        assertNotNull(status);
        assertFalse(status.isFamilyFound());
        assertFalse(status.isHof());
    }

    @Test
    @DisplayName("getFamilyByFamilyId: Successfully resolves family with members by familyId")
    void testGetFamilyByFamilyId_Success() {
        when(userConfiguration.getFamilyServiceHost()).thenReturn("http://upyog-family-service:8080");
        when(userConfiguration.getFamilyServiceSearchEndpoint()).thenReturn("/family/v1/family/_search");

        String jsonResponse = "{\"Families\":[{" +
                "\"id\":\"fam-uuid-1\"," +
                "\"familyId\":\"FAM-2026-001\"," +
                "\"tenantId\":\"pg.citya\"," +
                "\"headUserUuid\":\"user-head-1\"," +
                "\"headName\":\"Rajesh Kumar\"," +
                "\"status\":\"ACTIVE\"," +
                "\"category\":\"GENERAL\"," +
                "\"members\":[" +
                "  {\"id\":\"mem-1\",\"userUuid\":\"user-head-1\",\"relationshipWithHead\":\"SELF\",\"isHead\":true,\"active\":true}," +
                "  {\"id\":\"mem-2\",\"userUuid\":\"user-member-2\",\"relationshipWithHead\":\"SPOUSE\",\"isHead\":false,\"active\":true}" +
                "]" +
                "}]}";

        when(restTemplate.postForObject(contains("familyId=FAM-2026-001"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(jsonResponse);

        org.egov.user.web.contract.family.FamilyDetail family = familyRestRepository.getFamilyByFamilyId("FAM-2026-001", "pg.citya", requestInfo);

        assertNotNull(family);
        assertEquals("FAM-2026-001", family.getFamilyId());
        assertEquals("user-head-1", family.getHeadUserUuid());
        assertEquals(2, family.getMembers().size());
        assertEquals("user-head-1", family.getMembers().get(0).getUserUuid());
        assertTrue(family.getMembers().get(0).getIsHead());
        assertEquals("user-member-2", family.getMembers().get(1).getUserUuid());
    }

    @Test
    @DisplayName("getFamilyByFamilyId: Returns null when family not found")
    void testGetFamilyByFamilyId_NotFound() {
        when(userConfiguration.getFamilyServiceHost()).thenReturn("http://upyog-family-service:8080");
        when(userConfiguration.getFamilyServiceSearchEndpoint()).thenReturn("/family/v1/family/_search");

        when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn("{\"Families\":[]}");

        org.egov.user.web.contract.family.FamilyDetail family = familyRestRepository.getFamilyByFamilyId("FAM-NON-EXISTENT", "pg.citya", requestInfo);

        assertNull(family);
    }

    @Test
    @DisplayName("getFamilyByFamilyId: Returns null when familyId is blank")
    void testGetFamilyByFamilyId_BlankId() {
        org.egov.user.web.contract.family.FamilyDetail family = familyRestRepository.getFamilyByFamilyId("   ", "pg.citya", requestInfo);
        assertNull(family);
    }

    @Test
    @DisplayName("getFamilyByFamilyId: Throws CustomException when RestTemplate fails")
    void testGetFamilyByFamilyId_Exception() {
        when(userConfiguration.getFamilyServiceHost()).thenReturn("http://upyog-family-service:8080");
        when(userConfiguration.getFamilyServiceSearchEndpoint()).thenReturn("/family/v1/family/_search");

        when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        assertThrows(CustomException.class, () ->
                familyRestRepository.getFamilyByFamilyId("FAM-2026-001", "pg.citya", requestInfo));
    }
}
