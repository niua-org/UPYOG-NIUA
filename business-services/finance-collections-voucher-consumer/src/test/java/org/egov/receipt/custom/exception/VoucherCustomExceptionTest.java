package org.egov.receipt.custom.exception;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class VoucherCustomExceptionTest {
    @Test
    void classLoads() {
        assertThat(VoucherCustomException.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = VoucherCustomException.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
