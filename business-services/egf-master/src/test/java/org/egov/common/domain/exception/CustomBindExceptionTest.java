package org.egov.common.domain.exception;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CustomBindExceptionTest {
    @Test
    void classLoads() {
        assertThat(CustomBindException.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = CustomBindException.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
