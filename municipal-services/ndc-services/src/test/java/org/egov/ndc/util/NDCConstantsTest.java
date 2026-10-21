// generated-by: generate_per_class_unit_tests.py
package org.egov.ndc.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NDCConstantsTest {

    @Test
    void SEARCH_MODULE_whenRead_isNotBlank() {
        assertThat(NDCConstants.SEARCH_MODULE).isNotBlank();
    }

}
