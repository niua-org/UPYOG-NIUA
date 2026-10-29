package org.egov.web.models.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CategoryTest {
    @Test
    void values_nonEmpty() {
        assertThat(Category.values()).isNotEmpty();
    }
}
