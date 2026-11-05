package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DesignationResponseTest {
    @Test
    void classLoads() {
        assertThat(DesignationResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DesignationResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
