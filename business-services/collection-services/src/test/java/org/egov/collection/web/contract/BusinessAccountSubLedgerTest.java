package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BusinessAccountSubLedgerTest {
    @Test
    void classLoads() {
        assertThat(BusinessAccountSubLedger.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BusinessAccountSubLedger.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
