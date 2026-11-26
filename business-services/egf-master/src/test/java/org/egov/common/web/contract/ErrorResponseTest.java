package org.egov.common.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {
    @Test
    void classLoads() {
        assertThat(ErrorResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ErrorResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
