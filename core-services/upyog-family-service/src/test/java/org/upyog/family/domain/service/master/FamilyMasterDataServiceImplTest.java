package org.upyog.family.domain.service.master;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.upyog.family.config.FamilyConfiguration;
import org.upyog.family.util.FamilyConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyMasterDataServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private FamilyConfiguration configuration;

    @InjectMocks
    private FamilyMasterDataServiceImpl masterDataService;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(configuration.getMdmsHost()).thenReturn("https://upyog.niua.org");
        org.mockito.Mockito.lenient().when(configuration.getMdmsSearchPath()).thenReturn("/egov-mdms-service/v1/_search");
        org.mockito.Mockito.lenient().when(configuration.getMdmsFamilyModule()).thenReturn("common-masters");
        org.mockito.Mockito.lenient().when(configuration.getMdmsRelationshipMaster()).thenReturn("FamilyRelationship");
        org.mockito.Mockito.lenient().when(configuration.getMdmsRelationshipFilter()).thenReturn("[?(@.active==true)]");
        org.mockito.Mockito.lenient().when(configuration.getRelationshipCacheTtl()).thenReturn(86400000L);
    }

    @Test
    @DisplayName("getAllowedRelationships should fetch from MDMS and cache results")
    void getAllowedRelationships_FromMdms_Success() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> mdmsRes = new HashMap<>();
        Map<String, Object> commonMasters = new HashMap<>();
        List<Map<String, Object>> relList = new ArrayList<>();

        Map<String, Object> r1 = new HashMap<>();
        r1.put("code", "HEAD");
        Map<String, Object> r2 = new HashMap<>();
        r2.put("code", "SPOUSE");
        Map<String, Object> r3 = new HashMap<>();
        r3.put("code", "ADOPTED_SON");

        relList.add(r1);
        relList.add(r2);
        relList.add(r3);

        commonMasters.put("FamilyRelationship", relList);
        mdmsRes.put("common-masters", commonMasters);
        response.put("MdmsRes", mdmsRes);

        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(response);

        Set<String> result = masterDataService.getAllowedRelationships(new RequestInfo(), "pg.citya");

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("HEAD"));
        assertTrue(result.contains("SPOUSE"));
        assertTrue(result.contains("ADOPTED_SON"));
    }

    @Test
    @DisplayName("validateRelationship should succeed for valid MDMS relationship")
    void validateRelationship_Success() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> mdmsRes = new HashMap<>();
        Map<String, Object> commonMasters = new HashMap<>();
        List<Map<String, Object>> relList = new ArrayList<>();

        Map<String, Object> r1 = new HashMap<>();
        r1.put("code", "HEAD");
        relList.add(r1);

        commonMasters.put("FamilyRelationship", relList);
        mdmsRes.put("common-masters", commonMasters);
        response.put("MdmsRes", mdmsRes);

        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(response);

        assertDoesNotThrow(() -> masterDataService.validateRelationship(new RequestInfo(), "pg.citya", "HEAD"));
    }

    @Test
    @DisplayName("validateRelationship should throw CustomException for unknown relationship")
    void validateRelationship_UnknownFails() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> mdmsRes = new HashMap<>();
        Map<String, Object> commonMasters = new HashMap<>();
        List<Map<String, Object>> relList = new ArrayList<>();

        Map<String, Object> r1 = new HashMap<>();
        r1.put("code", "HEAD");
        relList.add(r1);

        commonMasters.put("FamilyRelationship", relList);
        mdmsRes.put("common-masters", commonMasters);
        response.put("MdmsRes", mdmsRes);

        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(response);

        CustomException ex = assertThrows(CustomException.class, () ->
                masterDataService.validateRelationship(new RequestInfo(), "pg.citya", "NEIGHBOR"));
        assertEquals(FamilyConstants.ERROR_INVALID_RELATIONSHIP, ex.getCode());
    }

    @Test
    @DisplayName("getAllowedRelationships should fallback to default relationships when MDMS call fails")
    void getAllowedRelationships_FallbackOnException() {
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenThrow(new RuntimeException("Connection timed out"));

        Set<String> result = masterDataService.getAllowedRelationships(new RequestInfo(), "pg.citya");

        assertNotNull(result);
        assertTrue(result.contains("HEAD"));
        assertTrue(result.contains("SPOUSE"));
        assertTrue(result.contains("SON"));
    }
}
