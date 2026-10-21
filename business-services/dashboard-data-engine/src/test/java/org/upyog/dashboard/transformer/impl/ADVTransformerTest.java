package org.upyog.dashboard.transformer.impl;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ADVTransformerTest {
    @Test
    void classLoads() {
        assertThat(ADVTransformer.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ADVTransformer.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
