// generated-by: generate_per_class_unit_tests.py
package org.upyog.chb.web.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.upyog.chb.service.CommunityHallBookingService;
import org.upyog.chb.service.DemandService;
import org.upyog.chb.service.SchedulerService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CommunityHallBookingControllerTest {
    @Mock
    private CommunityHallBookingService bookingService;
    @Mock
    private DemandService demandService;
    @Mock
    private SchedulerService schedulerService;

    private CommunityHallBookingController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new CommunityHallBookingController(bookingService, demandService, schedulerService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void mockMvc_whenStandaloneSetup_isNotNull() {
        assertThat(mockMvc).isNotNull();
    }
}
