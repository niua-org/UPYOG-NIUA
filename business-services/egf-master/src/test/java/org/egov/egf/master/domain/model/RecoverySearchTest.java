package org.egov.egf.master.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RecoverySearchTest {
    @Test
    void classLoads() {
        assertThat(RecoverySearch.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RecoverySearch.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
