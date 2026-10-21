package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SupplierTest {
    @Test
    void classLoads() {
        assertThat(Supplier.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Supplier.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
