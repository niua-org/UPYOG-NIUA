package org.egov.hrms.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class IdGenerationResponseTest {
    @Test
    void classLoads() {
        assertThat(IdGenerationResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = IdGenerationResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
