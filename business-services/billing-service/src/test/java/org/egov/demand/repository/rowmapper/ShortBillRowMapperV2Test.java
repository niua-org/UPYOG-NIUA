package org.egov.demand.repository.rowmapper;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ShortBillRowMapperV2Test {
    @Test
    void classLoads() {
        assertThat(ShortBillRowMapperV2.class).isNotNull();
    }
}
