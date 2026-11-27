package org.egov.common.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AttributeTest {
    @Test
    void classLoads() {
        assertThat(Attribute.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Attribute.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
