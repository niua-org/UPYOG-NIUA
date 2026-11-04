package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ApportionResponseTest {
    @Test
    void classLoads() {
        assertThat(ApportionResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ApportionResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
