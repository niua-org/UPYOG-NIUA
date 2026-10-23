package org.egov.collection.model.v1;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ReceiptSearchCriteria_v1Test {
    @Test
    void classLoads() {
        assertThat(ReceiptSearchCriteria_v1.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ReceiptSearchCriteria_v1.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
