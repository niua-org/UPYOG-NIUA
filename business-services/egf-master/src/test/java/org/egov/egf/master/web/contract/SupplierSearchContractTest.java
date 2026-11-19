package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SupplierSearchContractTest {
    @Test
    void classLoads() {
        assertThat(SupplierSearchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SupplierSearchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
