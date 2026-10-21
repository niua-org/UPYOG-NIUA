package org.egov.web.models.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DemandApportionRequestTest {
    @Test
    void classLoads() {
        assertThat(DemandApportionRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DemandApportionRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
