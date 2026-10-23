package org.egov.collection.model.v1;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BillAccountDetail_v1Test {
    @Test
    void classLoads() {
        assertThat(BillAccountDetail_v1.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = BillAccountDetail_v1.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
