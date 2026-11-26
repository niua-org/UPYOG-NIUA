package org.egov.user.domain.service.config.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.config.UserConfiguration;
import org.egov.user.web.contract.config.FamilyConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MdmsFamilyConfigurationServiceImplTest {

    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;
    private UserConfiguration userConfiguration;
    private MdmsFamilyConfigurationServiceImpl service;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        objectMapper = new ObjectMapper();
        userConfiguration = UserConfiguration.builder()
                .mdmsHost("http://localhost:8080")
                .mdmsPath("/egov-mdms-service/v1/_search")
                .mdmsFamilyModule("common-masters")
                .mdmsFamilyConfigurationMaster("FamilyConfiguration")
                .mdmsFamilyConfigurationFilter("[?(@.active==true)]")
                .familyConfigurationCacheTtl(86400000L)
                .stateLevelTenantId("pg")
                .build();

        service = new MdmsFamilyConfigurationServiceImpl(restTemplate, userConfiguration, objectMapper);
    }

    @Test
    @DisplayName("Should fetch STATE_REGISTRY (PPP) config from MDMS and cache it in egov-user")
    void testGetActiveFamilyConfiguration_StateRegistry_FromMdms() throws Exception {
        String jsonResponse = "{\n"
                + "  \"MdmsRes\": {\n"
                + "    \"common-masters\": {\n"
                + "      \"FamilyConfiguration\": [\n"
                + "        {\n"
                + "          \"code\": \"STATE_REGISTRY\",\n"
                + "          \"name\": \"Haryana PPP\",\n"
                + "          \"isFamilySupported\": true,\n"
                + "          \"familyType\": \"STATE_REGISTRY\",\n"
                + "          \"active\": true\n"
                + "        }\n"
                + "      ]\n"
                + "    }\n"
                + "  }\n"
                + "}";
        JsonNode node = objectMapper.readTree(jsonResponse);
        when(restTemplate.postForObject(anyString(), any(), eq(JsonNode.class))).thenReturn(node);

        FamilyConfig config = service.getActiveFamilyConfiguration(new RequestInfo(), "hr");

        assertNotNull(config);
        assertTrue(config.isFamilySupported());
        assertTrue(config.isStateRegistry());
        assertFalse(config.isUpyogInternal());
        assertEquals("STATE_REGISTRY", config.getFamilyType());

        // Subsequent call should hit cache and not restTemplate
        FamilyConfig cached = service.getActiveFamilyConfiguration(new RequestInfo(), "hr");
        assertEquals(config.getFamilyType(), cached.getFamilyType());
        verify(restTemplate, times(1)).postForObject(anyString(), any(), eq(JsonNode.class));
    }

    @Test
    @DisplayName("Should fallback to default UPYOG_INTERNAL config when MDMS returns empty in egov-user")
    void testGetActiveFamilyConfiguration_FallbackDefault() {
        when(restTemplate.postForObject(anyString(), any(), eq(JsonNode.class))).thenReturn(null);

        FamilyConfig config = service.getActiveFamilyConfiguration(new RequestInfo(), "pg");

        assertNotNull(config);
        assertTrue(config.isFamilySupported());
        assertTrue(config.isUpyogInternal());
        assertEquals("UPYOG_INTERNAL", config.getFamilyType());
    }

    @Test
    @DisplayName("Should allow family operations when family is supported")
    void testValidateFamilySupported_Success() {
        when(restTemplate.postForObject(anyString(), any(), eq(JsonNode.class))).thenReturn(null);

        assertDoesNotThrow(() -> service.validateFamilySupported(new RequestInfo(), "pg"));
    }

    @Test
    @DisplayName("Should reject operations when isFamilySupported is false")
    void testValidateFamilySupported_None_ThrowsException() throws Exception {
        String jsonResponse = "{\n"
                + "  \"MdmsRes\": {\n"
                + "    \"common-masters\": {\n"
                + "      \"FamilyConfiguration\": [\n"
                + "        {\n"
                + "          \"code\": \"NONE\",\n"
                + "          \"isFamilySupported\": false,\n"
                + "          \"familyType\": \"NONE\",\n"
                + "          \"active\": true\n"
                + "        }\n"
                + "      ]\n"
                + "    }\n"
                + "  }\n"
                + "}";
        JsonNode node = objectMapper.readTree(jsonResponse);
        when(restTemplate.postForObject(anyString(), any(), eq(JsonNode.class))).thenReturn(node);

        CustomException ex = assertThrows(CustomException.class, () ->
                service.validateFamilySupported(new RequestInfo(), "ul"));
        assertEquals("FAMILY_NOT_SUPPORTED", ex.getCode());
    }
}
