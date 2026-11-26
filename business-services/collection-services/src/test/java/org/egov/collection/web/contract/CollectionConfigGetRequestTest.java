package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CollectionConfigGetRequestTest {
    @Test
    void classLoads() {
        assertThat(CollectionConfigGetRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = CollectionConfigGetRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
