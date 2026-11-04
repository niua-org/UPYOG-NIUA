package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SubSchemeTest {
    @Test
    void classLoads() {
        assertThat(SubScheme.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SubScheme.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
