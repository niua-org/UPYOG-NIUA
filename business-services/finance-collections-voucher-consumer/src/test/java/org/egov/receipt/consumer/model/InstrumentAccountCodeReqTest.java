package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentAccountCodeReqTest {
    @Test
    void classLoads() {
        assertThat(InstrumentAccountCodeReq.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = InstrumentAccountCodeReq.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
