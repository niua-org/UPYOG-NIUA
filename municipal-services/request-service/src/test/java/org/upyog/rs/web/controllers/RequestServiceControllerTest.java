// generated-by: generate_per_class_unit_tests.py
package org.upyog.rs.web.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.upyog.rs.service.MobileToiletService;
import org.upyog.rs.service.WaterTankerService;
import org.upyog.rs.validator.ValidatorService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RequestServiceControllerTest {
    @Mock
    private WaterTankerService waterTankerService;
    @Mock
    private MobileToiletService mobileToiletService;
    @Mock
    private ValidatorService validatorService;

    @InjectMocks
    private RequestServiceController controller;
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
