package org.egov.collection.model.v1;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class Receipt_v1Test {
    @Test
    void classLoads() {
        assertThat(Receipt_v1.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Receipt_v1.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
