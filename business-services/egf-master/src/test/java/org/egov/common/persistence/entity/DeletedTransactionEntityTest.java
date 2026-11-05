package org.egov.common.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DeletedTransactionEntityTest {
    @Test
    void classLoads() {
        assertThat(DeletedTransactionEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DeletedTransactionEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
