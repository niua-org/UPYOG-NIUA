package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EgModulesTest {
    @Test
    void classLoads() {
        assertThat(EgModules.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = EgModules.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
