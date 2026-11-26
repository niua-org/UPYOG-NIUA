package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AuditableContractTest {
    @Test
    void classLoads() {
        assertThat(AuditableContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AuditableContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
