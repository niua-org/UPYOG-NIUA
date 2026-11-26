// generated-by: generate_per_class_unit_tests.py
package org.egov.vendor.driver.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.egov.vendor.driver.service.DriverService;
import org.egov.vendor.util.ResponseInfoFactory;
import org.egov.vendor.util.VendorUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class VendorDriverContollerTest {
    @Mock
    private DriverService driverService;
    @Mock
    private VendorUtil vendorUtil;
    @Mock
    private ResponseInfoFactory responseInfoFactory;

    @InjectMocks
    private VendorDriverContoller controller;
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
