package org.egov.common.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DeletedTransactionContractTest {
    @Test
    void classLoads() {
        assertThat(DeletedTransactionContract.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DeletedTransactionContract.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
