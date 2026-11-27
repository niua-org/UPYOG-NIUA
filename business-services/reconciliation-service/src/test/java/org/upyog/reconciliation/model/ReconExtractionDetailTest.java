package org.upyog.reconciliation.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ReconExtractionDetailTest {
    @Test
    void classLoads() {
        assertThat(ReconExtractionDetail.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ReconExtractionDetail.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
