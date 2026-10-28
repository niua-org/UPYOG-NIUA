package org.egov.ndc.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.ndc.config.NDCConfiguration;
import org.egov.ndc.repository.ServiceRequestRepository;
import org.egov.ndc.web.model.idgen.IdGenerationResponse;
import org.egov.ndc.web.model.idgen.IdResponse;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NDCUtilTest {

    @Mock
    private NDCConfiguration config;
    @Mock
    private ObjectMapper mapper;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    private NDCUtil ndcUtil;

    @BeforeEach
    void setUp() {
        ndcUtil = new NDCUtil(config, mapper, serviceRequestRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(ndcUtil, "idGenHost", "http://idgen");
        org.springframework.test.util.ReflectionTestUtils.setField(ndcUtil, "idGenPath", "/id/_generate");
    }

    @Test
    void getMdmsSearchUrl_concatenatesHostAndEndpoint() {
        when(config.getMdmsHost()).thenReturn("http://mdms");
        when(config.getMdmsEndPoint()).thenReturn("/mdms-v2/_search");

        assertThat(ndcUtil.getMdmsSearchUrl().toString()).isEqualTo("http://mdms/mdms-v2/_search");
    }

    @Test
    void getMDMSRequest_buildsCriteriaWithTenantAndModules() {
        RequestInfo requestInfo = RequestInfo.builder().apiId("ndc").build();

        MdmsCriteriaReq req = ndcUtil.getMDMSRequest(requestInfo, "pb.amritsar");

        assertThat(req.getRequestInfo()).isSameAs(requestInfo);
        assertThat(req.getMdmsCriteria().getTenantId()).isEqualTo("pb.amritsar");
        assertThat(req.getMdmsCriteria().getModuleDetails()).hasSize(2);
    }

    @Test
    void getNDCModuleRequest_includesNdcAndCommonMasters() {
        assertThat(ndcUtil.getNDCModuleRequest())
                .extracting(module -> module.getModuleName())
                .containsExactly(NDCConstants.NDC_MODULE, NDCConstants.COMMON_MASTERS_MODULE);
    }

    @Test
    void getIdList_whenIdgenReturnsIds_returnsIdStrings() {
        RequestInfo requestInfo = RequestInfo.builder().build();
        IdGenerationResponse response = IdGenerationResponse.builder()
                .idResponses(List.of(IdResponse.builder().id("NDC-001").build()))
                .build();
        when(serviceRequestRepository.fetchResult(any(), any())).thenReturn(Map.of());
        when(mapper.convertValue(any(), eq(IdGenerationResponse.class))).thenReturn(response);

        List<String> ids = ndcUtil.getIdList(requestInfo, "pb.amritsar", "ndc.app", "NDC-[cy:yyyy]", 1);

        assertThat(ids).containsExactly("NDC-001");
    }

    @Test
    void getIdList_whenIdgenEmpty_throwsCustomException() {
        RequestInfo requestInfo = RequestInfo.builder().build();
        IdGenerationResponse response = IdGenerationResponse.builder().idResponses(List.of()).build();
        when(serviceRequestRepository.fetchResult(any(), any())).thenReturn(Map.of());
        when(mapper.convertValue(any(), eq(IdGenerationResponse.class))).thenReturn(response);

        assertThatThrownBy(() -> ndcUtil.getIdList(requestInfo, "pb", "ndc.app", "fmt", 1))
                .isInstanceOf(CustomException.class);
    }
}
