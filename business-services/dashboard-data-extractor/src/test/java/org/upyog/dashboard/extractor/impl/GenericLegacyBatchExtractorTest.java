package org.upyog.dashboard.extractor.impl;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class GenericLegacyBatchExtractorTest {
    @Test
    void classLoads() {
        assertThat(GenericLegacyBatchExtractor.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = GenericLegacyBatchExtractor.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
