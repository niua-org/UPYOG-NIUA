// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.calculator.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.egov.pt.calculator.service.BillingSlabService;
import org.egov.pt.calculator.service.MutationBillingSlabService;
import org.egov.pt.calculator.validator.BillingSlabValidator;
import org.egov.pt.calculator.validator.MutationBillingSlabValidator;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BillingSlabControllerTest {
    @Mock
    private BillingSlabService service;
    @Mock
    private MutationBillingSlabService mutationService;
    @Mock
    private BillingSlabValidator billingSlabValidator;
    @Mock
    private MutationBillingSlabValidator mutationbillingSlabValidator;

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
