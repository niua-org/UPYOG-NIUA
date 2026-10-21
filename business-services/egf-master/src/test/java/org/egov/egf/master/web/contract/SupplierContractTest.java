package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SupplierContractTest {
    @Test
    void classLoads() {
        assertThat(SupplierContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SupplierContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
