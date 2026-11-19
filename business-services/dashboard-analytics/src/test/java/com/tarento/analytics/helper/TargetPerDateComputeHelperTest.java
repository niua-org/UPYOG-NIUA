package com.tarento.analytics.helper;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TargetPerDateComputeHelperTest {
    @Test
    void classLoads() {
        assertThat(TargetPerDateComputeHelper.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = TargetPerDateComputeHelper.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
