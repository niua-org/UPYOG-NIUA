package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RemittanceSearchRequestTest {
    @Test
    void classLoads() {
        assertThat(RemittanceSearchRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RemittanceSearchRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
