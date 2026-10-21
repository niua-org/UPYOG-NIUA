// generated-by: generate_per_class_unit_tests.py
package org.egov.ptr.web.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.egov.ptr.service.PetRegistrationService;
import org.egov.ptr.service.PetSchedulerService;
import org.egov.ptr.util.ResponseInfoFactory;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PetControllerTest {
    @Mock
    private PetRegistrationService petRegistrationService;
    @Mock
    private PetSchedulerService petSchedulerService;
    @Mock
    private ResponseInfoFactory responseInfoFactory;

    @InjectMocks
    private PetController controller;
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
