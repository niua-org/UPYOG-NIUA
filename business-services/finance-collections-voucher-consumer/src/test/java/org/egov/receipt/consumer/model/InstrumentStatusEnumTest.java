package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InstrumentStatusEnumTest {

    @Test
    void statusesByCategory_openCategory_includesOpenStatuses() {
        Set<String> statuses = InstrumentStatusEnum.statusesByCategory(InstrumentStatusEnum.Category.OPEN);

        assertThat(statuses).isNotEmpty();
        assertThat(statuses).allMatch(s -> InstrumentStatusEnum.fromValue(s).isCategory(InstrumentStatusEnum.Category.OPEN));
    }

    @Test
    void fromValue_knownStatus_returnsEnum() {
        InstrumentStatusEnum first = InstrumentStatusEnum.values()[0];
        assertThat(InstrumentStatusEnum.fromValue(first.toString())).isEqualTo(first);
    }
}
