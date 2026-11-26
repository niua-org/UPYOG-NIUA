package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MdmsResponseTest {
    @Test
    void classLoads() {
        assertThat(MdmsResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = MdmsResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
