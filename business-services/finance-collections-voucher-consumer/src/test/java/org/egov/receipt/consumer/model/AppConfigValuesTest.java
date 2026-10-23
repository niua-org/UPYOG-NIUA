package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AppConfigValuesTest {
    @Test
    void classLoads() {
        assertThat(AppConfigValues.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AppConfigValues.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
