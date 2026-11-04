// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.models.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentBelongsToTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(DocumentBelongsTo.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        DocumentBelongsTo first = DocumentBelongsTo.values()[0];
        assertThat(DocumentBelongsTo.valueOf(first.name())).isEqualTo(first);
    }
}
