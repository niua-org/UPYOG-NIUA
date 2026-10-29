// generated-by: generate_per_class_unit_tests.py
package org.egov.swcalculation.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class PropertyTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new Property()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(Property.builder().build()).isNotNull();
}

    }
