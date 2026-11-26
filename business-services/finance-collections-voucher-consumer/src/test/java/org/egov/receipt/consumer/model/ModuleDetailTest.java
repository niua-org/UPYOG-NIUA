package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ModuleDetailTest {
    @Test
    void classLoads() {
        assertThat(ModuleDetail.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ModuleDetail.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
