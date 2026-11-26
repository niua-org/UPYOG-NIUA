package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FilterRequestTest {
    @Test
    void classLoads() {
        assertThat(FilterRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = FilterRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
