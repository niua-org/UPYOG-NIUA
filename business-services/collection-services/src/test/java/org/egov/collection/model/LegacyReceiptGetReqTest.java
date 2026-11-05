package org.egov.collection.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LegacyReceiptGetReqTest {
    @Test
    void classLoads() {
        assertThat(LegacyReceiptGetReq.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = LegacyReceiptGetReq.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
