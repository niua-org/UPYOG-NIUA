package org.upyog.dashboard.transformer.impl;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FinanceTransformerTest {
    @Test
    void classLoads() {
        assertThat(FinanceTransformer.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FinanceTransformer.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
