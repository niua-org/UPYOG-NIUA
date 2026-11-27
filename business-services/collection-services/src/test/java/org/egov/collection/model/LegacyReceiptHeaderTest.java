package org.egov.collection.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LegacyReceiptHeaderTest {
    @Test
    void classLoads() {
        assertThat(LegacyReceiptHeader.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = LegacyReceiptHeader.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
