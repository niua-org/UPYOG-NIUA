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
import static org.mockito.Mockito.lenient;
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
        lenient().when(configuration.getMdmsHost()).thenReturn("https://upyog.niua.org");
        lenient().when(configuration.getMdmsSearchPath()).thenReturn("/egov-mdms-service/v1/_search");
        lenient().when(configuration.getMdmsFamilyModule()).thenReturn("common-masters");
        lenient().when(configuration.getMdmsRelationshipMaster()).thenReturn("FamilyRelationship");
        lenient().when(configuration.getMdmsRelationshipFilter()).thenReturn("[?(@.active==true)]");
        lenient().when(configuration.getRelationshipCacheTtl()).thenReturn(86400000L);
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
    @DisplayName("getAllowedRelationships should return empty set when MDMS call fails")
    void getAllowedRelationships_FallbackOnException() {
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenThrow(new RuntimeException("Connection timed out"));

        Set<String> result = masterDataService.getAllowedRelationships(new RequestInfo(), "pg.citya");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("validateCategory should succeed with MDMS returned category codes")
    void validateCategory_Mdms_Success() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> mdmsRes = new HashMap<>();
        Map<String, Object> commonMasters = new HashMap<>();
        List<Map<String, Object>> catList = new ArrayList<>();

        Map<String, Object> c1 = new HashMap<>();
        c1.put("code", "GENERAL");
        catList.add(c1);

        Map<String, Object> c2 = new HashMap<>();
        c2.put("code", "OBC");
        catList.add(c2);

        commonMasters.put("FamilyCategory", catList);
        mdmsRes.put("common-masters", commonMasters);
        response.put("MdmsRes", mdmsRes);

        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(response);

        assertDoesNotThrow(() -> masterDataService.validateCategory(new RequestInfo(), "pg.citya", "GENERAL"));
        assertDoesNotThrow(() -> masterDataService.validateCategory(new RequestInfo(), "pg.citya", "OBC"));

        CustomException ex = assertThrows(CustomException.class, () ->
                masterDataService.validateCategory(new RequestInfo(), "pg.citya", "INVALID_CAT"));
        assertEquals(FamilyConstants.ERROR_INVALID_CATEGORY, ex.getCode());
    }

    @Test
    @DisplayName("validateOccupation should succeed with MDMS returned master codes")
    void validateOccupation_Mdms_Success() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> mdmsRes = new HashMap<>();
        Map<String, Object> commonMasters = new HashMap<>();
        List<Map<String, Object>> occList = new ArrayList<>();

        Map<String, Object> o1 = new HashMap<>();
        o1.put("code", "SALARIED");
        occList.add(o1);

        Map<String, Object> o2 = new HashMap<>();
        o2.put("code", "BUSINESS");
        occList.add(o2);

        commonMasters.put("Occupation", occList);
        mdmsRes.put("common-masters", commonMasters);
        response.put("MdmsRes", mdmsRes);

        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(response);

        assertDoesNotThrow(() -> masterDataService.validateOccupation(new RequestInfo(), "pg.citya", "SALARIED"));
        assertDoesNotThrow(() -> masterDataService.validateOccupation(new RequestInfo(), "pg.citya", "BUSINESS"));

        CustomException ex = assertThrows(CustomException.class, () ->
                masterDataService.validateOccupation(new RequestInfo(), "pg.citya", "ASTRONAUT_INVALID"));
        assertEquals(FamilyConstants.ERROR_INVALID_REQUEST, ex.getCode());
    }

    @Test
    @DisplayName("validatePropertyType should succeed with MDMS returned master codes")
    void validatePropertyType_Mdms_Success() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> mdmsRes = new HashMap<>();
        Map<String, Object> ptMasters = new HashMap<>();
        List<Map<String, Object>> ptList = new ArrayList<>();

        Map<String, Object> p1 = new HashMap<>();
        p1.put("code", "RESIDENTIAL");
        ptList.add(p1);

        Map<String, Object> p2 = new HashMap<>();
        p2.put("code", "BUILTUP.SHAREDPROPERTY");
        ptList.add(p2);

        ptMasters.put("PropertyType", ptList);
        mdmsRes.put("PropertyTax", ptMasters);
        response.put("MdmsRes", mdmsRes);

        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(response);

        assertDoesNotThrow(() -> masterDataService.validatePropertyType(new RequestInfo(), "pg.citya", "RESIDENTIAL"));
        assertDoesNotThrow(() -> masterDataService.validatePropertyType(new RequestInfo(), "pg.citya", "BUILTUP.SHAREDPROPERTY"));

        CustomException ex = assertThrows(CustomException.class, () ->
                masterDataService.validatePropertyType(new RequestInfo(), "pg.citya", "SPACE_STATION_INVALID"));
        assertEquals(FamilyConstants.ERROR_INVALID_REQUEST, ex.getCode());
    }

    @Test
    @DisplayName("validatePropertyUsage should succeed with MDMS returned master codes")
    void validatePropertyUsage_Mdms_Success() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> mdmsRes = new HashMap<>();
        Map<String, Object> ptMasters = new HashMap<>();
        List<Map<String, Object>> usageList = new ArrayList<>();

        Map<String, Object> u1 = new HashMap<>();
        u1.put("code", "SELF_OCCUPIED");
        usageList.add(u1);

        Map<String, Object> u2 = new HashMap<>();
        u2.put("code", "RENTED");
        usageList.add(u2);

        ptMasters.put("UsageCategory", usageList);
        mdmsRes.put("PropertyTax", ptMasters);
        response.put("MdmsRes", mdmsRes);

        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(response);

        assertDoesNotThrow(() -> masterDataService.validatePropertyUsage(new RequestInfo(), "pg.citya", "SELF_OCCUPIED"));
        assertDoesNotThrow(() -> masterDataService.validatePropertyUsage(new RequestInfo(), "pg.citya", "RENTED"));

        CustomException ex = assertThrows(CustomException.class, () ->
                masterDataService.validatePropertyUsage(new RequestInfo(), "pg.citya", "UNKNOWN_USAGE"));
        assertEquals(FamilyConstants.ERROR_INVALID_REQUEST, ex.getCode());
    }
}
