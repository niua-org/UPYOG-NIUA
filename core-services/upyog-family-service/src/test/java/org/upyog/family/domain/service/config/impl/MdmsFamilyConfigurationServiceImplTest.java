package org.upyog.family.domain.service.config.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.upyog.family.config.FamilyConfiguration;
import org.upyog.family.web.contract.config.FamilyConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MdmsFamilyConfigurationServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private FamilyConfiguration configuration;

    private ObjectMapper objectMapper;
    private MdmsFamilyConfigurationServiceImpl service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        org.mockito.Mockito.lenient().when(configuration.getMdmsHost()).thenReturn("http://localhost:8080");
        org.mockito.Mockito.lenient().when(configuration.getMdmsSearchPath()).thenReturn("/egov-mdms-service/v1/_search");
        org.mockito.Mockito.lenient().when(configuration.getMdmsFamilyModule()).thenReturn("common-masters");
        org.mockito.Mockito.lenient().when(configuration.getMdmsConfigurationMaster()).thenReturn("FamilyConfiguration");
        org.mockito.Mockito.lenient().when(configuration.getMdmsConfigurationFilter()).thenReturn("[?(@.active==true)]");
        org.mockito.Mockito.lenient().when(configuration.getConfigurationCacheTtl()).thenReturn(86400000L);

        service = new MdmsFamilyConfigurationServiceImpl(restTemplate, configuration, objectMapper);
    }

    @Test
    @DisplayName("Should fetch and parse STATE_REGISTRY (PPP) config from MDMS and cache it")
    void testGetActiveFamilyConfiguration_StateRegistry_FromMdms() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> mdmsRes = new HashMap<>();
        Map<String, Object> commonMasters = new HashMap<>();
        List<Map<String, Object>> configList = new ArrayList<>();

        Map<String, Object> pppConfig = new HashMap<>();
        pppConfig.put("code", "STATE_REGISTRY");
        pppConfig.put("name", "Haryana PPP");
        pppConfig.put("isFamilySupported", true);
        pppConfig.put("familyType", "STATE_REGISTRY");
        pppConfig.put("active", true);

        configList.add(pppConfig);
        commonMasters.put("FamilyConfiguration", configList);
        mdmsRes.put("common-masters", commonMasters);
        response.put("MdmsRes", mdmsRes);

        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(response);

        FamilyConfig config = service.getActiveFamilyConfiguration(new RequestInfo(), "hr");

        assertNotNull(config);
        assertTrue(config.isFamilySupported());
        assertTrue(config.isStateRegistry());
        assertFalse(config.isUpyogInternal());
        assertEquals("STATE_REGISTRY", config.getFamilyType());

        // Subsequent call should hit cache and not restTemplate
        FamilyConfig cached = service.getActiveFamilyConfiguration(new RequestInfo(), "hr");
        assertEquals(config.getFamilyType(), cached.getFamilyType());
        verify(restTemplate, times(1)).postForObject(anyString(), any(), eq(Map.class));
    }

    @Test
    @DisplayName("Should fallback to default UPYOG_INTERNAL config when MDMS returns empty")
    void testGetActiveFamilyConfiguration_FallbackDefault() {
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(null);

        FamilyConfig config = service.getActiveFamilyConfiguration(new RequestInfo(), "pg");

        assertNotNull(config);
        assertTrue(config.isFamilySupported());
        assertTrue(config.isUpyogInternal());
        assertEquals("UPYOG_INTERNAL", config.getFamilyType());
    }

    @Test
    @DisplayName("Should allow UPYOG_INTERNAL actions and reject STATE_SYNC when tenant is UPYOG_INTERNAL")
    void testValidateApiAccess_UpyogInternal() {
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(null);

        // Native lifecycle operations should pass
        assertDoesNotThrow(() -> service.validateUpyogNativeFamily(new RequestInfo(), "pg"));
        assertDoesNotThrow(() -> service.validateFamilySupported(new RequestInfo(), "pg"));

        // State sync operations should be blocked
        CustomException ex = assertThrows(CustomException.class, () ->
                service.validateStateRegistryFamily(new RequestInfo(), "pg"));
        assertEquals("OPERATION_NOT_ALLOWED", ex.getCode());
    }

    @Test
    @DisplayName("Should allow STATE_SYNC and reject CREATE/SPLIT when tenant is STATE_REGISTRY (PPP)")
    void testValidateApiAccess_StateRegistry() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> mdmsRes = new HashMap<>();
        Map<String, Object> commonMasters = new HashMap<>();
        List<Map<String, Object>> configList = new ArrayList<>();

        Map<String, Object> pppConfig = new HashMap<>();
        pppConfig.put("code", "STATE_REGISTRY");
        pppConfig.put("isFamilySupported", true);
        pppConfig.put("familyType", "STATE_REGISTRY");
        pppConfig.put("active", true);

        configList.add(pppConfig);
        commonMasters.put("FamilyConfiguration", configList);
        mdmsRes.put("common-masters", commonMasters);
        response.put("MdmsRes", mdmsRes);

        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(response);

        // State wrapper actions should be allowed
        assertDoesNotThrow(() -> service.validateStateRegistryFamily(new RequestInfo(), "hr"));
        assertDoesNotThrow(() -> service.validateFamilySupported(new RequestInfo(), "hr"));

        // Native lifecycle actions should be blocked
        CustomException ex = assertThrows(CustomException.class, () ->
                service.validateUpyogNativeFamily(new RequestInfo(), "hr"));
        assertEquals("OPERATION_NOT_ALLOWED", ex.getCode());
    }

    @Test
    @DisplayName("Should block all operations when isFamilySupported is false (NONE)")
    void testValidateApiAccess_None() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> mdmsRes = new HashMap<>();
        Map<String, Object> commonMasters = new HashMap<>();
        List<Map<String, Object>> configList = new ArrayList<>();

        Map<String, Object> noneConfig = new HashMap<>();
        noneConfig.put("code", "NONE");
        noneConfig.put("isFamilySupported", false);
        noneConfig.put("familyType", "NONE");
        noneConfig.put("active", true);

        configList.add(noneConfig);
        commonMasters.put("FamilyConfiguration", configList);
        mdmsRes.put("common-masters", commonMasters);
        response.put("MdmsRes", mdmsRes);

        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(response);

        CustomException ex1 = assertThrows(CustomException.class, () ->
                service.validateFamilySupported(new RequestInfo(), "ul"));
        assertEquals("FAMILY_NOT_SUPPORTED", ex1.getCode());

        CustomException ex2 = assertThrows(CustomException.class, () ->
                service.validateUpyogNativeFamily(new RequestInfo(), "ul"));
        assertEquals("FAMILY_NOT_SUPPORTED", ex2.getCode());

        CustomException ex3 = assertThrows(CustomException.class, () ->
                service.validateStateRegistryFamily(new RequestInfo(), "ul"));
        assertEquals("FAMILY_NOT_SUPPORTED", ex3.getCode());
    }
}
