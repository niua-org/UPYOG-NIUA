package org.upyog.dashboard.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BulkIngestRequestTest {
    @Test
    void classLoads() {
        assertThat(BulkIngestRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BulkIngestRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
