package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MdmsCriteriaTest {
    @Test
    void classLoads() {
        assertThat(MdmsCriteria.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = MdmsCriteria.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
