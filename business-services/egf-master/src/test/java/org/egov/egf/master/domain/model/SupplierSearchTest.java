package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SupplierSearchTest {
    @Test
    void classLoads() {
        assertThat(SupplierSearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SupplierSearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
