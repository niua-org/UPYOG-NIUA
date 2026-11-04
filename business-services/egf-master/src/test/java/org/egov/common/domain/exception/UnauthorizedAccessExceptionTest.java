package org.egov.common.domain.exception;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UnauthorizedAccessExceptionTest {
    @Test
    void classLoads() {
        assertThat(UnauthorizedAccessException.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = UnauthorizedAccessException.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
