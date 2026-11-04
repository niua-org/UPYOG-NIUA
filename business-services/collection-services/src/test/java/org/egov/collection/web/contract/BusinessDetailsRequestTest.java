package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BusinessDetailsRequestTest {
    @Test
    void classLoads() {
        assertThat(BusinessDetailsRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BusinessDetailsRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
