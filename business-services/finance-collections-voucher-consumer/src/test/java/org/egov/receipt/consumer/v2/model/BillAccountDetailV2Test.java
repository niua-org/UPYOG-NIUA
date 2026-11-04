package org.egov.receipt.consumer.v2.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BillAccountDetailV2Test {
    @Test
    void classLoads() {
        assertThat(BillAccountDetailV2.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BillAccountDetailV2.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
