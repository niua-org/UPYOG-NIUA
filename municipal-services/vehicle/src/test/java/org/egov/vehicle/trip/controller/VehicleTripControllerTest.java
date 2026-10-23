// generated-by: generate_per_class_unit_tests.py
package org.egov.vehicle.trip.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.egov.vehicle.trip.service.VehicleTripService;
import org.egov.vehicle.util.ResponseInfoFactory;
import org.egov.vehicle.util.VehicleUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class VehicleTripControllerTest {
    @Mock
    private ResponseInfoFactory responseInfoFactory;
    @Mock
    private VehicleUtil vehicleLogUtil;
    @Mock
    private VehicleTripService vehicleTripService;

    @InjectMocks
    private VehicleTripController controller;
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
