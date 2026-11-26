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
import org.egov.asset.service.AssetMaintenanceService;
import org.egov.asset.util.ResponseInfoFactory;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AssetMaintenanceControllerTest {
    @Mock
    private AssetMaintenanceService assetMaintenanceService;
    @Mock
    private ResponseInfoFactory responseInfoFactory;

    private AssetMaintenanceController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new AssetMaintenanceController(assetMaintenanceService, responseInfoFactory);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void mockMvc_whenStandaloneSetup_isNotNull() {
        assertThat(mockMvc).isNotNull();
    }
}
