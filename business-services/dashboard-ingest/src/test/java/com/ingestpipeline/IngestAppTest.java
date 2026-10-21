package com.ingestpipeline;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class IngestAppTest {
    @Test
    void classLoads() {
        assertThat(IngestApp.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = IngestApp.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
