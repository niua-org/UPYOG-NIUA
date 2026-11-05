package org.egov.hrms.utils;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ErrorConstantsTest {
    @Test
    void classLoads() {
        assertThat(ErrorConstants.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ErrorConstants.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
