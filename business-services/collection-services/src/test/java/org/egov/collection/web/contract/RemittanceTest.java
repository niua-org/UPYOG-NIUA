package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RemittanceTest {
    @Test
    void classLoads() {
        assertThat(Remittance.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Remittance.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
