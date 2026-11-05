package org.egov.wscalculation.util;

import org.egov.common.contract.request.RequestInfo;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.tracer.model.CustomException;
import org.egov.wscalculation.config.WSCalculationConfiguration;
import org.egov.wscalculation.web.models.MeterConnectionRequest;
import org.egov.wscalculation.web.models.MeterReading;
import org.egov.wscalculation.web.models.MeterReadingResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeterReadingUtilTest {

    @Mock
    private WSCalculationConfiguration config;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper mapper;

    @InjectMocks
    private MeterReadingUtil meterReadingUtil;

    @Test
    void getMeterReadingRequest_wrapsReadingAndRequestInfo() {
        RequestInfo requestInfo = RequestInfo.builder().build();
        MeterReading reading = MeterReading.builder().connectionNo("WS-1").build();

        MeterConnectionRequest request = meterReadingUtil.getMeterReadingRequest(requestInfo, reading);

        assertThat(request.getRequestInfo()).isSameAs(requestInfo);
        assertThat(request.getMeterReading()).isSameAs(reading);
    }

    @Test
    void getDemandGenerationCreateURL_usesBillingHostAndEndpoint() {
        when(config.getBillingServiceHost()).thenReturn("http://billing/");
        when(config.getDemandCreateEndPoint()).thenReturn("demand/_create");

        assertThat(meterReadingUtil.getDemandGenerationCreateURL().toString())
                .isEqualTo("http://billing/demand/_create");
    }

    @Test
    void prepareMdMsRequest_buildsModuleAndMasterDetails() {
        RequestInfo requestInfo = RequestInfo.builder().build();
        MdmsCriteriaReq criteriaReq = meterReadingUtil.prepareMdMsRequest(
                "pb", "ws-services", List.of("BillingPeriod"), "[?(@.active==true)]", requestInfo);

        assertThat(criteriaReq.getMdmsCriteria().getTenantId()).isEqualTo("pb");
        assertThat(criteriaReq.getMdmsCriteria().getModuleDetails().get(0).getMasterDetails())
                .extracting("name")
                .containsExactly("BillingPeriod");
    }

    @Test
    void getMeterReadingDetails_whenMapperFails_throwsParsingError() {
        when(mapper.convertValue(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(MeterReadingResponse.class)))
                .thenThrow(new IllegalArgumentException("bad json"));

        assertThatThrownBy(() -> meterReadingUtil.getMeterReadingDetails(Map.of()))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("cannot be parsed");
    }
}
