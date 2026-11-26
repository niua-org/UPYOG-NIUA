// generated-by: generate_per_class_unit_tests.py
package org.egov.fsm.calculator.web.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.egov.fsm.calculator.services.BillingSlabService;
import org.egov.fsm.calculator.utils.BillingSlabUtil;
import org.egov.fsm.calculator.utils.ResponseInfoFactory;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BillingSlabControllerTest {
    @Mock
    private BillingSlabUtil billigSlabUtil;
    @Mock
    private ResponseInfoFactory responseInfoFactory;
    @Mock
    private BillingSlabService billingSlabService;

    @InjectMocks
    private BillingSlabController controller;
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
