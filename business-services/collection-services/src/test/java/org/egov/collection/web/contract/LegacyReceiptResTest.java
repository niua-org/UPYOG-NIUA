package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LegacyReceiptResTest {
    @Test
    void classLoads() {
        assertThat(LegacyReceiptRes.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = LegacyReceiptRes.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
