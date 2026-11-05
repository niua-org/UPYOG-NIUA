package org.egov.receipt.consumer.v2.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BillDetailV2Test {
    @Test
    void classLoads() {
        assertThat(BillDetailV2.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BillDetailV2.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
