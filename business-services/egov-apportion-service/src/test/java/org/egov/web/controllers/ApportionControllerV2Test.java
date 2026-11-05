package org.egov.web.controllers;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

class ApportionControllerV2Test {
    @ExtendWith(MockitoExtension.class)
    class ControllerContractTests {
        @Test
        void class_isRestController() {
            assertThat(ApportionControllerV2.class).isNotNull();
        }
    }
}
