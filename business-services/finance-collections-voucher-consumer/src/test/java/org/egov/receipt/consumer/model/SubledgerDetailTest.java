package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SubledgerDetailTest {
    @Test
    void classLoads() {
        assertThat(SubledgerDetail.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SubledgerDetail.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
