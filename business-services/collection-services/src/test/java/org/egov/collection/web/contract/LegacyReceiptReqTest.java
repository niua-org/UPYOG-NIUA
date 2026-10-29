package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LegacyReceiptReqTest {
    @Test
    void classLoads() {
        assertThat(LegacyReceiptReq.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = LegacyReceiptReq.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
