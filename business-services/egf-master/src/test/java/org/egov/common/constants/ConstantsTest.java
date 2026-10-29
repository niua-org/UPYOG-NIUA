package org.egov.common.constants;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ConstantsTest {
    @Test
    void classLoads() {
        assertThat(Constants.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Constants.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
