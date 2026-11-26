package org.egov.collection.model.v1;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BillResponse_v1Test {
    @Test
    void classLoads() {
        assertThat(BillResponse_v1.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BillResponse_v1.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
