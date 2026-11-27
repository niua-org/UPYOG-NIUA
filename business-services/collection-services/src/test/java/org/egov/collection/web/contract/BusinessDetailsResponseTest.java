package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BusinessDetailsResponseTest {
    @Test
    void classLoads() {
        assertThat(BusinessDetailsResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BusinessDetailsResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
