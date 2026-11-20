package org.egov.dx.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.dx.service.impl.MdmsTenantVerificationConfigServiceImpl;
import org.egov.dx.util.Configurations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MdmsTenantVerificationConfigServiceImplTest {

    private RestTemplate restTemplate;
    private Configurations configurations;
    private ObjectMapper objectMapper;
    private MdmsTenantVerificationConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        configurations = mock(Configurations.class);
        objectMapper = new ObjectMapper();

        when(configurations.getMdmsHost()).thenReturn("http://localhost:8088/");
        when(configurations.getMdmsSearchEndpoint()).thenReturn("egov-mdms-service/v1/_search");
        when(configurations.getMdmsVerificationModule()).thenReturn("common-masters");
        when(configurations.getMdmsVerificationMaster()).thenReturn("VerificationConfiguration");
        when(configurations.getMdmsVerificationCacheTtlSeconds()).thenReturn(300L);
        when(configurations.getMdmsGenderModule()).thenReturn("common-masters");
        when(configurations.getMdmsGenderMaster()).thenReturn("GenderType");

        service = new MdmsTenantVerificationConfigServiceImpl(restTemplate, configurations);
    }

    private JsonNode createMockMdmsResponse(String serviceType, boolean enabled) throws Exception {
        String json = "{\n" +
                "  \"MdmsRes\": {\n" +
                "    \"common-masters\": {\n" +
                "      \"VerificationConfiguration\": [\n" +
                "        {\n" +
                "          \"type\": \"" + serviceType + "\",\n" +
                "          \"enabled\": " + enabled + "\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}";

        return objectMapper.readTree(json);
    }

    /**
     * Tests Is Verification Enabled under the success_enabled in tenant scenario.
     *
     * Verifies that the check evaluates to false.
     */
    @Test
    void testIsVerificationEnabled_Success_EnabledInTenant() throws Exception {
        JsonNode mockRes = createMockMdmsResponse("TAN", true);
        when(restTemplate.postForObject(anyString(), any(), eq(JsonNode.class))).thenReturn(mockRes);

        boolean enabled = service.isVerificationEnabled(new RequestInfo(), "pg.citya", "TAN");
        assertTrue(enabled);
    }

    /**
     * Tests Is Verification Enabled under the success_state tenant scenario.
     *
     * Verifies that the check evaluates to false.
     */
    @Test
    void testIsVerificationEnabled_Success_StateTenant() throws Exception {
        JsonNode mockRes = createMockMdmsResponse("PAN", true);
        when(restTemplate.postForObject(anyString(), any(), eq(JsonNode.class))).thenReturn(mockRes);

        boolean enabled = service.isVerificationEnabled(new RequestInfo(), "pg", "PAN");
        assertTrue(enabled);
    }

    /**
     * Tests Is Verification Enabled under the disabled in config_returns false scenario.
     *
     * Verifies that the check evaluates to false.
     */
    @Test
    void testIsVerificationEnabled_DisabledInConfig_ReturnsFalse() throws Exception {
        JsonNode mockRes = createMockMdmsResponse("GST", false);
        when(restTemplate.postForObject(anyString(), any(), eq(JsonNode.class))).thenReturn(mockRes);

        boolean enabled = service.isVerificationEnabled(new RequestInfo(), "pg.citya", "GST");
        assertFalse(enabled);
    }

    /**
     * Tests Is Verification Enabled under the service not found_returns false scenario.
     *
     * Verifies that the check evaluates to false.
     */
    @Test
    void testIsVerificationEnabled_ServiceNotFound_ReturnsFalse() throws Exception {
        JsonNode mockRes = createMockMdmsResponse("TAN", true);
        when(restTemplate.postForObject(anyString(), any(), eq(JsonNode.class))).thenReturn(mockRes);

        boolean enabled = service.isVerificationEnabled(new RequestInfo(), "pg.citya", "PAN");
        assertFalse(enabled);
    }

    /**
     * Tests Is Verification Enabled under the blank tenant or service_returns false scenario.
     *
     * Verifies that the check evaluates to false.
     */
    @Test
    void testIsVerificationEnabled_BlankTenantOrService_ReturnsFalse() {
        assertFalse(service.isVerificationEnabled(new RequestInfo(), "", "TAN"));
        assertFalse(service.isVerificationEnabled(new RequestInfo(), "pg.citya", ""));
        assertFalse(service.isVerificationEnabled(new RequestInfo(), null, "TAN"));
    }

    /**
     * Tests Is Verification Enabled under the rest template exception_returns false scenario.
     *
     * Verifies that the check evaluates to false.
     */
    @Test
    void testIsVerificationEnabled_RestTemplateException_ReturnsFalse() {
        when(restTemplate.postForObject(anyString(), any(), eq(JsonNode.class)))
                .thenThrow(new RuntimeException("Connection refused to MDMS"));

        boolean enabled = service.isVerificationEnabled(new RequestInfo(), "pg.citya", "TAN");
        assertFalse(enabled);
    }

    /**
     * Tests Is Verification Enabled under the missing config_returns false scenario.
     *
     * Verifies that the check evaluates to false.
     */
    @Test
    void testIsVerificationEnabled_MissingConfig_ReturnsFalse() {
        when(configurations.getMdmsHost()).thenReturn(null);
        boolean enabled = service.isVerificationEnabled(new RequestInfo(), "pg.citya", "TAN");
        assertFalse(enabled);
    }

    /**
     * Tests Get Valid Genders under the success scenario.
     *
     * Verifies that the condition evaluates to true.
     */
    @Test
    void testGetValidGenders_Success() throws Exception {
        String json = """
                {
                  "MdmsRes": {
                    "common-masters": {
                      "GenderType": [
                        {"code": "MALE", "active": true},
                        {"code": "FEMALE", "active": true},
                        {"code": "TRANSGENDER", "active": true}
                      ]
                    }
                  }
                }""";
        JsonNode mockRes = objectMapper.readTree(json);
        when(restTemplate.postForObject(anyString(), any(), eq(JsonNode.class))).thenReturn(mockRes);

        Set<String> genders = service.getValidGenders(new RequestInfo(), "pg.citya");
        assertNotNull(genders);
        assertEquals(3, genders.size());
        assertTrue(genders.contains("MALE"));
        assertTrue(genders.contains("FEMALE"));
        assertTrue(genders.contains("TRANSGENDER"));
    }

    /**
     * Tests Get Valid Genders under the blank tenant_returns empty scenario.
     *
     * Verifies that the condition evaluates to true.
     */
    @Test
    void testGetValidGenders_BlankTenant_ReturnsEmpty() {
        Set<String> genders = service.getValidGenders(new RequestInfo(), "");
        assertTrue(genders.isEmpty());
    }

    /**
     * Tests Get Valid Genders under the rest template exception_returns empty scenario.
     *
     * Verifies that the condition evaluates to true.
     */
    @Test
    void testGetValidGenders_RestTemplateException_ReturnsEmpty() {
        when(restTemplate.postForObject(anyString(), any(), eq(JsonNode.class)))
                .thenThrow(new RuntimeException("Connection refused to MDMS"));

        Set<String> genders = service.getValidGenders(new RequestInfo(), "pg.citya");
        assertTrue(genders.isEmpty());
    }
}
