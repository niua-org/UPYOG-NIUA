package org.upyog.employee.dasboard.util;

import org.egov.common.contract.request.RequestInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MdmsUtilTest {

    @Mock
    private RestTemplate restTemplate;

    private MdmsUtil mdmsUtil;

    @BeforeEach
    void setUp() {
        mdmsUtil = new MdmsUtil(restTemplate);
        ReflectionTestUtils.setField(mdmsUtil, "mdmsHost", "http://mdms/");
        ReflectionTestUtils.setField(mdmsUtil, "mdmsUrl", "v1/_search");
        ReflectionTestUtils.setField(mdmsUtil, "masterName", "RegistrationCharges");
        ReflectionTestUtils.setField(mdmsUtil, "moduleName", "VTR");
    }

    @Test
    void fetchRegistrationChargesFromMdms_returnsAmount() {
        Map<String, Object> response = Map.of(
                "MdmsRes", Map.of("VTR", Map.of("RegistrationCharges", java.util.List.of(Map.of("amount", 500))))
        );
        when(restTemplate.postForObject(any(String.class), any(), eq(Map.class))).thenReturn(response);

        Integer rate = mdmsUtil.fetchRegistrationChargesFromMdms(new RequestInfo(), "pb.amritsar");

        assertThat(rate).isEqualTo(500);
    }

    @Test
    void fetchRegistrationChargesFromMdms_onError_returnsZero() {
        when(restTemplate.postForObject(any(String.class), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("mdms down"));

        assertThat(mdmsUtil.fetchRegistrationChargesFromMdms(new RequestInfo(), "pb.amritsar")).isZero();
    }
}
