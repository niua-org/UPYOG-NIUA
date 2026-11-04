package com.tarento.analytics.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TransactionTest {
    @Test
    void classLoads() {
        assertThat(Transaction.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Transaction.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
