// generated-by: generate_per_class_unit_tests.py
package org.upyog.tp.web.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.upyog.tp.service.TreePruningService;
import org.upyog.tp.validator.ValidatorService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TreePruningControllerTest {
    @Mock
    private TreePruningService treePruningService;
    @Mock
    private ValidatorService validatorService;

    private TreePruningController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new TreePruningController(treePruningService, validatorService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void mockMvc_whenStandaloneSetup_isNotNull() {
        assertThat(mockMvc).isNotNull();
    }
}
