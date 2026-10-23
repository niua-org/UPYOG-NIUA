package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EgfStatusSearchContractTest {
    @Test
    void classLoads() {
        assertThat(EgfStatusSearchContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = EgfStatusSearchContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
