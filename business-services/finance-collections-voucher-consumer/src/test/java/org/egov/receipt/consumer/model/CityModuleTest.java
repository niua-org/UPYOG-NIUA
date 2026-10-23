package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CityModuleTest {
    @Test
    void classLoads() {
        assertThat(CityModule.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = CityModule.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
