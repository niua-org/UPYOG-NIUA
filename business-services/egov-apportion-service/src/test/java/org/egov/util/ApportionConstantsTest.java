package org.egov.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ApportionConstantsTest {
    @Test
    void classLoads() {
        assertThat(ApportionConstants.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ApportionConstants.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
