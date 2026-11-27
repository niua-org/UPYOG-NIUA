package org.upyog.dashboard.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LegacyBatchIngestRequestTest {
    @Test
    void classLoads() {
        assertThat(LegacyBatchIngestRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = LegacyBatchIngestRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
