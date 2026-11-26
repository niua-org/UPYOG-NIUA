package org.egov.web.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CollectionTypeTest {
    @Test
    void values_nonEmpty() {
        assertThat(CollectionType.values()).isNotEmpty();
    }
}
