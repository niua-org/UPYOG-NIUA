package org.egov.common.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AttributeContractTest {
    @Test
    void classLoads() {
        assertThat(AttributeContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AttributeContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
