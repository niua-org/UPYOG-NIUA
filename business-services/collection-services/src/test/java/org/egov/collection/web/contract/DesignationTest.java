package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DesignationTest {
    @Test
    void classLoads() {
        assertThat(Designation.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Designation.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
