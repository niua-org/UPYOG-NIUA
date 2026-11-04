package org.egov.egf.master.web.requests;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SupplierResponseTest {
    @Test
    void classLoads() {
        assertThat(SupplierResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SupplierResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
