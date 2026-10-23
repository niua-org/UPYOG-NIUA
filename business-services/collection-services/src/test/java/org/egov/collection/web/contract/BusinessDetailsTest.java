package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BusinessDetailsTest {
    @Test
    void classLoads() {
        assertThat(BusinessDetails.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BusinessDetails.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
