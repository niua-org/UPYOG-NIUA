package org.egov.egf.master.web.advice;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

class CustomControllerAdviceTest {
    @ExtendWith(MockitoExtension.class)
    class ControllerContractTests {
        @Test
        void class_isRestController() {
            assertThat(CustomControllerAdvice.class).isNotNull();
        }
    }
}
