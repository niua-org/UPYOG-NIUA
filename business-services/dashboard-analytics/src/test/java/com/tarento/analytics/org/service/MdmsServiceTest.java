package com.tarento.analytics.org.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarento.analytics.service.impl.RestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MdmsServiceTest {

    @Mock
    private RestService restService;

    private MdmsService mdmsService;

    @BeforeEach
    void setUp() throws Exception {
        mdmsService = new MdmsService();
        ReflectionTestUtils.setField(mdmsService, "restService", restService);
        ReflectionTestUtils.setField(mdmsService, "mapper", new ObjectMapper());
        ReflectionTestUtils.setField(mdmsService, "stateLevelTenantId", "pb");
        ReflectionTestUtils.setField(mdmsService, "mdmsServiceHost", "http://mdms");
        ReflectionTestUtils.setField(mdmsService, "mdmsSearchEndpoint", "/search");
        when(restService.post(anyString(), anyString(), any())).thenThrow(new RuntimeException("mdms down"));
        mdmsService.loadMdmsService();
    }

    @Test
    void getDDRNameByCode_returnsDefaultMappingWhenMdmsFails() {
        assertThat(mdmsService.getDDRNameByCode("1")).isEqualTo("Amritsar-DDR");
        assertThat(mdmsService.getDDRNameByCode("unknown")).isEmpty();
    }
}
