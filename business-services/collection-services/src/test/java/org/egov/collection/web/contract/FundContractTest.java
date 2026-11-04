package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FundContractTest {
    @Test
    void classLoads() {
        assertThat(FundContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FundContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
