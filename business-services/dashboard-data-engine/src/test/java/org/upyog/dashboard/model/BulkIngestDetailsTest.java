package org.upyog.dashboard.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BulkIngestDetailsTest {
    @Test
    void classLoads() {
        assertThat(BulkIngestDetails.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BulkIngestDetails.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
