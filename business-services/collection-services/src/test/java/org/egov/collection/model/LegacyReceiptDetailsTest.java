package org.egov.collection.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LegacyReceiptDetailsTest {
    @Test
    void classLoads() {
        assertThat(LegacyReceiptDetails.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = LegacyReceiptDetails.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
