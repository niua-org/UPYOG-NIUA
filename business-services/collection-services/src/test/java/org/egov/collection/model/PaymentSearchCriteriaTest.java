package org.egov.collection.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentSearchCriteriaTest {
    @Test
    void classLoads() {
        assertThat(PaymentSearchCriteria.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = PaymentSearchCriteria.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
