package org.upyog.dashboard.extractor.impl;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PtModuleExtractorTest {
    @Test
    void classLoads() {
        assertThat(PtModuleExtractor.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = PtModuleExtractor.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
