package org.egov.receipt.consumer.repository.builder;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class VoucherIntegrationLogQueryBuilderTest {
    @Test
    void classLoads() {
        assertThat(VoucherIntegrationLogQueryBuilder.class).isNotNull();
    }
}
