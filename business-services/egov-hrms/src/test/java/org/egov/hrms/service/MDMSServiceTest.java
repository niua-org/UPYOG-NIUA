package org.egov.hrms.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.hrms.utils.HRMSConstants;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.mdms.model.MdmsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MDMSServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private MDMSService mdmsService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mdmsService, "mdmsHost", "http://mdms/");
        ReflectionTestUtils.setField(mdmsService, "mdmsEndpoint", "v1/_search");
    }

    @Test
    void prepareMDMSRequest_buildsCriteriaWithModules() {
        StringBuilder uri = new StringBuilder();
        MdmsCriteriaReq req = mdmsService.prepareMDMSRequest(uri, new RequestInfo(), "pb.amritsar");

        assertThat(uri.toString()).isEqualTo("http://mdms/v1/_search");
        assertThat(req.getMdmsCriteria().getTenantId()).isEqualTo("pb.amritsar");
        assertThat(req.getMdmsCriteria().getModuleDetails()).isNotEmpty();
    }

    @Test
    void prepareMDMSRequestLoc_buildsLocationCriteria() {
        StringBuilder uri = new StringBuilder();
        MdmsCriteriaReq req = mdmsService.prepareMDMSRequestLoc(uri, new RequestInfo(), "pb");

        assertThat(uri.toString()).contains("http://mdms/");
        assertThat(req.getMdmsCriteria().getModuleDetails()).isNotEmpty();
    }

    @Test
    void fetchMDMSData_returnsResponseFromRestTemplate() {
        MdmsResponse mdmsResponse = new MdmsResponse();
        when(restTemplate.postForObject(any(String.class), any(), eq(MdmsResponse.class))).thenReturn(mdmsResponse);

        assertThat(mdmsService.fetchMDMSData(new RequestInfo(), "pb")).isSameAs(mdmsResponse);
    }

    @Test
    void fetchMDMSData_onException_returnsNull() {
        when(restTemplate.postForObject(any(String.class), any(), eq(MdmsResponse.class)))
                .thenThrow(new RuntimeException("fail"));

        assertThat(mdmsService.fetchMDMSData(new RequestInfo(), "pb")).isNull();
    }

    @Test
    void fetchMDMSDataLoc_returnsResponseFromRestTemplate() {
        MdmsResponse mdmsResponse = new MdmsResponse();
        when(restTemplate.postForObject(any(String.class), any(), eq(MdmsResponse.class))).thenReturn(mdmsResponse);

        assertThat(mdmsService.fetchMDMSDataLoc(new RequestInfo(), "pb")).isSameAs(mdmsResponse);
    }

    @Test
    void getMDMSData_whenResponseNull_returnsEmptyMap() {
        when(restTemplate.postForObject(any(String.class), any(), eq(MdmsResponse.class))).thenReturn(null);

        Map<String, List<String>> data = mdmsService.getMDMSData(new RequestInfo(), "pb");

        assertThat(data).isEmpty();
    }
}
