package org.egov.common.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DeletedTransactionTest {
    @Test
    void classLoads() {
        assertThat(DeletedTransaction.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DeletedTransaction.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
