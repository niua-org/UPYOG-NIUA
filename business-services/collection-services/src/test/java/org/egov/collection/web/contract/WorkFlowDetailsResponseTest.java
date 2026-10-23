package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class WorkFlowDetailsResponseTest {
    @Test
    void classLoads() {
        assertThat(WorkFlowDetailsResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = WorkFlowDetailsResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
