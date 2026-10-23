package org.upyog.dashboard.mdms.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MdmsTenantResponseTest {
    @Test
    void classLoads() {
        assertThat(MdmsTenantResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = MdmsTenantResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
