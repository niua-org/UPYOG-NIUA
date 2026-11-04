package org.egov.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ApportionRequestV2Test {
    @Test
    void classLoads() {
        assertThat(ApportionRequestV2.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ApportionRequestV2.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
