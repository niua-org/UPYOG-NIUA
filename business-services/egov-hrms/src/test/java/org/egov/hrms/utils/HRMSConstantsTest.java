package org.egov.hrms.utils;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class HRMSConstantsTest {
    @Test
    void classLoads() {
        assertThat(HRMSConstants.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = HRMSConstants.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
