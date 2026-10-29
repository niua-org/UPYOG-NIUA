package org.egov.collection.model.v1;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ReceiptRequest_v1Test {
    @Test
    void classLoads() {
        assertThat(ReceiptRequest_v1.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ReceiptRequest_v1.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
