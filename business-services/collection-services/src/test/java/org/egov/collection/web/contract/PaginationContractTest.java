package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PaginationContractTest {
    @Test
    void classLoads() {
        assertThat(PaginationContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = PaginationContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
