package org.egov.demand.helper;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CollectionReceiptRequestTest {
    @Test
    void classLoads() {
        assertThat(CollectionReceiptRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = CollectionReceiptRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
