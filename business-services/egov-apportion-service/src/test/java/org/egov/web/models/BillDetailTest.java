package org.egov.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BillDetailTest {
    @Test
    void classLoads() {
        assertThat(BillDetail.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BillDetail.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
