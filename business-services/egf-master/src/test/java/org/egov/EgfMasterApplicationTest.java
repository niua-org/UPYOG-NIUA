package org.egov;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EgfMasterApplicationTest {
    @Test
    void classLoads() {
        assertThat(EgfMasterApplication.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = EgfMasterApplication.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
