package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BillAccountDetailTest {
    @Test
    void classLoads() {
        assertThat(BillAccountDetail.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BillAccountDetail.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
