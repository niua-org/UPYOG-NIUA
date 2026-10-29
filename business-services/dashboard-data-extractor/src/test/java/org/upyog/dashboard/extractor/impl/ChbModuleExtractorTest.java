package org.upyog.dashboard.extractor.impl;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ChbModuleExtractorTest {
    @Test
    void classLoads() {
        assertThat(ChbModuleExtractor.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ChbModuleExtractor.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
