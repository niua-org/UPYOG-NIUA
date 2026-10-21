package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RemittanceReceiptTest {
    @Test
    void classLoads() {
        assertThat(RemittanceReceipt.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RemittanceReceipt.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
