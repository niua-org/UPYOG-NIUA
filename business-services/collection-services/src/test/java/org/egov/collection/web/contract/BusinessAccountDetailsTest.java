package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BusinessAccountDetailsTest {
    @Test
    void classLoads() {
        assertThat(BusinessAccountDetails.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BusinessAccountDetails.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
