package org.upyog.dashboard.pt.constants;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PTMetricConstantsTest {
    @Test
    void classLoads() {
        assertThat(PTMetricConstants.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = PTMetricConstants.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
