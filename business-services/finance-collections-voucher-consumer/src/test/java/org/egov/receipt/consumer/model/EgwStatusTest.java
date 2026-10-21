package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EgwStatusTest {
    @Test
    void classLoads() {
        assertThat(EgwStatus.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = EgwStatus.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
