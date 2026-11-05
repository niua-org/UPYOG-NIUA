// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.calculator.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DemandDetailAndCollectionTest {

    @Test
    void builder_whenDefaultBuilds_isNotNull() {
        assertThat(DemandDetailAndCollection.builder().build()).isNotNull();
    }

    @Test
    void class_whenLoaded_isNotNull() {
        assertThat(DemandDetailAndCollection.class).isNotNull();
    }
}
