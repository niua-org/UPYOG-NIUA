package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MasterDetailTest {
    @Test
    void classLoads() {
        assertThat(MasterDetail.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = MasterDetail.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
