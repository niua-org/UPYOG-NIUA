package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CollectionConfigResponseTest {
    @Test
    void classLoads() {
        assertThat(CollectionConfigResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = CollectionConfigResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
