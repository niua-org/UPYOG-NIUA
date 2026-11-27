// generated-by: generate_per_class_unit_tests.py
package org.egov.waterconnection.web.models.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RelationshipTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(Relationship.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        Relationship first = Relationship.values()[0];
        assertThat(Relationship.valueOf(first.name())).isEqualTo(first);
    }
}
