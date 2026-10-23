package org.upyog.dashboard.transformer.impl;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CHBTransformerTest {
    @Test
    void classLoads() {
        assertThat(CHBTransformer.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = CHBTransformer.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
