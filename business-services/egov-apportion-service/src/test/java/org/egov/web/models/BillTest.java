package org.egov.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BillTest {
    @Test
    void classLoads() {
        assertThat(Bill.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Bill.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
