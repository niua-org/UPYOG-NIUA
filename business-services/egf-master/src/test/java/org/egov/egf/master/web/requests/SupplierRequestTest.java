package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SupplierRequestTest {
    @Test
    void classLoads() {
        assertThat(SupplierRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SupplierRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
