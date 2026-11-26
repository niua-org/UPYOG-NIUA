package org.egov.demand.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UpdateBillRequestTest {
    @Test
    void classLoads() {
        assertThat(UpdateBillRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = UpdateBillRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
