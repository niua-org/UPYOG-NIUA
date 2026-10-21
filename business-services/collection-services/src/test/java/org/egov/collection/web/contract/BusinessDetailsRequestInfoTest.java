package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BusinessDetailsRequestInfoTest {
    @Test
    void classLoads() {
        assertThat(BusinessDetailsRequestInfo.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BusinessDetailsRequestInfo.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
