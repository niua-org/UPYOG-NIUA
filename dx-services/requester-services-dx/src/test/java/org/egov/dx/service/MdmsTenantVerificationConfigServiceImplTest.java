package org.egov.dx.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.dx.service.impl.MdmsTenantVerificationConfigServiceImpl;
import org.egov.dx.util.Configurations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

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

    @Test
    void testIsVerificationEnabled_Success_EnabledInTenant() throws Exception {
        JsonNode mockRes = createMockMdmsResponse("TAN", true);
        when(restTemplate.postForObject(anyString(), any(), eq(JsonNode.class))).thenReturn(mockRes);

        boolean enabled = service.isVerificationEnabled(new RequestInfo(), "pg.citya", "TAN");
        assertTrue(enabled);
    }

    @Test
    void testIsVerificationEnabled_Success_StateTenant() throws Exception {
        JsonNode mockRes = createMockMdmsResponse("PAN", true);
        when(restTemplate.postForObject(anyString(), any(), eq(JsonNode.class))).thenReturn(mockRes);

        boolean enabled = service.isVerificationEnabled(new RequestInfo(), "pg", "PAN");
        assertTrue(enabled);
    }

    @Test
    void testIsVerificationEnabled_DisabledInConfig_ReturnsFalse() throws Exception {
        JsonNode mockRes = createMockMdmsResponse("GST", false);
        when(restTemplate.postForObject(anyString(), any(), eq(JsonNode.class))).thenReturn(mockRes);

        boolean enabled = service.isVerificationEnabled(new RequestInfo(), "pg.citya", "GST");
        assertFalse(enabled);
    }

    @Test
    void testIsVerificationEnabled_ServiceNotFound_ReturnsFalse() throws Exception {
        JsonNode mockRes = createMockMdmsResponse("TAN", true);
        when(restTemplate.postForObject(anyString(), any(), eq(JsonNode.class))).thenReturn(mockRes);

        boolean enabled = service.isVerificationEnabled(new RequestInfo(), "pg.citya", "PAN");
        assertFalse(enabled);
    }

    @Test
    void testIsVerificationEnabled_BlankTenantOrService_ReturnsFalse() {
        assertFalse(service.isVerificationEnabled(new RequestInfo(), "", "TAN"));
        assertFalse(service.isVerificationEnabled(new RequestInfo(), "pg.citya", ""));
        assertFalse(service.isVerificationEnabled(new RequestInfo(), null, "TAN"));
    }

    @Test
    void testIsVerificationEnabled_RestTemplateException_ReturnsFalse() {
        when(restTemplate.postForObject(anyString(), any(), eq(JsonNode.class)))
                .thenThrow(new RuntimeException("Connection refused to MDMS"));

        boolean enabled = service.isVerificationEnabled(new RequestInfo(), "pg.citya", "TAN");
        assertFalse(enabled);
    }

    @Test
    void testIsVerificationEnabled_MissingConfig_ReturnsFalse() {
        when(configurations.getMdmsHost()).thenReturn(null);
        boolean enabled = service.isVerificationEnabled(new RequestInfo(), "pg.citya", "TAN");
        assertFalse(enabled);
    }
}
