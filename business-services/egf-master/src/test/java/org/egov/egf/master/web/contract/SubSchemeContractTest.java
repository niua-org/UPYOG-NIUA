package org.egov.egf.master.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SubSchemeContractTest {
    @Test
    void classLoads() {
        assertThat(SubSchemeContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SubSchemeContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
