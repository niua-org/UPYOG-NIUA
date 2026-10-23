// generated-by: generate_per_class_unit_tests.py
package org.egov.vehicle.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.egov.vehicle.service.VehicleService;
import org.egov.vehicle.util.ResponseInfoFactory;
import org.egov.vehicle.util.VehicleUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class VehicleControllerTest {
    @Mock
    private VehicleService vehicleService;
    @Mock
    private VehicleUtil vehicleUtil;
    @Mock
    private ResponseInfoFactory responseInfoFactory;

    @InjectMocks
    private VehicleController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void mockMvc_whenStandaloneSetup_isNotNull() {
        assertThat(mockMvc).isNotNull();
    }
}
