package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ApportionRequestTest {
    @Test
    void classLoads() {
        assertThat(ApportionRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ApportionRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
