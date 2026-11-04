package org.egov.collection.model.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentTypesEnumTest {
    @Test
    void values_nonEmpty() {
        assertThat(InstrumentTypesEnum.values()).isNotEmpty();
    }
}
