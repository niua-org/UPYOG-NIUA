package org.egov.demand.model.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TypeTest {
    @Test
    void values_nonEmpty() {
        assertThat(Type.values()).isNotEmpty();
    }
}
