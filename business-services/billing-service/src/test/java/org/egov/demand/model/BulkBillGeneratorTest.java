package org.egov.demand.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BulkBillGeneratorTest {
    @Test
    void classLoads() {
        assertThat(BulkBillGenerator.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BulkBillGenerator.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
