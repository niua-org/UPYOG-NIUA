package org.egov;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

class EgfMasterAspectjTest {
    @ExtendWith(MockitoExtension.class)
    class ControllerContractTests {
        @Test
        void class_isRestController() {
            assertThat(EgfMasterAspectj.class).isNotNull();
        }
    }
}
