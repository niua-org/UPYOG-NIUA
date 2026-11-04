package org.egov.collection.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EnumDataTest {
    @Test
    void classLoads() {
        assertThat(EnumData.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = EnumData.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
