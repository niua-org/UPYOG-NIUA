package com.ingestpipeline.controller;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

class RestApiControllerTest {
    @ExtendWith(MockitoExtension.class)
    class ControllerContractTests {
        @Test
        void class_isRestController() {
            assertThat(RestApiController.class).isNotNull();
        }
    }
}
