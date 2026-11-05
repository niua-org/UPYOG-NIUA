// generated-by: generate_per_class_unit_tests.py
package org.upyog.pgrai.web.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.core.io.ResourceLoader;
import org.upyog.pgrai.util.HRMSUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MockControllerTest {
    @Mock
    private ResourceLoader resourceLoader;
    @Mock
    private HRMSUtil hrmsUtil;

    private MockController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new MockController(resourceLoader, hrmsUtil);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void mockMvc_whenStandaloneSetup_isNotNull() {
        assertThat(mockMvc).isNotNull();
    }
}
