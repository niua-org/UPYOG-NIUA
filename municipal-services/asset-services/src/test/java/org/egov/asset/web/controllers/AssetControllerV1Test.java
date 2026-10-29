// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.web.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.egov.asset.service.AssetCalculationClient;
import org.egov.asset.service.AssetService;
import org.egov.asset.util.ResponseInfoFactory;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AssetControllerV1Test {
    @Mock
    private ResponseInfoFactory responseInfoFactory;
    @Mock
    private AssetService assetService;
    @Mock
    private AssetCalculationClient assetCalculationClient;

    private AssetControllerV1 controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new AssetControllerV1(responseInfoFactory, assetService, assetCalculationClient);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void mockMvc_whenStandaloneSetup_isNotNull() {
        assertThat(mockMvc).isNotNull();
    }
}
