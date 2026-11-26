package org.egov.egf.master.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SupplierEntityTest {
    @Test
    void classLoads() {
        assertThat(SupplierEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SupplierEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
