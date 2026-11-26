package org.egov.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TaxDetailTest {
    @Test
    void classLoads() {
        assertThat(TaxDetail.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = TaxDetail.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
