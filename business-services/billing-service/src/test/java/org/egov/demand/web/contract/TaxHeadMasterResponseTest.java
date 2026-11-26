package org.egov.demand.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TaxHeadMasterResponseTest {
    @Test
    void classLoads() {
        assertThat(TaxHeadMasterResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = TaxHeadMasterResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
