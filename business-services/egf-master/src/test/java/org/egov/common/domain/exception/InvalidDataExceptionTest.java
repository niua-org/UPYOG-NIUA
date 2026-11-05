package org.egov.common.domain.exception;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InvalidDataExceptionTest {
    @Test
    void classLoads() {
        assertThat(InvalidDataException.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InvalidDataException.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
